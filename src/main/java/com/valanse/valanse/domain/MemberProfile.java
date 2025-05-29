package com.valanse.valanse.domain;

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
@AllArgsConstructor // 해당 클래스 내의 모든 변수에 대하여 생성자를 만들어줌
@NoArgsConstructor // 기본 생성자를 만들어줌
@Getter
@Entity
public class MemberProfile {

    @Id
    private Long id;

    @OneToOne
    @MapsId
    @JoinColumn(name = "member_id")
    private Member member;

    private String profile_image_url; // 프로필 이미지

    private String nickname; // 추가 정보

    @Enumerated(EnumType.STRING)
    private Gender gender; // 추가 정보

    @Enumerated(EnumType.STRING)
    private Age age; // 추가 정보

    @Enumerated(EnumType.STRING)
    private MbtiIe mbtiIe; // 추가 정보

    @Enumerated(EnumType.STRING)
    private MbtiTf mbtiTf; // 추가 정보
}
