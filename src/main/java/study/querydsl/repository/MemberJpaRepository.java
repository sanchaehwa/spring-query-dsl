package study.querydsl.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import org.springframework.util.StringUtils;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.dto.QMemberTeamDto;
import study.querydsl.entity.Member;

import java.util.List;
import java.util.Optional;

import static org.springframework.util.StringUtils.hasText;
import static org.springframework.util.StringUtils.isEmpty;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

//순수 JPA 레포지토리 - QueryDSL
@Repository
@RequiredArgsConstructor
public class MemberJpaRepository {
    private final EntityManager em;
    private final JPAQueryFactory queryFactory; //JPAQueryFactory : JPA의 엔티티룰 이용하여 JPQL Query를 보다 쉽고 편리하게 작성할 수 있는 QueryDSl 도구

//    public MemberJpaRepository(EntityManager em, JPAQueryFactory queryFactory) {
//        this.em = em;
//        this.queryFactory = queryFactory;
    //Application에서 Bean으로 등록해주지않으면
            //this.queryFactory = new JPAQueryFactory(em);
//    }
    public void save(Member member) {
        em.persist(member); // JPA 영속성 컨텍스트에 등록 (DB에 저장될 준비 상태)
    }

    /*
     * QueryDSL은 JPA 기반의 타입 안전한 쿼리 빌더이다.
     * 따라서 사용하는 대상은 반드시 @Entity로 정의된 JPA 엔티티여야 한다.
     * 즉, JPA가 엔티티를 영속성 컨텍스트에서 관리하고 있어야
     * QueryDSL을 통해 해당 엔티티를 조회하거나 조건을 걸 수 있다.
     */
    public Optional<Member>  findById(Long id) {
        Member findMember = em.find(Member.class, id);
        return Optional.ofNullable(findMember);
    }
    public List<Member> findAll() {
        return em.createQuery("select m from Member m", Member.class).getResultList();
    }
    //queryDSL
    public List<Member> findAll_Querydsl(){
        return queryFactory
                .selectFrom(member)
                .fetch();
    }

    public List<Member> findByUsername(String username) {
        return  em.createQuery("select  m from Member m where m.username = :username",Member.class)
                .setParameter("username",username)
                .getResultList();
    }
    public List<Member> findByUsername_Querydsl(String username) {
        return queryFactory
                .selectFrom(member)
                .where(member.username.eq((username)))
                .fetch();
    }
    // 검색 조건이 아예 없을 경우 전체 데이터를 모두 조회하게 되기에, 조건이 없을 때는 제한(LIMIT)걸지 않으면 성능 문제가 발생할 수 있음.
    public List<MemberTeamDto> searchByBuilder(MemberSearchCondition condition) {

        BooleanBuilder builder = new BooleanBuilder();
        if(hasText(condition.getUsername())) {
            builder.and(member.username.eq(condition.getUsername()));
        }
        if(hasText(condition.getTeamName())){
            builder.and(team.name.eq(condition.getTeamName()));
        }
        if (condition.getAgeGoe() != null) {
            builder.and(member.age.goe(condition.getAgeGoe()));
        }
        if (condition.getAgeGoe() != null) {
            builder.and(member.age.goe(condition.getAgeLoe()));
        }

        return queryFactory
                .select(new QMemberTeamDto(
                        member.id.as("memberId"),
                        member.username,
                        member.age,
                        team.id.as("teamId"),
                        team.name.as("teamName")
                ))
                .from(member)
                .leftJoin(member.team,team)
                .where(builder)
                .fetch();
    }
    /*
    if(builder.getValue() == null) {
        query.limit(100)
     } -> 조건이 없을 경우 하나도 없을떄 제한을 걸어둠
     */
    public List<MemberTeamDto> search(MemberSearchCondition condition) {
        return queryFactory
                .select(new QMemberTeamDto(
                        member.id.as("memberId"),
                        member.username,
                        member.age,
                        team.id.as("teamId"),
                        team.name.as("teamName")
                ))
                .from(member)
                .leftJoin(member.team,team)
                .where(usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                )
                .fetch();
    }

    public List<Member> searchMember(MemberSearchCondition condition) {
        return queryFactory
                .selectFrom(member)
                .leftJoin(member.team,team) //select projection이 달라져도 where 재사용 가능
                .where(usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe()) //ageBetweenn?
                )
                .fetch();
    }

    private BooleanExpression ageBetween(int ageLoe, int ageGoe) {
        return ageGoe(ageLoe).and(ageGoe(ageGoe));
    }


    private BooleanExpression teamNameEq(String teamName) {
        return hasText(teamName) ? team.name.eq(teamName) : null;
    }

    private BooleanExpression usernameEq(String username) {
        return hasText(username) ? member.username.eq(username) : null;
    }
    private BooleanExpression ageLoe(Integer ageLoe) {
        return ageLoe != null ? member.age.loe(ageLoe) : null;
    }

    private BooleanExpression ageGoe(Integer ageGoe) {
        return ageGoe != null ? member.age.goe(ageGoe): null;
    }
    //BooleanExpression : .and() / .or() 조건을 조립해서 하나로 합칠 수 있다는것 (붙이고 떌수있다 근데 null를 반환해도 .where(), .and() 에서 자동으로 건너뜀)
}
