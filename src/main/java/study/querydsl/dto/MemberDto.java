package study.querydsl.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor

public class MemberDto {

    private String username;
    private int age;

    //DTO를 Q파일로 생성해주는것
    //QueryDSl에 의존적인 DTO가 됨 => 순수하지 못함
    @QueryProjection
    public MemberDto(String username, int age) {
        this.username = username;
        this.age = age;
    }

    public void setUsername(String username) {
        this.username = username;
    }



}
