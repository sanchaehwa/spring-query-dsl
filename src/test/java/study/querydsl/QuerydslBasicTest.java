package study.querydsl;

import com.querydsl.core.QueryResults;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.member;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @Autowired
    EntityManager em;

    //querydsl 짜려면 jpa query factory 로 시작
    JPAQueryFactory jpaQueryFactory;
    @BeforeEach
    public void before(){
        jpaQueryFactory = new JPAQueryFactory(em); //필드레벨로 가지고와도 됨.(초기화)

        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member memberA = new Member("memberA",10,teamA);
        Member memberB = new Member("memberB",20,teamA);

        Member memberC = new Member("memberC",30,teamB);
        Member memberD = new Member("memberD",40,teamB);
        em.persist(memberA);
        em.persist(memberB);
        em.persist(memberC);
        em.persist(memberD);
    }
    @Test
    public void startJPQL() {
        //member 찾기
        String qlString = "select m from Member m " +
                "where m.username = :username";
        Member findMember = em.createQuery(qlString, Member.class)
                .setParameter("username", "memberA")
                .getSingleResult();
        assertThat(findMember.getUsername()).isEqualTo("memberA");
    }
    @Test
    public void startQuerydsl() {
        //jpql의 alias 변경
        QMember m1 = new QMember("m1"); //같은 테이블을 조인해야할때,

        //QMember 판별을 위한 이름 설정 new QMember(*) : 별칭을 직접 지정하는 방법
       // QMember m = new QMember("m");
        Member findMember = jpaQueryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("memberA"))
                .fetchOne(); //fetchOne 고유한 결과를 가져오거나 결과가 없으면 null

        assertThat(findMember.getUsername()).isEqualTo("memberA");
    }
    @Test
    public void search(){
        Member findMember = jpaQueryFactory
                .selectFrom(member)
                .where(member.username.eq("memberA")
                        //.and(member.age.eq(10)))
                        .and(member.age.between(10,30))) //10~30 범위안에서 찾기
                .fetchOne();
        assertThat(findMember.getUsername()).isEqualTo("memberA");
    }
    @Test
    public void searchAndParam(){
        Member findMember = jpaQueryFactory
                .selectFrom(member)
                .where(member.username.eq("memberA"),member.age.eq(10)) //and랑 똑같음
                       // .and(member.age.eq(10)))
                .fetchOne(); //단건 조회 - list 조회 fetch() : 없으면 Null 반환
        assertThat(findMember.getUsername()).isEqualTo("memberA");
    }

    @Test
    public void resultFetch(){
//        List<Member> fetch = jpaQueryFactory.selectFrom(member).fetch();//리스트 조회
//
//        Member fetchOne = jpaQueryFactory.selectFrom(member).fetchOne();//단건조회
//
//        Member fetchFirst = jpaQueryFactory.selectFrom(member).fetchFirst();//처음 한건 조회

        QueryResults<Member> results = jpaQueryFactory.selectFrom(member).fetchResults();

        results.getTotal();
        List<Member> content = results.getResults(); //result에 대한 content가 나오게 됨.

        long total = jpaQueryFactory.selectFrom(member).fetchCount();
    }
    /**
     * 회원 정렬 순서
     * 회원 나이 내림차순 (desc)
     * 회원 이름 올림차순 (asc)
     * 회원 이름이 없으면 마지막에 출력 (nulls last)
     */
    @Test
    public void sort(){
        em.persist(new Member(null,100));
        em.persist(new Member("member5",100));
        em.persist(new Member("member6",100));

        List<Member> result = jpaQueryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();
        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull= result.get(2);

        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();
    }
//페이징
    @Test
    public void paging1(){
//        List<Member> result = jpaQueryFactory.selectFrom(member)
//                .orderBy(member.username.desc())
//                .offset(1) //페이징 지원
//                .limit(2)
//                .fetch();
//        assertThat(result.size()).isEqualTo(2);
        //전체 조회
        QueryResults<Member> queryResults = jpaQueryFactory.selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetchResults();
        assertThat(queryResults.getTotal()).isEqualTo(4);
        assertThat(queryResults.getLimit()).isEqualTo(2);
        assertThat(queryResults.getOffset()).isEqualTo(1);
        assertThat(queryResults.getResults().size()).isEqualTo(2);
    }
}
