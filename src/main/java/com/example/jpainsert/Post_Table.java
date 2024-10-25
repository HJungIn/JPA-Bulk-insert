package com.example.jpainsert;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class Post_Table {

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "POST_TABLE_SEQ_GENERATOR")
    @TableGenerator(
            name = "POST_TABLE_SEQ_GENERATOR", // 식별자 생성기 이름
            table = "POST_TABLE_SEQUENCE", // 위에서 생성한 테이블명(식별자 생성기의 테이블명)
            pkColumnName = "POST_TABLE_SEQ" // 위에서 생성한 테이블에 sequence_name
    )
    private Long id;

    private String content;
}
