package com.valanse.valanse.domain;

import com.valanse.valanse.domain.common.BaseEntity;
import com.valanse.valanse.domain.enums.*;
import com.valanse.valanse.domain.mapping.CommentLike;
import com.valanse.valanse.domain.mapping.MemberVoteOption;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

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

    @Column(nullable = false)
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

    private String nickname;

    @Builder.Default
    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL)
    private List<Comment> comments = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<CommentLike> commentLikes = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL)
    private List<MemberVoteOption> memberVoteOptions = new ArrayList<>();

    @OneToOne(mappedBy = "member", cascade = CascadeType.ALL, optional = true)
    private MemberProfile profile;

    @Builder.Default
    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL)
    private List<Vote> votes = new ArrayList<>();
}
