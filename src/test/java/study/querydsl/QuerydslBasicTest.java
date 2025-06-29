package study.querydsl;

import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;
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
import static study.querydsl.entity.QTeam.team;

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
    //집합
    @Test
    public void aggregation(){
        List<Tuple> result = jpaQueryFactory
                .select( //대상이 두개 이상 튜플이나 DTO로 조회해야함.
                        member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min())
                .from(member)
                .fetch();
        Tuple tuple = result.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
    }
    /*
    팀의 이름과 각 팀의 평균 연령을 구하라
     */
    @Test
    public void group() throws Exception{
        List<Tuple> result = jpaQueryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);

        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);

        /*
       // SQL 형태로 해석한 쿼리 설명
                SELECT t.name, AVG(m.age)         -- 팀 이름과 해당 팀에 속한 멤버들의 평균 나이 조회
                FROM member m                     -- 기준 테이블은 member
                JOIN m.team t                     -- member가 소속된 team과 내부 조인
                GROUP BY t.name                   -- 팀 이름 기준으로 그룹핑
        */
     }
     //팀 A에 소속된 모든 회원
     @Test
     public void join() {
         List<Member> result = jpaQueryFactory
                 .selectFrom(member) // FROM Member
                 .join(member.team, team) // 기본 Inner Join: member.team 과 team 간에 매칭되는 데이터가 있는 경우만 조회됨
                 //.leftJoin(member.team, team) // Left Join: member 는 모두 유지되며, 연관된 team 이 없으면 null 로 채워짐
                 //.rightJoin(member.team, team) // (참고) Right Join: team 은 모두 유지되며, 연관된 member 가 없으면 null 로 채워짐
                 .where(team.name.eq("teamA")) // team 이름이 'teamA' 인 경우만 필터링
                 .fetch();

         assertThat(result)
                 .extracting("username")
                 .containsExactly("memberA", "memberB");
     }

    //연관관계 없이 Join - 세타조인, 회원의 이름이 팀 이름과 같은 회원 조회
    @Test
    public void thetaJoin() {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        // 두 테이블(member, team)은 연관관계가 없지만,
        // 모든 조합을 만든 후 조건 (member.username == team.name)에 맞는 데이터만 필터링하는 방식
        // 즉, 이름이 우연히 같으면 임의 기준을 세워 조인하는 방식 → 세타조인(Theta Join)

        List<Member> result = jpaQueryFactory.select(member)
                .from(member, team) //연관 관게가 없는 두 테이블을 나란히 명시한뒤에
                .where(member.username.eq(team.name)) //조인을 하고 필터링해서 원하는 정보만
                .fetch();
        assertThat(result)
                .extracting("username")
                .containsExactly("teamA", "teamB");
    }
    /*
    회원과 팀을 조인하면서, 팀 이름이 teamA 인 팀만 조회, 회원은 모두 조회
     */
    @Test
    public void join_on_filtering() {
        List<Tuple> result = jpaQueryFactory
                .select(member, team) //selectFrom 이 아닌, From 만 정의하면  이후에 select() 별도로 지정해줘야함 - 조인하거나 복잡한 쿼리 구성할때는 이 방법이 좋음
                .from(member)
                .leftJoin(member.team, team).on(team.name.eq("teamA")) //teamA가 아닌 회원은  null 채워서 반환 *leftjoin = on 절
                //on(team.name.eq("teamA")) == where(team.name.eq("teamA"))
                .fetch();
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }
    /*
    연관관계 없는 엔티티 외부조인
    회원의 이름이 팀 이름과 같은 대상 외부 조인
     */
    @Test
    public void join_on_no_relation(){
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Tuple> result = jpaQueryFactory
                .select(member,team)
                .from(member)
                .leftJoin(team).on(member.username.eq(team.name))
                .fetch();
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @PersistenceUnit
    EntityManagerFactory emf;

    //주로 성능최적화를 위해 Fetch Join을 씀
    @Test
    public void fetchJoinNo(){
        //DB에 우선 반영
        em.flush();
        //영속성 컨텍스트 비우고
        em.clear();

        Member findMember = jpaQueryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("패치 조인 미적용").isFalse();

    }


    @Test
    public void fetchJoinUse(){
        //DB에 우선 반영
        em.flush();
        //영속성 컨텍스트 비우고
        em.clear();

        Member findMember = jpaQueryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("패치 조인 적용").isFalse();

    }


}
