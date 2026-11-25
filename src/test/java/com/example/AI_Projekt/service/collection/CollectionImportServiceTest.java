package com.example.AI_Projekt.service.collection;

import com.example.AI_Projekt.entity.AppUser;
import com.example.AI_Projekt.entity.CollectionItem;
import com.example.AI_Projekt.entity.RebrickableAccountLink;
import com.example.AI_Projekt.rebrickable.ColorDto;
import com.example.AI_Projekt.rebrickable.PartDto;
import com.example.AI_Projekt.rebrickable.RebrickableClient;
import com.example.AI_Projekt.rebrickable.RebrickableException;
import com.example.AI_Projekt.rebrickable.SetDto;
import com.example.AI_Projekt.rebrickable.UserPartDto;
import com.example.AI_Projekt.rebrickable.UserSetDto;
import com.example.AI_Projekt.repository.AppUserRepo;
import com.example.AI_Projekt.repository.CollectionItemRepo;
import com.example.AI_Projekt.repository.RebrickableAccountLinkRepo;
import com.example.AI_Projekt.security.TokenCipher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** PB-14: an import makes anna's collection_item rows match her Rebrickable account. */
class CollectionImportServiceTest {

    private RebrickableClient client;
    private RebrickableAccountLinkRepo rebrickableAccountLinkRepo;
    private CollectionItemRepo collectionItemRepo;
    private RebrickableAccountLink link;
    private CollectionImportService service;

    @BeforeEach
    void setUp() {
        AppUserRepo appUserRepo = mock(AppUserRepo.class);
        client = mock(RebrickableClient.class);
        rebrickableAccountLinkRepo = mock(RebrickableAccountLinkRepo.class);
        collectionItemRepo = mock(CollectionItemRepo.class);
        TokenCipher tokenCipher = new TokenCipher("TL6PdGYQcF6q3iSmT7d4zffXdMWgeMBJAxDFrVNZsOg=");

        AppUser anna = new AppUser();
        anna.setId(7);
        anna.setUsername("anna");
        when(appUserRepo.findByUsernameIgnoreCase("anna")).thenReturn(Optional.of(anna));

        // The link holds the token encrypted, as RebrickableLinkService saves it.
        // The client is only stubbed for "tok123", so every test also proves the token was decrypted.
        link = new RebrickableAccountLink();
        link.setUserId(7);
        link.setEncryptedToken(tokenCipher.encrypt("tok123"));
        when(rebrickableAccountLinkRepo.findByUserId(7)).thenReturn(Optional.of(link));

        // Defaults: empty account on Rebrickable, empty collection here. Tests override what they need.
        when(client.fetchUserSets("tok123")).thenReturn(List.of());
        when(client.fetchUserParts("tok123")).thenReturn(List.of());
        when(collectionItemRepo.findByUserId(7)).thenReturn(List.of());

        service = new CollectionImportService(client, appUserRepo, rebrickableAccountLinkRepo, tokenCipher, collectionItemRepo);
    }

    @Test
    void newSetAndPartAreAdded() {
        when(client.fetchUserSets("tok123")).thenReturn(List.of(userSet("10281-1", 2)));
        when(client.fetchUserParts("tok123")).thenReturn(List.of(userPart("3001", 4, 14)));

        ImportResult result = service.importCollection("anna");

        assertThat(result).isEqualTo(new ImportResult(1, 1));
        List<CollectionItem> saved = savedItems();
        assertThat(saved).hasSize(2);

        CollectionItem set = only(saved, "SET");
        assertThat(set.getUserId()).isEqualTo(7);
        assertThat(set.getItemRef()).isEqualTo("10281-1");
        assertThat(set.getColorId()).isEqualTo(-1);   // sets have no colour
        assertThat(set.getQuantity()).isEqualTo(2);

        CollectionItem part = only(saved, "PART");
        assertThat(part.getItemRef()).isEqualTo("3001");
        assertThat(part.getColorId()).isEqualTo(4);
        assertThat(part.getQuantity()).isEqualTo(14);
    }

    @Test
    void sameSetInTwoListsIsSummedIntoOneRow() {
        when(client.fetchUserSets("tok123")).thenReturn(List.of(userSet("10281-1", 1), userSet("10281-1", 2)));

        ImportResult result = service.importCollection("anna");

        assertThat(result.sets()).isEqualTo(1);
        List<CollectionItem> saved = savedItems();
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getQuantity()).isEqualTo(3);
    }

    @Test
    void existingRowIsUpdatedInPlaceAndKeepsItsNotes() {
        CollectionItem existing = item("SET", "10281-1", -1, 1);
        existing.setNotes("Box is in the attic");
        when(collectionItemRepo.findByUserId(7)).thenReturn(List.of(existing));
        when(client.fetchUserSets("tok123")).thenReturn(List.of(userSet("10281-1", 2)));

        service.importCollection("anna");

        List<CollectionItem> saved = savedItems();
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0)).isSameAs(existing);   // the same row, not a new one
        assertThat(saved.get(0).getQuantity()).isEqualTo(2);
        assertThat(saved.get(0).getNotes()).isEqualTo("Box is in the attic");
        assertThat(deletedItems()).isEmpty();
    }

    @Test
    void itemGoneFromRebrickableIsDeleted() {
        CollectionItem gone = item("PART", "3622", 1, 3);
        CollectionItem kept = item("PART", "3001", 4, 14);
        when(collectionItemRepo.findByUserId(7)).thenReturn(List.of(gone, kept));
        when(client.fetchUserParts("tok123")).thenReturn(List.of(userPart("3001", 4, 14)));

        service.importCollection("anna");

        assertThat(deletedItems()).containsExactly(gone);
        assertThat(savedItems()).containsExactly(kept);
    }

    @Test
    void samePartInAnotherColourIsANewRow() {
        CollectionItem red = item("PART", "3001", 4, 14);
        when(collectionItemRepo.findByUserId(7)).thenReturn(List.of(red));
        when(client.fetchUserParts("tok123")).thenReturn(List.of(userPart("3001", 4, 14), userPart("3001", 1, 6)));

        ImportResult result = service.importCollection("anna");

        assertThat(result.parts()).isEqualTo(2);
        List<CollectionItem> saved = savedItems();
        assertThat(saved).hasSize(2).contains(red);
        assertThat(saved).filteredOn(i -> i != red).singleElement()
                .satisfies(blue -> assertThat(blue.getColorId()).isEqualTo(1));
    }

    @Test
    void importStampsLastImportAt() {
        service.importCollection("anna");

        assertThat(link.getLastImportAt()).isNotNull();
        verify(rebrickableAccountLinkRepo).save(link);
    }

    @Test
    void wrongTokenAsksForRelinkAndChangesNothing() {
        when(client.fetchUserSets("tok123")).thenThrow(new RebrickableException("Rebrickable returned 404", 404, null));

        assertThatThrownBy(() -> service.importCollection("anna"))
                .isInstanceOf(RebrickableRelinkNeededException.class);
        verify(collectionItemRepo, never()).saveAll(any());
        verify(collectionItemRepo, never()).deleteAll(any());
        verify(rebrickableAccountLinkRepo, never()).save(any());
    }

    @Test
    void rebrickableDownIsPassedOnUnchanged() {
        // A failure on the second fetch: the sets were fetched, but nothing may be saved yet
        when(client.fetchUserParts("tok123")).thenThrow(new RebrickableException("Rebrickable did not respond", null, null));

        assertThatThrownBy(() -> service.importCollection("anna"))
                .isInstanceOf(RebrickableException.class)
                .isNotInstanceOf(RebrickableRelinkNeededException.class);
        verify(collectionItemRepo, never()).saveAll(any());
        verify(collectionItemRepo, never()).deleteAll(any());
    }

    @Test
    void notLinkedThrowsBeforeCallingRebrickable() {
        when(rebrickableAccountLinkRepo.findByUserId(7)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.importCollection("anna"))
                .isInstanceOf(RebrickableNotLinkedException.class);
        verify(client, never()).fetchUserSets(any());
    }

    // ---- helpers ----

    private static UserSetDto userSet(String setNum, int quantity) {
        return new UserSetDto(quantity, new SetDto(setNum, null, null, null, null, null, null, null));
    }

    private static UserPartDto userPart(String partNum, int colorId, int quantity) {
        return new UserPartDto(quantity, new PartDto(partNum, null, null, null, null), new ColorDto(colorId, null, null, false));
    }

    private static CollectionItem item(String type, String ref, int colorId, int quantity) {
        CollectionItem item = new CollectionItem();
        item.setUserId(7);
        item.setItemType(type);
        item.setItemRef(ref);
        item.setColorId(colorId);
        item.setQuantity(quantity);
        return item;
    }

    private static CollectionItem only(List<CollectionItem> items, String type) {
        return items.stream().filter(i -> i.getItemType().equals(type)).findFirst().orElseThrow();
    }

    /** What the service passed to saveAll. HashMap order is random, so tests never rely on order. */
    private List<CollectionItem> savedItems() {
        ArgumentCaptor<Iterable<CollectionItem>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(collectionItemRepo).saveAll(captor.capture());
        return toList(captor.getValue());
    }

    private List<CollectionItem> deletedItems() {
        ArgumentCaptor<Iterable<CollectionItem>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(collectionItemRepo).deleteAll(captor.capture());
        return toList(captor.getValue());
    }

    private static List<CollectionItem> toList(Iterable<CollectionItem> items) {
        List<CollectionItem> list = new ArrayList<>();
        items.forEach(list::add);
        return list;
    }
}
