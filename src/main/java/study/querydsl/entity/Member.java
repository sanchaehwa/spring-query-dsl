package study.querydsl.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(of={"id","username","age"}) //연관관계는 제외하고 toString(매서드 객체가 가지고 있는 정보나 값들을 문자열로 만들어 리턴)
public class Member {
    @Id
    @GeneratedValue
    @Column(name="member_id")
    private Long id;
    private String username;
    private int age;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="team_id")
    private Team team;

    public Member(String username, int age,Team team) {
        this.username = username;
        this.age = age;
        if(team != null) {
            changeTeam(team);
        }
    }
    public Member(String username, int age) {
        this(username, age, null);
    }
    public Member(String username) {
        this(username, 0);
    }
    public void changeTeam(Team team) {
        this.team = team;
        team.getMembers().add(this);
    }

}
