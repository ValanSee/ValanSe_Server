package com.valanse.valanse.domain;

import com.valanse.valanse.domain.enums.*;
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
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // uuid

    private String socialId; // kakao id

    @Column(nullable = false, unique = true)
    private String email; // 카카오 이메일

    private String name; // 카카오 이름

    private String profile_image_url; // 프로필 이미지

    @Enumerated(EnumType.STRING)
    @Builder.Default // 디폴트값을 user로 설정함
    private Role role = Role.USER;


    @Enumerated(EnumType.STRING)
    @Builder.Default
    private SocialType socialType = SocialType.KAKAO;

    private String kakaoAccessToken;

    private String kakaoRefreshToken;
}
