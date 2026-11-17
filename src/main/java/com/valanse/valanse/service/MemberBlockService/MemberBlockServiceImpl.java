package com.valanse.valanse.service.MemberBlockService;

import com.valanse.valanse.common.api.ApiException;
import com.valanse.valanse.common.message.MemberErrorMessage;
import com.valanse.valanse.domain.Member;
import com.valanse.valanse.domain.mapping.MemberBlock;
import com.valanse.valanse.dto.MemberBlock.MemberBlockListResponse;
import com.valanse.valanse.repository.MemberBlockRepository;
import com.valanse.valanse.repository.MemberProfileRepository;
import com.valanse.valanse.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
/**
 * 회원 차단/차단 해제/차단 목록 조회를 처리하는 서비스 코드입니다.
 */
public class MemberBlockServiceImpl implements MemberBlockService {

    private final MemberBlockRepository memberBlockRepository;
    private final MemberRepository memberRepository;
    private final MemberProfileRepository memberProfileRepository;

    /**
     * 대상 회원을 차단합니다. 이미 차단돼 있으면 아무 것도 하지 않습니다(멱등).
     * 동시 요청(더블 탭)으로 유니크 제약에 걸려도 성공으로 처리하기 위해 바깥 트랜잭션을 두지 않고,
     * 저장은 레포지토리 자체 트랜잭션에서 수행합니다.
     */
    @Override
    public void block(Long blockerId, Long blockedId) {
        if (blockerId.equals(blockedId)) {
            throw new ApiException(MemberErrorMessage.CANNOT_BLOCK_SELF.message(), HttpStatus.BAD_REQUEST);
        }

        Member blocker = findActiveMember(blockerId);
        Member blocked = findActiveMember(blockedId);

        if (memberBlockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId)) {
            return;
        }

        try {
            memberBlockRepository.saveAndFlush(MemberBlock.builder()
                    .blocker(blocker)
                    .blocked(blocked)
                    .build());
        } catch (DataIntegrityViolationException e) {
            // 동시에 들어온 같은 차단 요청이 먼저 저장된 경우 - 이미 차단된 상태이므로 성공으로 본다.
            if (!memberBlockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId)) {
                throw e;
            }
        }
    }

    /**
     * 차단을 해제합니다. 차단돼 있지 않아도 성공으로 처리합니다(멱등).
     */
    @Override
    @Transactional
    public void unblock(Long blockerId, Long blockedId) {
        memberBlockRepository.deleteByBlockerIdAndBlockedId(blockerId, blockedId);
    }

    /**
     * 내가 차단한 회원 목록을 최신 차단순으로 조회합니다.
     */
    @Override
    @Transactional(readOnly = true)
    public MemberBlockListResponse getBlocks(Long blockerId) {
        List<MemberBlock> memberBlocks = memberBlockRepository.findAllByBlockerIdWithBlocked(blockerId);
        Map<Long, String> nicknamesByMemberId = getNicknamesByMemberIds(memberBlocks.stream()
                .map(block -> block.getBlocked().getId())
                .toList());

        List<MemberBlockListResponse.BlockedMember> blocks = memberBlocks.stream()
                .map(block -> new MemberBlockListResponse.BlockedMember(
                        block.getBlocked().getId(),
                        nicknamesByMemberId.get(block.getBlocked().getId()),
                        block.getCreatedAt()
                ))
                .toList();
        return new MemberBlockListResponse(blocks);
    }

    private Map<Long, String> getNicknamesByMemberIds(List<Long> memberIds) {
        if (memberIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> nicknames = new HashMap<>();
        memberProfileRepository.findAllByMemberIdIn(memberIds).stream()
                .filter(profile -> profile.getMember() != null && profile.getNickname() != null)
                .forEach(profile -> nicknames.putIfAbsent(profile.getMember().getId(), profile.getNickname()));
        return nicknames;
    }

    private Member findActiveMember(Long memberId) {
        return memberRepository.findByIdAndDeletedAtIsNull(memberId)
                .orElseThrow(() -> new ApiException(MemberErrorMessage.MEMBER_NOT_FOUND.message(), HttpStatus.NOT_FOUND));
    }
}
