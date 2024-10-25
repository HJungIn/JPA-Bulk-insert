package com.example.jpainsert;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class PostTest {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostBulkRepository postBulkRepository;

    @Autowired
    private Post_SequenceRepository postSequenceRepository;

    @Autowired
    private Post_TableRepository postTableRepository;

    @Test
    void test() throws InterruptedException {

        System.out.println("now = " + LocalDateTime.now());
        LocalDateTime start = LocalDateTime.now();
        Thread.sleep(1000);
        LocalDateTime end = LocalDateTime.now();
        System.out.println("총 걸린시간 = " + Duration.between(start, end).getSeconds());
    }

    /**
     * <10만건 기준>
     * save() : @Transactional 기준으로 감싸져 있어 프록시 기반 동작을 한다.
     * => 프록시 기반 동작 : 실제 메서드 호출 전에 트랜잭션 관련 처리를 하고, 메서드 호출이 끝난 후에도 트랜잭션을 커밋하거나 롤백하는 등의 작업
     * => ★ 각 save()때마다 프록시 기반으로 동작한다.
     *
     * ============================================
     *
     * * 결과 :
     * Hibernate: 
     *     insert 
     *     into
     *         post
     *         (content) 
     *     values
     *         (?)
     *   ...
     * 총 걸린시간(s) = 835s
     *
     * ============================================
     *
     * * 13분 9초 걸림
     *
     * */
    @Test
    void save() {

        LocalDateTime start = LocalDateTime.now();

        for(int i=0;i<100000; i++){
            Post post = Post.builder()
                    .content("save() : " + i)
                    .build();
            postRepository.save(post);
        }

        LocalDateTime end = LocalDateTime.now();
        System.out.println("총 걸린시간(s) = " + Duration.between(start, end).getSeconds());
    }


    /**
     * <10만건 기준>
     * saveAll() : @Transactional 기준으로 감싸져 있어 프록시 기반 동작을 한다.
     * => 프록시 기반 동작 : 실제 메서드 호출 전에 트랜잭션 관련 처리를 하고, 메서드 호출이 끝난 후에도 트랜잭션을 커밋하거나 롤백하는 등의 작업
     * => ★ save()와 달리 1번만 트랜잭션을 생성하고, save()를 같은 인스턴스 내부에서 호출하기 때문에 프록시 로직을 타지 않게 된다.
     *
     * ============================================
     *
     * * 결과 :
     * Hibernate:
     *     insert
     *     into
     *         post
     *         (content)
     *     values
     *         (?)
     *  ...
     * 총 걸린시간(s) = 21
     *
     * ============================================
     *
     * * 21초 걸림
     *
     * */
    @Test
    void saveAll() {

        LocalDateTime start = LocalDateTime.now();

        List<Post> postList = new ArrayList<>();

        for(int i=0;i<100000; i++){
            postList.add(
                    Post.builder()
                            .content("saveAll() : "+i)
                            .build()
            );
        }
        postRepository.saveAll(postList);

        LocalDateTime end = LocalDateTime.now();
        System.out.println("총 걸린시간(s) = " + Duration.between(start, end).getSeconds());
    }

    /**
     * <10만건 기준>
     * saveByJPQL() : jpql로 작성한 insert
     * <설정방법>
     *     @Modifying // jpql로 insert하기 위해 필요
     *     @Query(value = "INSERT INTO post (content) VALUES (:content)", nativeQuery = true)
     *     void saveByJPQL(@Param("content") String content);
     * 처럼 @Modifying, nativeQuery 필수 사용 + @Transactional 사용도 필수
     *
     * 1. void saveByJPQL(@Param("content") String content); 위에 @Transactional 이 붙어있는 경우
     *   == save()와 같이 각 saveByJPQL()때마다 프록시 기반으로 동작한다. (연결 및 끊기)
     *
     * 2. void saveByJPQL(@Param("content") String content); 를 사용하는 service에 붙어있는 경우
     *   == saveAll()과 같이 1번만 트랜잭션을 생성하고, flush()될 때 한번에 진행된다.
     *
     *
     * ============================================
     *
     * * 1번의 결과 :
     * Hibernate:
     *     insert
     *     into
     *         post
     *         (content)
     *     values
     *         (?)
     *   ...
     * 총 걸린시간(s) = 834
     *
     *
     * * 2번의 결과 :
     * Hibernate:
     *     insert
     *     into
     *         post
     *         (content)
     *     values
     *         (?)
     *   ...
     * 총 걸린시간(s) = 22
     *
     * ============================================
     *
     * * 1번 : 13분 8초 걸림
     * * 2번 : 22초
     * */
    @Test
    void saveByJPQL_repository_transaction_1() {

        LocalDateTime start = LocalDateTime.now();

        for(int i=0;i<100000; i++){
            postRepository.saveByJPQL("saveByJPQL_repository_transaction_1() : " + 0);
        }

        LocalDateTime end = LocalDateTime.now();
        System.out.println("총 걸린시간(s) = " + Duration.between(start, end).getSeconds());
    }

    @Transactional
    @Test
    void saveByJPQL_service_transaction_2() {

        LocalDateTime start = LocalDateTime.now();

        for(int i=0;i<100000; i++){
            postRepository.saveByJPQL("saveByJPQL_service_transaction_2() : " + 0);
        }

        LocalDateTime end = LocalDateTime.now();
        System.out.println("총 걸린시간(s) = " + Duration.between(start, end).getSeconds());
    }


    /**
     * Bulk 삽입(=Batch Insert) : 여러 건의 삽입을 할 때, 1개의 쿼리로 나가는 것 (<-> save(), saveAll() : 단건 삽입(여러개의 쿼리로 나감))
     *      insert into post_table (post_title, post_content)
     * 	    values
     * 	    (title1, content1),
     * 	    (title2, content2),
     * 	    (title3, content3),
     * 	    (title4, content4)
     *
     * => IDENTITY 전략 시 Batch Insert를 사용할 수 x
     *  => why? JPA의 쓰기지연 특성 ( DB에 Insert가 되어야 id 값을 알 수 있다 ) 때문이다. batch는 id값을 알아야 하는 특성과 충돌됨
     *  => 그래서 sequence 또는 table로 사용하면 됨
     *
     * ============================================
     * <설정 방법>
     * * spring.datasource.url에 'rewriteBatchedStatements=true' 추가 => 이 옵션이 true로 설정해야지 Bulk로 수행됨.
     *  - postfileSQL = true : 전송 쿼리 출력
     *  - logger=Slf4JLogger : 쿼리 출력 시 사용할 로거 설정
     *  - maxQuerySizeToLog=999999 : 출력할 쿼리 길이
     *  ※ spring.datasource.hikari.data-source-properties.rewriteBatchedStatements=true // mysql
     *  ※ spring.datasource.hikari.data-source-properties.rewriteBatchedInserts=true // postgre
     *
     *  <application.yml>
     *   spring:
     *     datasource:
     *         url: jdbc:mysql://localhost:3306/db명?rewriteBatchedStatements=true
     *     jpa:
     *         properties:
     *             hibernate:
     *                 ## bulk insert 옵션 ##
     *                 # 정렬 옵션
     *                 order_inserts: true
     *                 order_updates: true
     *                 # 한번에 나가는 배치 개수 -> 100개의 insert를 1개로 보낸다.
     *                 jdbc:
     *                     batch_size: 100
     *
     * ============================================
     *
     * 해결방법 :
     *  1. Spring JDBC를 이용한 Batch Insert
     *  2. jpa에서 Id의 GeneratedValue를 sequence로 설정
     *  2. jpa에서 Id의 GeneratedValue를 table로 설정
     * */
    
    /**
     * 1. Spring JDBC를 이용한 Batch Insert
     *
     * ============================================
     * * 결과 :
     * 총 걸린시간(s) = 1
     *
     * ============================================
     *
     * * 1초 걸림
     * => rewriteBatchedStatements=true 안붙였을 때는 15초
     *
     * */
    @Test
    void BulkInsert_1_JDBC사용() {

        LocalDateTime start = LocalDateTime.now();

        List<Post> postList = new ArrayList<>();

        for(int i=0;i<100000; i++){
            postList.add(
                    Post.builder()
                            .content("saveAllBulk() _ using JDBC: "+i)
                            .build()
            );
        }
        postBulkRepository.saveAllBulk(postList);

        LocalDateTime end = LocalDateTime.now();
        System.out.println("총 걸린시간(s) = " + Duration.between(start, end).getSeconds());
    }

    /**
     * 2. jpa에서 Id의 GeneratedValue를 sequence로 설정
     * * sequence 전략 : em.persist()를 호출할 때 먼저 데이터베이스 시퀀스를 사용해 식별자를 조회해서 가져오고, 조회한 식별자를 엔티티에 할당한 후 엔티티를 영속성 컨텍스트에 저장
     *
     * ============================================
     * # spring.jpa.properties.hibernate.jdbc.batch_size = 100000 설정 안했을 경우
     * Hibernate:
     *     insert
     *     into
     *         post_table
     *         (content, id)
     *     values
     *         (?, ?)
     *
     *  * save() 결과 :
     *  총 걸린시간(s) = 882
     *
     *  * saveAll() 결과 :
     *  총 걸린시간(s) = 39
     *
     *
     * # spring.jpa.properties.hibernate.jdbc.batch_size = 100000 설정 했을 경우
     * insert into post_sequence (content,id) values ('saveAllBulk() _ using JPA - sequence: 0',300002),('saveAllBulk() _ using JPA - sequence: 1',300003),('saveAllBulk() _ using JPA - sequence: 2',300004)...
     *  * save() 결과 :
     *  총 걸린시간(s) = 1005
     *
     *  * saveAll() 결과 :
     *  총 걸린시간(s) = 26
     *
     * ============================================
     *
     * # spring.jpa.properties.hibernate.jdbc.batch_size = 100000 설정 안했을 경우
     *  * save() : 14분 7초 걸림
     *  * saveAll() : 39초 걸림
     *
     * # spring.jpa.properties.hibernate.jdbc.batch_size = 100000 설정 했을 경우
     *  * save() : 16분 7초 걸림
     *  * saveAll() : 26초 걸림
     *
     * */
    @Test
    void BulkInsert_2_JPA_Sequence사용() {

        LocalDateTime start = LocalDateTime.now();

        List<Post_Sequence> postList = new ArrayList<>();

        for(int i=0;i<100000; i++){
            postList.add(
                    Post_Sequence.builder()
                            .content("saveAllBulk() _ using JPA - sequence: "+i)
                            .build()
            );

//            postSequenceRepository.save(
//                    Post_Sequence.builder()
//                            .content("saveAllBulk() _ using JPA - sequence: "+i)
//                            .build()
//            );
        }
        postSequenceRepository.saveAll(postList);

        LocalDateTime end = LocalDateTime.now();
        System.out.println("총 걸린시간(s) = " + Duration.between(start, end).getSeconds());
    }

    /**
     * 3. jpa에서 Id의 GeneratedValue를 table로 설정
     * * table 전략 : 키 생성 전용 테이블을 하나 만들어서 데이터베이스 시퀀스를 흉내내는 전략 (but, 성능적인 손해)
     *
     * ============================================
     * # spring.jpa.properties.hibernate.jdbc.batch_size = 100000 설정 안했을 경우
     * Hibernate:
     *     insert
     *     into
     *         post_table
     *         (content, id)
     *     values
     *         (?, ?)
     *
     *  * save() 결과 :
     *  총 걸린시간(s) = 1062
     *
     *  * saveAll() 결과 :
     *  총 걸린시간(s) = 85
     *
     *
     * # spring.jpa.properties.hibernate.jdbc.batch_size = 100000 설정 했을 경우
     * insert into post_table (content,id) values ('saveAllBulk() _ using JPA - table: 0',300002),('saveAllBulk() _ using JPA - table: 1',300003),('saveAllBulk() _ using JPA - table: 2',300004)...
     *  * save() 결과 :
     *  총 걸린시간(s) = 985
     *
     *  * saveAll() 결과 :
     *  총 걸린시간(s) = 27
     *
     * ============================================
     *
     * # spring.jpa.properties.hibernate.jdbc.batch_size = 100000 설정 안했을 경우
     *  * save() : 17분 7초 걸림
     *  * saveAll() : 1분 25초 걸림
     *
     * # spring.jpa.properties.hibernate.jdbc.batch_size = 100000 설정 했을 경우
     *  * save() : 16분 4초 걸림
     *  * saveAll() : 27초 걸림
     *
     * */
    @Test
    void BulkInsert_3_JPA_Table사용() {

        LocalDateTime start = LocalDateTime.now();

        List<Post_Table> postList = new ArrayList<>();

        for(int i=0;i<100000; i++){
            postList.add(
                    Post_Table.builder()
                            .content("saveAllBulk() _ using JPA - table: "+i)
                            .build()
            );

//            postTableRepository.save(
//                    Post_Table.builder()
//                            .content("saveAllBulk() _ using JPA - table: "+i)
//                            .build()
//            );
        }
        postTableRepository.saveAll(postList);

        LocalDateTime end = LocalDateTime.now();
        System.out.println("총 걸린시간(s) = " + Duration.between(start, end).getSeconds());
    }


    /**
     * Hibernate에서 flush는 아래와 같이 동작한다.
     *
     * 1. 트랜잭션을 커밋하기 전
     * 2. 영속성 컨텍스트의 쓰기 지연 저장소에 대기 중인 엔터티의 작업과 겹치는 JPQL 쿼리를 실행하기 전
     * 3. 네이티브 SQL 쿼리를 실행하기 전
     *
     * */

    /**
     * @Transactional이 필요한 곳
     * 1. DB  변경 작업(쓰기 작업)
     * 2. 여러 작업을 하나의 단위로 묶어야 할때 (ACID) 보장 => 원자성 보장
     * 3. 다중 데이터베이스 작업 (복수의 테이블/엔티티 처리) => 일부 데이터는 성공하고 일부는 실패하는 불일치 상태 예방
     * 4. 복구할 수 없는 중요한 작업일 때 => rollback이 필요할 때 (ex.상품 구매 후 재고 감소가 되지 않았을 때 취소하기 위해)
     * */
//    1. DB  변경 작업
//    @Transactional
//    public void createOrder(Order order) {
//        orderRepository.save(order);  // 데이터베이스에 새로운 주문 추가
//        paymentService.processPayment(order);  // 결제 처리
//    }

//    2. 여러 작업을 하나의 단위로 묶어야 할때 (전체 로직이 실행되거나 롤백되야 할 때)
//    @Transactional
//    public void transferMoney(Account fromAccount, Account toAccount, BigDecimal amount) {
//        accountService.withdraw(fromAccount, amount);  // 출금
//        accountService.deposit(toAccount, amount);     // 입금
//    }

//    3. 하나의 메서드가 여러 개의 테이블에 대해 데이터를 수정할 때
//    @Transactional
//    public void updateCustomerAndOrder(Customer customer, Order order) {
//        customerRepository.save(customer);  // 고객 정보 수정
//        orderRepository.save(order);        // 주문 정보 수정
//    }

//    4. rollback이 필요할 때
//    @Transactional
//    public void completePurchase(Purchase purchase) {
//        inventoryService.reduceStock(purchase.getProductId(), purchase.getQuantity());
//        paymentService.processPayment(purchase);
//    }
}


//출처 : https://velog.io/@joonghyun/Spring-JPA-Save-vs-SaveAll-vs-Bulk-Insert