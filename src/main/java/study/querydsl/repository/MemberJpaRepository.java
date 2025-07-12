package study.querydsl.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;

import java.util.List;
import java.util.Optional;

import static study.querydsl.entity.QMember.member;

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
    public Optional<Member> findById(Long id) {
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
}
