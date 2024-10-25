package com.example.jpainsert;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


public interface Post_SequenceRepository extends JpaRepository<Post_Sequence, Long> {
}
