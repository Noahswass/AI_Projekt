package com.example.AI_Projekt.repository;
import com.example.AI_Projekt.entity.LegoSet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;


public interface LegoSetRepo extends JpaRepository<LegoSet, Integer> {
    Optional<LegoSet> findByLegoSetNum(String setNum);

    /**
     * Sets whose name or set number matches {@code pattern}, optionally limited to some themes.
     *
     * @param pattern   lower-case LIKE pattern, with '!' as the escape character; "%" matches every set
     * @param allThemes true to ignore {@code themeIds}
     * @param themeIds  must not be empty, also when {@code allThemes} is true
     */
    @Query("""
            select s from LegoSet s
            where (:allThemes = true or s.themeId in :themeIds)
              and (lower(s.name) like :pattern escape '!'
                   or lower(s.legoSetNum) like :pattern escape '!')
            """)
    Page<LegoSet> search(@Param("pattern") String pattern,
                         @Param("allThemes") boolean allThemes,
                         @Param("themeIds") Collection<Integer> themeIds,
                         Pageable pageable);
}
