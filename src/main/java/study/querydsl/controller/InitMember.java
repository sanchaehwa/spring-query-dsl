package study.querydsl.controller;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.Team;

//Member 초기화
@Profile("local")
@Component
@RequiredArgsConstructor
public class InitMember {

    private final InitMemberService initMemberService;
    //profile local 설정 해놔서 @PostConstruct 실행되고
    @PostConstruct
    public void init() {
        initMemberService.init();
    }

    //-> DB에 한번에 데이터를 다 넣고 시작함
    @Component
    static class InitMemberService{

        @PersistenceContext
        private EntityManager em;

        //데이터 초기화 로직
        @Transactional
        public void init(){
            Team teamA = new Team("teamA");
            Team teamB = new Team("teamB");
            em.persist(teamA);
            em.persist(teamB);

            for (int i=0; i < 100; i++){
                //50명은 teamA --- 50명은 teamB에 속하도록
                Team selectedTeam = i % 2 == 0 ? teamA : teamB;
                em.persist(new Member("member" + i, i, selectedTeam));
            }
        }

    }
}
