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
@Table(indexes = @Index(name = "idx_member_deleted_at", columnList = "deleted_at"))
/**
 * Member 정보를 저장하고 연관관계를 표현하는 JPA 도메인 엔티티 코드입니다.
 */
public class Member extends BaseEntity {

    /**
     * 탈퇴한 회원(soft delete 직후 ~ 퍼지 이후)이 작성한 콘텐츠의 작성자 닉네임으로 노출하는 문구입니다.
     */
    public static final String WITHDRAWN_NICKNAME = "탈퇴한 사용자";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // uuid

    private String socialId; // kakao id 카카오가 제공하는 고유 사용자 id

    @Column(nullable = true)
    private String email; // 카카오 이메일

    private String name; // 카카오 이름

    private String profile_image_url; // 프로필 이미지

    @Enumerated(EnumType.STRING)
    @Builder.Default // 디폴트값을 user로 설정함
    private Role role = Role.USER;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private SocialType socialType = SocialType.KAKAO;

    private String kakaoAccessToken; // 카카오 API 호출용(짧은 만료시간)

    private String kakaoRefreshToken; // AccessToken 갱신용

    private String appleRefreshToken; // 탈퇴 시 Apple 연결 해제(revoke)용

    private String appleClientId; // refresh token 을 발급받은 client_id (앱 번들 ID 또는 웹 Services ID)

    private String nickname;

    @Builder.Default
    private boolean isBot = false;

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

    public void updateProfileImageUrl(String profileImageUrl) {
        this.profile_image_url = profileImageUrl;
    }

    /**
     * 탈퇴(soft delete)한 회원인지 판별하는 메서드입니다.
     */
    public boolean isWithdrawn() {
        return getDeletedAt() != null;
    }

    /**
     * 콘텐츠 작성자로 노출해도 되는 회원이면 그대로, 탈퇴했거나(soft delete) 퍼지돼 없으면 null을 반환합니다.
     * 탈퇴 직후부터 퍼지 이후와 똑같이 "탈퇴한 사용자"로 익명화해 보여주기 위해 사용합니다.
     */
    public static Member visibleAuthorOrNull(Member member) {
        return member == null || member.isWithdrawn() ? null : member;
    }

    public void revokeCredentials() {
        this.kakaoAccessToken = null;
        this.kakaoRefreshToken = null;
        this.appleRefreshToken = null;
        this.appleClientId = null;
    }
}
