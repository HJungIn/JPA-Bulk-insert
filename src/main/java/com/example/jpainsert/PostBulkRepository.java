package com.example.jpainsert;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class PostBulkRepository {

    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public void saveAllBulk(List<Post> postList){
        String sql = "INSERT INTO post (content) VALUES (?)";

        jdbcTemplate.batchUpdate(
                sql, // 사용할 sql
                postList, // 저장할 리스트
                postList.size(), // 배치 사이즈
                (PreparedStatement ps, Post post) -> {
                    ps.setString(1, post.getContent());
                }
        );
    }
}
