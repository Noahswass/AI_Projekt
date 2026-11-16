package com.example.AI_Projekt.service;

import com.example.AI_Projekt.entity.Part;
import com.example.AI_Projekt.entity.SyncRun;
import com.example.AI_Projekt.entity.SyncStatus;
import com.example.AI_Projekt.repository.LegoColourRepo;
import com.example.AI_Projekt.repository.LegoSetRepo;
import com.example.AI_Projekt.repository.MinifigRepo;
import com.example.AI_Projekt.repository.PartRepo;
import com.example.AI_Projekt.repository.SyncRunRepo;
import com.example.AI_Projekt.repository.ThemeRepo;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the whole sync against a fake Rebrickable (WireMock) and a real H2 database.
 * Own database name, so rows from other test classes cannot change the counts here.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:syncrun;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "app.rebrickable.api-key=test-key",
        "app.rebrickable.max-attempts=2",
        "app.rebrickable.backoff=10ms"
})
class SyncRunServiceTest {

    private static final String API = "/api/v3/lego";

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void rebrickableUrl(DynamicPropertyRegistry registry) {
        registry.add("app.rebrickable.base-url", () -> wireMock.baseUrl() + "/api/v3");
    }

    @Autowired
    SyncRunService syncRunService;
    @Autowired
    SyncRunRepo syncRunRepo;
    @Autowired
    ThemeRepo themeRepo;
    @Autowired
    LegoColourRepo legoColourRepo;
    @Autowired
    LegoSetRepo legoSetRepo;
    @Autowired
    PartRepo partRepo;
    @Autowired
    MinifigRepo minifigRepo;

    @BeforeEach
    void emptyTables() {
        syncRunRepo.deleteAll();
        minifigRepo.deleteAll();
        partRepo.deleteAll();
        legoSetRepo.deleteAll();   // before themes: lego_set has a foreign key to theme
        legoColourRepo.deleteAll();
        themeRepo.deleteAll();
    }

    @Test
    void successfulRunWritesCountsAndIsRepeatable() {
        stubThemesColoursAndSets();
        stubParts();
        stubMinifigs();

        SyncRun first = syncRunService.run();

        assertThat(first.getStatus()).isEqualTo(SyncStatus.SUCCESS);
        assertThat(first.getCountsJson())
                .isEqualTo("{\"themes\":2,\"colours\":2,\"sets\":1,\"parts\":2,\"minifigs\":1}");
        assertThat(first.getErrorMessage()).isNull();
        assertThat(first.getStartedAt()).isNotNull();
        assertThat(first.getFinishedAt()).isNotNull();
        assertThat(partRepo.count()).isEqualTo(2);
        assertThat(minifigRepo.count()).isEqualTo(1);

        Part sticker = partRepo.findAll().stream()
                .filter(part -> part.getPartNum().equals("003381")).findFirst().orElseThrow();
        assertThat(sticker.getName()).isEqualTo("Sticker Sheet for Set 663-1");
        assertThat(sticker.getPartCatId()).isEqualTo(58);
        assertThat(sticker.getPartImgUrl()).isNull();
        assertThat(minifigRepo.findAll().get(0).getFigNum()).isEqualTo("fig-000001");

        SyncRun second = syncRunService.run();

        assertThat(second.getStatus()).isEqualTo(SyncStatus.SUCCESS);
        assertThat(syncRunRepo.count()).isEqualTo(2);
        assertThat(themeRepo.count()).isEqualTo(2);
        assertThat(legoColourRepo.count()).isEqualTo(2);
        assertThat(legoSetRepo.count()).isEqualTo(1);
        assertThat(partRepo.count()).isEqualTo(2);
        assertThat(minifigRepo.count()).isEqualTo(1);
    }

    @Test
    void failedRunKeepsTheErrorMessageAndRollsBackTheData() {
        stubThemesColoursAndSets();
        wireMock.stubFor(get(urlPathEqualTo(API + "/parts/")).willReturn(aResponse().withStatus(401)));
        stubMinifigs();

        SyncRun run = syncRunService.run();

        assertThat(run.getStatus()).isEqualTo(SyncStatus.FAILED);
        assertThat(run.getErrorMessage()).contains("401").contains("/lego/parts/");
        assertThat(run.getCountsJson()).isNull();
        assertThat(run.getFinishedAt()).isNotNull();

        // The sync_run row survives, the half-finished catalog data does not.
        SyncRun stored = syncRunRepo.findById(run.getId()).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(SyncStatus.FAILED);
        assertThat(stored.getErrorMessage()).contains("401");
        assertThat(themeRepo.count()).isZero();
        assertThat(legoColourRepo.count()).isZero();
        assertThat(legoSetRepo.count()).isZero();
        assertThat(partRepo.count()).isZero();
    }

    private void stubThemesColoursAndSets() {
        wireMock.stubFor(get(urlPathEqualTo(API + "/themes/")).willReturn(okJson("""
                {"count":2,"next":null,"previous":null,"results":[
                  {"id":1,"parent_id":null,"name":"Technic"},
                  {"id":2,"parent_id":1,"name":"Cars"}]}""")));
        wireMock.stubFor(get(urlPathEqualTo(API + "/colors/")).willReturn(okJson("""
                {"count":2,"next":null,"previous":null,"results":[
                  {"id":0,"name":"Black","rgb":"05131D","is_trans":false},
                  {"id":15,"name":"White","rgb":"FFFFFF","is_trans":false}]}""")));
        wireMock.stubFor(get(urlPathEqualTo(API + "/sets/")).willReturn(okJson("""
                {"count":1,"next":null,"previous":null,"results":[
                  {"set_num":"001-1","name":"Gears","year":1965,"theme_id":2,"num_parts":43,
                   "set_img_url":"https://cdn.rebrickable.com/media/sets/001-1/11530.jpg",
                   "set_url":"https://rebrickable.com/sets/001-1/gears/",
                   "last_modified_dt":"2024-01-08T19:22:27.085452Z"}]}""")));
    }

    private void stubParts() {
        wireMock.stubFor(get(urlPathEqualTo(API + "/parts/")).willReturn(okJson("""
                {"count":2,"next":null,"previous":null,"results":[
                  {"part_num":"003381","name":"Sticker Sheet for Set 663-1","part_cat_id":58,
                   "part_url":"https://rebrickable.com/parts/003381/sticker-sheet-for-set-663-1/",
                   "part_img_url":null,"external_ids":{"BrickLink":["663stk01"]}},
                  {"part_num":"3001","name":"Brick 2 x 4","part_cat_id":11,
                   "part_url":"https://rebrickable.com/parts/3001/brick-2-x-4/",
                   "part_img_url":"https://cdn.rebrickable.com/media/parts/elements/300101.jpg",
                   "external_ids":{}}]}""")));
    }

    private void stubMinifigs() {
        wireMock.stubFor(get(urlPathEqualTo(API + "/minifigs/")).willReturn(okJson("""
                {"count":1,"next":null,"previous":null,"results":[
                  {"set_num":"fig-000001","name":"Toy Store Employee","num_parts":4,
                   "set_img_url":"https://cdn.rebrickable.com/media/sets/fig-000001/55726.jpg",
                   "set_url":"https://rebrickable.com/minifigs/fig-000001/toy-store-employee/",
                   "last_modified_dt":"2020-05-27T21:47:00.694941Z"}]}""")));
    }
}
