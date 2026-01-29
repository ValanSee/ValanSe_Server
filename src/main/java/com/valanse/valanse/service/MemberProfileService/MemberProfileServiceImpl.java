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

        // ✅ 추가: MBTI 검증
        if (dto.mbtiIe() == null || dto.mbtiTf() == null) {
            throw new IllegalArgumentException("MBTI를 모두 선택해주세요");
        }

        if (dto.mbti() == null || dto.mbti().length() != 4) {
            throw new IllegalArgumentException("MBTI는 4자리여야 합니다 (예: ENFP)");
        }

        // 기존 프로필 조회
        Optional<MemberProfile> existingProfileOpt = memberProfileRepository.findByMemberId(userId);

        if (existingProfileOpt.isPresent()) {
            // ✅ 수정: 기존 프로필이 있는 경우
            MemberProfile profile = existingProfileOpt.get();

            // 닉네임이 변경되었을 때만 중복 체크
            if (!profile.getNickname().equals(dto.nickname())) {
                if (memberProfileRepository.existsByNickname(dto.nickname())) {
                    throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
                }
            }

            // 기존 객체에 값만 덮어쓰기
            profile.update(dto.nickname(), dto.gender(), dto.age(), dto.mbtiIe(), dto.mbtiTf(), dto.mbti());
            memberProfileRepository.save(profile);
        } else {
            // ✅ 신규 생성: 무조건 중복 체크
            if (memberProfileRepository.existsByNickname(dto.nickname())) {
                throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
            }

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
        // 0. 전체 공백 허용 x
        if (nickname == null || nickname.isBlank()) {
            return false;
        }

        // 1. 특수문자/이모지 한글 자음,모음 차단
        if (!nickname.matches("[a-zA-Z가-힣0-9]+")){
            return false;
        }

        // 2. 한글/ 영어 혼합금지
        boolean hasEnglish = nickname.matches(".*[a-zA-Z].*");
        boolean hasKorean = nickname.matches(".*[가-힣].*");
        if (hasEnglish && hasKorean) {
            return false;
        }

        // 3. 길이제한 (한글 8/ 영어 16)
        int length = nickname.length();

        if (hasKorean && length > 8) {
            return false;
        }
        else if (hasEnglish && length > 16) {
            return false;
        }

        // 4. 같은 문자 4회 이상 반복 (예: aaaa, ㅋㅋㅋㅋ, ㅏㅏㅏㅏ)
        if (nickname.matches(".*(.)\\1{3,}.*")) {
            return false;
        }

        // 5. 반복되는 문자열 패턴 감지 (예: ililil, ababab)
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
        // 6. 중복 문자 제외하고 글자 수
        if (nickname.codePoints().distinct().count() <= 1) {
            return false;
        }
        return true;
    }

    private static final Set<String> BAD_WORDS = Set.of(
            "시발", "씨발", "병신", "개새끼",
            "섹스", "보지", "자지", "좆", "고아",
            "애미"
    );

    @Override
    public boolean isCleanNickname(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            return false;
        }

        String lower = nickname.toLowerCase().replaceAll("[^가-힣a-zA-Z0-9]", "");

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