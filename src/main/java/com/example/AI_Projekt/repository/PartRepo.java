package com.example.AI_Projekt.repository;

import com.example.AI_Projekt.entity.Part;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * The interface Part repo.
 */
public interface PartRepo extends JpaRepository<Part, Integer> {
    /**
     * Find by part num optional.
     *
     * @param partNum the part num
     * @return the optional
     */
    Optional<Part> findByPartNum(String partNum);

    /**
     * Search page.
     *
     * @param pattern  the pattern
     * @param pageable the pageable
     * @return the page
     */
    @Query("""
        select p from Part p
                where (lower(p.name) like :pattern escape '!'
        or lower(p.partNum) like :pattern escape '!')
        """)
    Page<Part> search(@Param("pattern") String pattern,
                      Pageable pageable);


}