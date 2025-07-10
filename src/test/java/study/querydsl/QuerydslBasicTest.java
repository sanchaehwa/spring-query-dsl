package study.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.UserDto;
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
    public void before() {
        jpaQueryFactory = new JPAQueryFactory(em); //필드레벨로 가지고와도 됨.(초기화)

        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member memberA = new Member("memberA", 10, teamA);
        Member memberB = new Member("memberB", 20, teamA);

        Member memberC = new Member("memberC", 30, teamB);
        Member memberD = new Member("memberD", 40, teamB);
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
    public void search() {
        Member findMember = jpaQueryFactory
                .selectFrom(member)
                .where(member.username.eq("memberA")
                        //.and(member.age.eq(10)))
                        .and(member.age.between(10, 30))) //10~30 범위안에서 찾기
                .fetchOne();
        assertThat(findMember.getUsername()).isEqualTo("memberA");
    }

    @Test
    public void searchAndParam() {
        Member findMember = jpaQueryFactory
                .selectFrom(member)
                .where(member.username.eq("memberA"), member.age.eq(10)) //and랑 똑같음
                // .and(member.age.eq(10)))
                .fetchOne(); //단건 조회 - list 조회 fetch() : 없으면 Null 반환
        assertThat(findMember.getUsername()).isEqualTo("memberA");
    }

    @Test
    public void resultFetch() {
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
    public void sort() {
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        List<Member> result = jpaQueryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();
        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);

        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();
    }

    //페이징
    @Test
    public void paging1() {
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
    public void aggregation() {
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
    public void group() throws Exception {
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
    public void join_on_no_relation() {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Tuple> result = jpaQueryFactory
                .select(member, team)
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
    public void fetchJoinNo() {
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
    public void fetchJoinUse() {
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

    @Test
    public void subQuery() {
        QMember memberSub = new QMember("memberSub"); //서브쿼리에서 사용할 Q타입 인스턴스
//QueryDSL은 하나의 쿼리 안에 같은 엔티티(Member)를 두번 참조할수 없으니깐 별칭이 다른 인스턴스 생성
        List<Member> result = jpaQueryFactory
                .selectFrom(member) //메인 쿼리 대상의 Member : 싱글톤 사용가능 (싱글톤 -> QueryDSL이 자동으로 생성해주는것)
                .where(member.age.eq(
                        JPAExpressions
                                .select(memberSub.age.max()) //
                                .from(memberSub)
                ))
                .fetch();
        assertThat(result).extracting("age")
                .containsExactly(40);
    }

    /**
     * 나이가 평균  이상인 회원 조회
     */
    @Test
    public void subQueryGoe() {

        QMember memberSub = new QMember("memberSub");
        List<Member> result = jpaQueryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        JPAExpressions
                                .select(memberSub.age.avg())
                                .from(memberSub)
                ))
                .fetch();
        assertThat(result).extracting("age")
                .containsExactly(30, 40);

    }

    //IN절
    @Test
    public void subQueryIn() {

        QMember memberSub = new QMember("memberSub");
        List<Member> result = jpaQueryFactory
                .selectFrom(member)
                .where(member.age.in(
                        JPAExpressions
                                .select(memberSub.age)
                                .from(memberSub)
                                .where(memberSub.age.gt(10)) //10보다 큰 값들의 집합에 포함되는지
                ))
                .fetch();
        assertThat(result).extracting("age")
                .containsExactly(20, 30, 40);
    }

    //select절
    @Test
    public void selectSubQuery() {
        QMember memberSub = new QMember("memberSub");
        List<Tuple> result = jpaQueryFactory
                .select(member.username,
                        JPAExpressions
                                .select(memberSub.age.avg()) //나이의 평균
                                .from(memberSub))
                .from(member)
                .fetch();
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    //Case문
    @Test
    public void basicCase() {
        List<String> result = jpaQueryFactory
                .select(member.age
                        .when(10).then("열살 ") //10살이면 열살
                        .when(20).then("스무살") //20살이면 스무살
                        .otherwise("기타")) //그외는 기타
                .from(member)
                .fetch();
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void complexCase() {
        List<String> result = jpaQueryFactory
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20살")
                        .when(member.age.between(21, 30)).then("21~30살")
                        .otherwise("기타"))
                .from(member)
                .fetch();
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void contant() {
        List<Tuple> result = jpaQueryFactory
                .select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    //문자 더하기
    @Test
    public void concat() {
        //username_age
        List<String> result = jpaQueryFactory
                .select(member.username.concat("_").concat(member.age.stringValue())) //stringValue 로 바꿨기 때문에 캐스팅 된것
                .from(member)
                .where(member.username.eq("memberA"))
                .fetch();
        for (String s : result) {
            System.out.println("s = " + s);
        }

    }

    //단일
    @Test
    public void simpleProjection() {
        List<String> result = jpaQueryFactory
                .select(member.username)
                .from(member)
                .fetch(); //String 객체타입 - Member 객체 * 프렌젝션 대상이 하나인것
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    //QueryDSL 의 Tuple을 사용 => QueryDSl의 종속 타입이라는것 : Tuple 을 레포지토리말고 서비스 계층에서 튜플을 그대로 받아오면 , 나중에 QueryDSL이 아닌 다른 ORM으로 바꾸면 서비스단도 바꿔야하니깐, DTO로 변환 해서 반환해야함
    //프로젝션 대상이 둘 이상일때 : 튜플 조회
    @Test
    public void tupleProjection() {
        List<Tuple> result = jpaQueryFactory //반환 타입 : Tuple * 여러개의 값이 넘어오니깐
                .select(member.username, member.age)
                .from(member)
                .fetch();
        for (Tuple tuple : result) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);

            System.out.println("username = " + username);
            System.out.println("age = " + age);
        }
    }

    //DTO
    @Test
    public void findDtoByJPQL() {
        List<MemberDto> result = em.createQuery("select new study.querydsl.dto.MemberDto(m.username, m.age) from Member m", MemberDto.class)
                .getResultList(); //JPQl에서 제공하는 new Operation (JPQL 쿼리 결과를 DTO로 직접 매핑할 때 사용 ) -- 생성자 꼭 있어야함

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }

    }

    //Property 작동 방법 -- Setter를 활용한 방법
    @Test
    public void findDtoBySetter() {
        List<MemberDto> result = jpaQueryFactory //setter 로 인젝션 * JPAQueryFactory 객체를 주입(Injection)할때, (new) 생성자가 아닌, Setter 매서드를 이용해서 넣어준다
                .select(Projections.bean(MemberDto.class, member.username, member.age)) //  DTO는 Projections.bean() 방식에서 Setter 매서드로 값이 주입
                .from(member)
                .fetch();
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    // -- 필드를 활용한 방식
    @Test
    public void findDtoByField() {
        List<MemberDto> result = jpaQueryFactory
                .select(Projections.fields(MemberDto.class, member.username, member.age)) //DTO 필드에 값을 꽂아넣는거
                .from(member)
                .fetch();
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }

    }
    //--생성자 접근 방식
    @Test
    public void findDtoByConstructor() {
        List<MemberDto> result = jpaQueryFactory
                .select(Projections.constructor(MemberDto.class, member.username, member.age)) //타입이 일치해야 호출 - (Username String, Age Int) (타입이 일치하지 않을 경우, 컴파일 오류는 못 잡고, 런타임 오류가 발생함(코드를 실행하는 순간에서 문제를 찾을 수 있다는 것 )
                .from(member)
                .fetch();
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }

    }
    @Test
    public void findUserDtoByField() {
        QMember memberSub = new QMember("memberSub");
        List<UserDto> result = jpaQueryFactory
                .select(Projections.constructor(UserDto.class, member.username.as("name"), //이 방식이 더 좋음
                        //ExpressionUtils.as(member.username, "name")
                        //이름이 없을때 (서브 쿼리 사용)
                        ExpressionUtils.as(JPAExpressions
                                .select(memberSub.age.max()) //10-20-30-40 살이 아니라, 40살로 찍어짐
                                .from(memberSub), "age")

                )) // 필드명이 다르면 매칭이 안됨 - 별칭 설정(alias)

                .from(member)
                .fetch();
        for (UserDto userDto : result) {
            System.out.println("memberDto = " + userDto);
        }

    }

    //DTO - Q파일로 생성 : QueryDSl로 생성자 접근 방식 (생성자 접근 방식과 달리 QueryDSl로 쓰면 컴파일 에러가 나기때문에 실행하지않아도 문제점 파악 가능) **** DTO 자체가 QueryDSl에 의존적이기때문에 깔끔하게 DTO를 가지고온다 하면 (Basic) 생성자 방식 사용
    @Test
    public void findDtoByQueryProjection() {
        List<MemberDto> result = jpaQueryFactory //생성자를 그대로 가져오기때문에, 타입 맞춰줌 - 안정적으로 DTO Q파일을 가져올수있다
                .select(new QMemberDto(member.username, member.age)) //queryDSL 에서 username , age만 타입으로 설정 다른 타입이 들어오면 에러 발생
                .from(member)
                .fetch();
        for(MemberDto memberDto : result){
            System.out.println("memberDto = " + memberDto);
        }
    }
    @Test
    public void dynamicQuery_BooleanBuilder() {
        String usernameParam = "memberA";
        Integer ageParam = null; //둘다 Null이면 조건문에 안들어감 조건이 - 하나라도 Null이 아니면, 그 조건이 들어감

        List<Member> result = searchMember1(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember1(String usernameCond, Integer ageCond) {
       BooleanBuilder builder = new BooleanBuilder(); //QueryDSl에서 조건을 동적으로 추가할 때
       if (usernameCond != null) {
           builder.and(member.username.eq(usernameCond));
       }
       if (ageCond != null) {
           builder.and(member.age.eq(ageCond));
       }

        return jpaQueryFactory
                .selectFrom(member)
                .where(builder) //위에 Builder에 대한 결과가 출력됨
                .fetch();
    }
    //동적 쿼리 - Where 다중 파라미터 사용 : 여러 조건들을 조합할때 유용
    @Test
    public void dynamicQuery_WhereParam() {
        String usernameParam = "memberA";
        Integer ageParam = null;

        List<Member> result = searchMember2(usernameParam,ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember2(String usernameCond, Integer ageCond) {
        return  jpaQueryFactory
                .selectFrom(member)
               // .where(usernameEq(usernameCond), ageEq(ageCond))
                .where(allEq(usernameCond, ageCond))
                .fetch();
    }


    private BooleanExpression usernameEq(String usernameCond) {
//        if (usernameCond == null) {
//            return null;
//        }else{
//            return member.username.eq(usernameCond);
//
//        }
        //간단할 경우에는 삼항 연산자 사용
        return usernameCond != null ? member.username.eq(usernameCond) : null; //null이 아닌경우  member.username.eq(usernameCond) 반환 --- null인 경우에는 null 반환
    }

    private BooleanExpression ageEq(Integer ageCond) {
        return ageCond != null ? member.age.eq(ageCond) : null;

    }
    //두 조건을 모두 만족해야한다면 -> AND 로 두 조건을 조합해야하는데 위 조건들을 조합하려면 Predicate가 아닌, BooleanExpression을 써야함
    private BooleanExpression allEq(String usernameCond, Integer ageCond) {
        return usernameEq(usernameCond).and(ageEq(ageCond));
    }
    //수정 ,삭제 같은 것을 한번에 처리할 수 있는 배치 쿼리 => 벌크 연산 : 대량의 데이터를 수정할 떄 사용
    @Test
    public void bulkUpdate() {
        //벌크 연산은 영속성 컨텍스트를 무시하고 DB에 바로 쿼리 날림 : DB의 상태와 영속성 컨텍스트의 상태가 달라짐
        //memberA = 10 -> DB 비회원 영속성 컨텍스트 memberA
        //memberB = 20 -> DB 비회원 // memberB
        //memberC = 30 -> DB memberC // memberC
        //memberD = 40 -> DB memberD //memberD
        long count = jpaQueryFactory
                .update(member)
                .set(member.username, "비회원")
                .where(member.age.lt(28)) //28살 이하인 맴버의 이름을 비회원으로 바꿈
                .execute();
        //영속성 컨텍스트의 값을 초기화하면서 -> 영속성 컨텍스트에 이미 값이 있는 상태가 아니니깐 DB에서 가져온 값을 영속성 컨텍스트에 넣을수 있음 : 벌크 연산 문제점 해결
        em.flush();
        em.clear();
        List<Member> result = jpaQueryFactory.selectFrom(member).fetch();
        //DB - 값을 넣어줌 이때 JPA가 영속성컨텍스트에 이미 값이 있으면 (값이) 그러면 DB에서 가져온 값을 버림 =>영속성 컨텍스트가 항상 우선권을 가짐 :
        for (Member member : result) {
            System.out.println("member = " + member);
        }
    }

    @Test
    public void bulkAdd(){
        long count = jpaQueryFactory
                .update(member)
                .set(member.age, member.age.add(1)) //multiply : 곱
                .execute();
    }
    @Test
    public void bulkDelete(){
        long execute = jpaQueryFactory
                .delete(member)
                .where(member.age.gt(18))
                .execute(); //18이상인 맴버인 경우에는 삭제
    }

    //SQL Function 호출하기 (JPA와 같은 Dialect에 등록된 내용만 호출할 수 있음)
    @Test
    public void sqlFunction() {
        List<String> result = jpaQueryFactory
                .select(Expressions.stringTemplate(
                        "function('replace', {0}, {1}, {2})",//첫번쨰 파라미터 -> Member.username, 두번쨰 파라미터 -> Member , 세번찌 파라미터 'M
                        member.username, "member", "M")) //Member 를 M으로 바꿔서 조회
                .from(member)
                .fetch();
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }
    @Test
    public void sqlFunction2() {
        List<String> result = jpaQueryFactory
                .select(member.username)
                .from(member)
//                .where(member.username.eq(Expressions.stringTemplate(
//                        "function('lower',{0})", member.username //소문자로 바꾸는 경우
//                )))
                .where(member.username.eq(member.username.lower()))
                .fetch();
        for (String s: result) {
            System.out.println("s = " + s);
        }
    }
}
