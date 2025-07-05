package com.valanse.valanse.domain;

import com.valanse.valanse.domain.common.BaseEntity;
import com.valanse.valanse.domain.enums.Age;
import com.valanse.valanse.domain.enums.Gender;
import com.valanse.valanse.domain.enums.MbtiIe;
import com.valanse.valanse.domain.enums.MbtiTf;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Entity
public class MemberProfile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @MapsId
    @JoinColumn(name = "member_id")
    private Member member;

    private String nickname; // 추가 정보

    @Enumerated(EnumType.STRING)
    private Gender gender; // 추가 정보

    @Enumerated(EnumType.STRING)
    private Age age; // 추가 정보

    @Enumerated(EnumType.STRING)
    private MbtiIe mbtiIe; // 추가 정보

    @Enumerated(EnumType.STRING)
    private MbtiTf mbtiTf; // 추가 정보

    private String mbti;

    public void update(String nickname, Gender gender, Age age, MbtiIe mbtiIe, MbtiTf mbtiTf, String mbti) {
        this.nickname = nickname;
        this.gender = gender;
        this.age = age;
        this.mbtiIe = mbtiIe;
        this.mbtiTf = mbtiTf;
        this.mbti = mbti;
    }

}
