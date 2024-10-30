# save(), saveAll(), jpql, bulk insert

### save()
* save() : @Transactional 기준으로 감싸져 있어 프록시 기반 동작을 한다. <br />
  => 프록시 기반 동작 : 실제 메서드 호출 전에 트랜잭션 관련 처리를 하고, 메서드 호출이 끝난 후에도 트랜잭션을 커밋하거나 롤백하는 등의 작업<br />
  => ★ 각 save()때마다 프록시 기반으로 동작한다. 
  <br /><br />
* 결과 : <u>총 걸린시간(s) = 835s</u>
<br /><br/>

### saveAll()
* saveAll() : @Transactional 기준으로 감싸져 있어 프록시 기반 동작을 한다.<br />
  => 프록시 기반 동작 : 실제 메서드 호출 전에 트랜잭션 관련 처리를 하고, 메서드 호출이 끝난 후에도 트랜잭션을 커밋하거나 롤백하는 등의 작업<br />
  => ★ save()와 달리 1번만 트랜잭션을 생성하고, save()를 같은 인스턴스 내부에서 호출하기 때문에 프록시 로직을 타지 않게 된다.
  <br /><br />
* 결과 : <u>총 걸린시간(s) = 21s</u>

<br /><br/>
### jpql
* saveByJPQL() : jpql로 작성한 insert
* <설정방법>
  * @Modifying, nativeQuery 필수 사용 + @Transactional 사용도 필수
  ```java
  @Modifying // jpql로 insert하기 위해 필요
  @Query(value = "INSERT INTO post (content) VALUES (:content)", nativeQuery = true)
  void saveByJPQL(@Param("content") String content);
  ```
* <code>@Transactional</code>위치에 따른 변화
1. void saveByJPQL(@Param("content") String content); 위에 @Transactional 이 붙어있는 경우 
   == save()와 같이 각 saveByJPQL()때마다 프록시 기반으로 동작한다. (연결 및 끊기)

2. void saveByJPQL(@Param("content") String content); 를 사용하는 service에 붙어있는 경우
   == saveAll()과 같이 1번만 트랜잭션을 생성하고, flush()될 때 한번에 진행된다.
  <br /><br />
* 결과 :
1. <u>총 걸린시간(s) = 834s</u>
2. <u>총 걸린시간(s) = 22s</u>

<br /><br/>
### bulk insert 
* Bulk 삽입(=Batch Insert) : 여러 건의 삽입을 할 때, 1개의 쿼리로 나가는 것 (<-> save(), saveAll() : 단건 삽입(여러개의 쿼리로 나감))
  ```java
  insert into post_table (post_title, post_content)
  values
  (title1, content1),
  (title2, content2),
  (title3, content3),
  (title4, content4)
  ```
  => IDENTITY 전략 시 Batch Insert를 사용할 수 x  <br />
  => why? JPA의 쓰기지연 특성 ( DB에 Insert가 되어야 id 값을 알 수 있다 ) 때문이다. batch는 id값을 알아야 하는 특성과 충돌됨<br />
  => 그래서 sequence 또는 table로 사용하거나 JDBC를 통해 insert 하면 된다.
<br /><br />
* <설정 방법>
  * spring.datasource.url에 <code>rewriteBatchedStatements=true</code> 추가 => 이 옵션이 true로 설정해야지 Bulk로 수행됨.
  >  - <code>postfileSQL = true</code> : 전송 쿼리 출력
  >  - <code>logger=Slf4JLogger</code> : 쿼리 출력 시 사용할 로거 설정
  >  - <code>maxQuerySizeToLog=999999</code> : 출력할 쿼리 길이
  >  - ※ <code>spring.datasource.hikari.data-source-properties.rewriteBatchedStatements=true</code> : mysql
  >  - ※ <code>spring.datasource.hikari.data-source-properties.rewriteBatchedInserts=true</code> : postgre<br />

  *  <application.yml>
    ```yml
    spring:
      datasource:
          url: jdbc:mysql://localhost:3306/db명?rewriteBatchedStatements=true
      jpa:
          properties:
              hibernate:
                  ## bulk insert 옵션 ##
                  # 정렬 옵션
                  order_inserts: true
                  order_updates: true
                  # 한번에 나가는 배치 개수 -> 100개의 insert를 1개로 보낸다.
                  jdbc:
                      batch_size: 100
    ```  
  <br />
* 해결방법 :
  1. Spring JDBC를 이용한 Batch Insert
  2. jpa에서 Id의 GeneratedValue를 sequence로 설정
  3. jpa에서 Id의 GeneratedValue를 table로 설정  
  
  <br /><br/>
##### Spring JDBC를 이용한 Batch Insert
```java
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
```
* 결과 : <u>총 걸린시간(s) = 1s</u><br />
=> rewriteBatchedStatements=true 안붙였을 때는 15초

<br /><br/>
##### jpa에서 Id의 GeneratedValue를 sequence로 설정
* sequence 전략 : em.persist()를 호출할 때 먼저 데이터베이스 시퀀스를 사용해 식별자를 조회해서 가져오고, 조회한 식별자를 엔티티에 할당한 후 엔티티를 영속성 컨텍스트에 저장
  => <code>insert into post_sequence (content,id) values ('saveAllBulk() _ using JPA - sequence: 0',300002),('saveAllBulk() _ using JPA - sequence: 1',300003),('saveAllBulk() _ using JPA - sequence: 2',300004)...</code>

* save() 결과 : <u>총 걸린시간(s) = 1005s</u>
* saveAll() 결과 : <u>총 걸린시간(s) = 26s</u>

<br /><br/>
##### jpa에서 Id의 GeneratedValue를 table로 설정
* table 전략 : 키 생성 전용 테이블을 하나 만들어서 데이터베이스 시퀀스를 흉내내는 전략 (but, 성능적인 손해)
 => <code>insert into post_table (content,id) values ('saveAllBulk() _ using JPA - table: 0',300002),('saveAllBulk() _ using JPA - table: 1',300003),('saveAllBulk() _ using JPA - table: 2',300004)...</code>

* save() 결과 : <u>총 걸린시간(s) = 985s</u>
* saveAll() 결과 : <u>총 걸린시간(s) = 27s</u>


### 요약
|속도|save()|saveAll()|jpql|jdbc(bulk insert)|
|---|---|---|---|---|
|10만 건|835s|21s|22s|1s|

* JPA의 Identity로 Batch INSERT 가 안되는 이유
  * Identity는 ID를 자동할당하는데 (실제 DB에)insert를 실행하기 전까지는 ID에 할당된 값을 알 수 없기 때문에 (JPA의 쓰기지연 특성)
  * Batch insert는 Id 값을 알아야 함.
  

##### 참조
* https://velog.io/@joonghyun/Spring-JPA-Save-vs-SaveAll-vs-Bulk-Insert
* https://velog.io/@hyunho058/JdbcTemplate-batchUpdate%EB%A5%BC-%ED%99%9C%EC%9A%A9%ED%95%9C-Bulk-Insert
* https://medium.com/@hee98.09.14/jpa-id%EC%A0%84%EB%9E%B5%EC%9D%B4-identity%EC%9D%B8-%EC%83%81%ED%83%9C%EC%97%90%EC%84%9C-bulk-insert-%EC%88%98%ED%96%89%ED%95%98%EA%B8%B0-8bf9c760bd82
* https://backtony.tistory.com/45