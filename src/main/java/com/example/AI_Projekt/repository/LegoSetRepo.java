package com.example.AI_Projekt.repository;
import com.example.AI_Projekt.entity.LegoSet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
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

    /** How many sets each theme has. Themes without sets are not in the result. */
    @Query("""
            select s.themeId as themeId, count(s) as setCount
            from LegoSet s
            where s.themeId is not null
            group by s.themeId
            """)
    List<ThemeSetCount> countSetsPerTheme();

    /**
     * For each theme, the picture of its set with the most parts: one row per theme, and no row
     * when none of its sets has a picture.
     *
     * Native SQL with a window function, because the same question as a JPQL subquery
     * ("num_parts = the max for that theme") took 22 seconds on the real catalog; this takes 0.15.
     */
    @Query(nativeQuery = true, value = """
            select theme_id as themeId, set_img_url as imgUrl, num_parts as numParts
            from (select theme_id, set_img_url, num_parts,
                         row_number() over (partition by theme_id
                                            order by num_parts desc, set_num) as position
                  from lego_set
                  where set_img_url is not null and theme_id is not null) ranked
            where position = 1
            """)
    List<ThemePicture> findBiggestSetPicturePerTheme();

    interface ThemeSetCount {
        Integer getThemeId();

        long getSetCount();
    }

    interface ThemePicture {
        Integer getThemeId();

        String getImgUrl();

        Integer getNumParts();
    }
}
