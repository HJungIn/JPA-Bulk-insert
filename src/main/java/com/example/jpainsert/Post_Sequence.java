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
public class Post_Sequence {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "POST_SEQ_GENERATOR")
    @SequenceGenerator(
            name = "POST_SEQ_GENERATOR", // 식별자 생성기 이름
            sequenceName = "POST_SEQ", // 데이터베이스에 등록되어있는 시퀀스 이름: DB에는 해당 이름으로 매핑된다.
            initialValue = 1, // DDL 생성시에만 사용되며 시퀀스 DDL을 생성할 때 처음 시작하는 수를 지정
            allocationSize = 50 // 시퀀스 한 번 호출에 증가하는 수
    )
    private Long id;

    private String content;
}
