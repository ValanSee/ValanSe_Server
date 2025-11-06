package com.valanse.valanse.service.MemberProfileService;

import com.valanse.valanse.domain.Member;
import com.valanse.valanse.domain.MemberProfile;
import com.valanse.valanse.dto.MemberProfile.MemberMyPageResponse;
import com.valanse.valanse.dto.MemberProfile.MemberProfileRequest;
import com.valanse.valanse.dto.MemberProfile.MemberProfileResponse;
import com.valanse.valanse.repository.MemberProfileRepository;
import com.valanse.valanse.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberProfileServiceImpl implements MemberProfileService {

    private final MemberRepository memberRepository;
    private final MemberProfileRepository memberProfileRepository;

    @Override
    public void saveOrUpdateProfile(MemberProfileRequest dto) {
        Long userId = Long.parseLong(SecurityContextHolder.getContext().getAuthentication().getName());

        Member member = memberRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다."));

        // 기존 프로필 조회
        Optional<MemberProfile> existingProfileOpt = memberProfileRepository.findByMemberId(userId);

        if (existingProfileOpt.isPresent()) {
            // 기존 객체에 값만 덮어쓰기
            MemberProfile profile = existingProfileOpt.get();
            profile.update(dto.nickname(), dto.gender(), dto.age(), dto.mbtiIe(), dto.mbtiTf(), dto.mbti());

            // 수정된 객체 저장 (영속성 때문에 이 부분은 생략도 가능)
            memberProfileRepository.save(profile);
        } else {
            // 새 객체 생성 후 저장
            MemberProfile newProfile = MemberProfile.builder()
                    .member(member)
                    .nickname(dto.nickname())
                    .gender(dto.gender())
                    .age(dto.age())
                    .mbtiIe(dto.mbtiIe())
                    .mbtiTf(dto.mbtiTf())
                    .mbti(dto.mbti())
                    .build();

            memberProfileRepository.save(newProfile);
        }
    }

    @Transactional(readOnly = true)
    @Override
    public MemberProfileResponse getProfile() {
        Long userId = Long.parseLong(SecurityContextHolder.getContext().getAuthentication().getName());

        Member member = memberRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다."));

        MemberProfile profile = memberProfileRepository.findByMemberId(userId)
                .orElse(null);

        if (profile == null) {
            return new MemberProfileResponse(null);
        }

        MemberProfileResponse.Info info = new MemberProfileResponse.Info(
                profile.getNickname(),
                profile.getGender() != null ? profile.getGender().name() : null,
                profile.getAge() != null ? profile.getAge().name() : null,
                profile.getMbtiIe() != null ? profile.getMbtiIe().name() : null,
                profile.getMbtiTf() != null ? profile.getMbtiTf().name() : null,
                profile.getMbti() != null ? profile.getMbti() : null,
                member.getRole() != null ? member.getRole() : null
        );

        return new MemberProfileResponse(info);
    }

    @Override
    public boolean isAvailableNickname(String nickname) {
        return !memberProfileRepository.existsByNickname(nickname);
    }

    @Override
    public boolean isMeaningfulNickname(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            return false;
        }

        // 1. 같은 문자 4회 이상 반복 (예: aaaa, ㅋㅋㅋㅋ, ㅏㅏㅏㅏ)
        if (nickname.matches(".*(.)\\1{3,}.*")) {
            return false;
        }

        // 2. 반복되는 문자열 패턴 감지 (예: ililil, ababab)
        for (int i = 1; i <= nickname.length() / 2; i++) {
            String pattern = nickname.substring(0, i);
            StringBuilder repeated = new StringBuilder();
            while (repeated.length() < nickname.length()) {
                repeated.append(pattern);
            }
            if (repeated.toString().equals(nickname)) {
                return false;
            }
        }

        // 3. 중복 문자 제외하고 글자 수
        if (nickname.codePoints().distinct().count() <= 1) {
            return false;
        }
        return true;
    }

    private static final Set<String> BAD_WORDS = Set.of(
            "시발", "씨발", "병신", "ㅅㅂ", "ㅂㅅ", "ㄱㅅㄲ", "개새끼"
    );

    @Override
    public boolean isCleanNickname(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            return false;
        }

        String lower = nickname.toLowerCase().replaceAll("[^가-힣a-zA-Z0-9ㄱ-ㅎㅏ-ㅣ]", "");

        for (String bad : BAD_WORDS) {
            if (lower.contains(bad)) {
                return false;
            }
        }

        return true;
    }

    @Transactional(readOnly = true)
    @Override
    public MemberMyPageResponse getMyProfile() {
        Long userId = Long.parseLong(SecurityContextHolder.getContext().getAuthentication().getName());

        MemberProfile profile = memberProfileRepository.findByMemberId(userId)
                .orElse(null);
        Member member = memberRepository.findByIdAndDeletedAtIsNull(userId).orElse(null);

        if (profile == null) {
            return new MemberMyPageResponse(null);
        }

        MemberMyPageResponse.MyPageInfo info = new MemberMyPageResponse.MyPageInfo(
                member.getProfile_image_url(),
                member.getName(),
                member.getEmail(),
                profile.getNickname(),
                profile.getGender() != null ? profile.getGender().name() : null,
                profile.getAge() != null ? profile.getAge().name() : null,
                profile.getMbti() != null ? profile.getMbti() : null
        );

        return new MemberMyPageResponse(info);
    }
}
