package com.example.jpainsert;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;


public interface PostRepository extends JpaRepository<Post, Long> {

//    @Transactional // jpql로 insert하기 위해 필요
    @Modifying // jpql로 insert하기 위해 필요
    @Query(value = "INSERT INTO post (content) VALUES (:content)", nativeQuery = true)
    void saveByJPQL(@Param("content") String content);
}
