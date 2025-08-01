package study.querydsl.repository;

import com.querydsl.core.QueryResults;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.dto.QMemberTeamDto;

import java.util.List;

import static org.springframework.util.StringUtils.hasText;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

public class MemberRepositoryImpl implements MemberRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    public  MemberRepositoryImpl(EntityManager em) {
        queryFactory = new JPAQueryFactory(em);
    }


    @Override
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

    @Override
    public Page<MemberTeamDto> searchPageSimple(MemberSearchCondition condition, Pageable pageable) {
        QueryResults<MemberTeamDto> memberTeamDtoQueryResults = queryFactory
                .select(new QMemberTeamDto(
                        member.id.as("memberId"),
                        member.username,
                        member.age,
                        team.id.as("teamId"),
                        team.name.as("teamName")
                ))
                .from(member)
                .leftJoin(member.team, team)
                .where(usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                )
                //offset 몇번째부터 시작할건지
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetchResults(); //fetchResults : 전체 카운트를 기반으로 함 / 현재 페이지에 필요한 데이터만 가져오는 쿼리 + 전체 레코드 수를 구하는 COUNT 쿼리도 같이 실행됨
        //전체 카운트 기반은 성능 최적화를 위해 - OrderBy 정렬 안씀 : COUNT(*) 정렬과 상관없이 수만 세면 되기때문에
        //전체 카운트 기반으로 한다는것은 = Count(전체 데이터 수)를 기준으로 페이징 정보를 만드는것
        List<MemberTeamDto> content = memberTeamDtoQueryResults.getResults(); // 현재 페이지 데이터 (limit wjrdydehla)
        long total = memberTeamDtoQueryResults.getTotal(); //전체 데이터 수 (limit 적용 안됨)

        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public Page<MemberTeamDto> searchPageComplex(MemberSearchCondition condition, Pageable pageable) {
        //실제 데이터 조회 쿼리 *LIMIT 적용
        List<MemberTeamDto> results = getMemberTeamDtos(condition, pageable);

        //두번째 쿼리 *전체 개수 조회 쿼리
        long total = queryFactory
                .select(member)
                .from(member)
                .leftJoin(member.team, team)
                .where(usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())

                )
                .fetchCount();


        return new PageImpl<>(results, pageable, total);    }

    private List<MemberTeamDto> getMemberTeamDtos(MemberSearchCondition condition, Pageable pageable) {
        List<MemberTeamDto> results = queryFactory
                .select(new QMemberTeamDto(
                        member.id.as("memberId"),
                        member.username,
                        member.age,
                        team.id.as("teamId"),
                        team.name.as("teamName")
                ))
                .from(member)
                .leftJoin(member.team, team)
                .where(usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                )
                //offset 몇번째부터 시작할건지
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
        return results;
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

