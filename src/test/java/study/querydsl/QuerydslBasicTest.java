package study.querydsl;

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
        jpaQueryFactory = new JPAQueryFactory(em); //필드레벨로 가지고와도 됨.

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
}
