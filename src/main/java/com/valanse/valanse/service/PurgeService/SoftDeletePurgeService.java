package com.valanse.valanse.service.PurgeService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SoftDeletePurgeService {
    private final JdbcTemplate jdbcTemplate;
    private final StorageDeleteTaskRepository storageDeleteTaskRepository;
    private final PurgeProperties properties;

    @Transactional(readOnly = true)
    public PurgePreview preview(LocalDateTime now) {
        LocalDateTime cutoff = now.minusDays(properties.retentionDays());
        Timestamp cutoffTimestamp = Timestamp.valueOf(cutoff);
        Long comments = jdbcTemplate.queryForObject(
                "select count(*) from comment where deleted_at < ? and purged_at is null",
                Long.class, cutoffTimestamp);
        Long votes = jdbcTemplate.queryForObject(
                "select count(*) from vote where deleted_at < ?",
                Long.class, cutoffTimestamp);
        Long members = jdbcTemplate.queryForObject(
                "select count(*) from member where deleted_at < ?",
                Long.class, cutoffTimestamp);
        return new PurgePreview(cutoff, valueOrZero(comments), valueOrZero(votes), valueOrZero(members));
    }

    @Transactional
    public PurgeResult purgeExpired(LocalDateTime now) {
        LocalDateTime cutoff = now.minusDays(properties.retentionDays());
        int comments = anonymizeComments(cutoff);
        int votes = purgeVotes(cutoff);
        int members = purgeMembers(cutoff);
        return new PurgeResult(comments, votes, members);
    }

    private int anonymizeComments(LocalDateTime cutoff) {
        List<Long> ids = findIds("select id from comment where deleted_at < ? and purged_at is null order by id limit ?", cutoff);
        for (Long id : ids) {
            jdbcTemplate.update("delete from comment_like where comment_id = ?", id);
            jdbcTemplate.update("delete from report where report_type = 'COMMENT' and target_id = ?", id);
            jdbcTemplate.update("update comment set member_id = null, vote_option_id = null, title = null, content = '삭제된 댓글입니다', purged_at = current_timestamp where id = ?", id);
        }
        return ids.size();
    }

    private int purgeVotes(LocalDateTime cutoff) {
        List<Long> voteIds = findIds("select id from vote where deleted_at < ? order by id limit ?", cutoff);
        for (Long voteId : voteIds) {
            enqueueImageDeletes(
                    jdbcTemplate.queryForList(
                            "select image_url from vote_option where vote_id = ? and image_url is not null",
                            String.class,
                            voteId),
                    "VOTE",
                    voteId);
            List<Long> commentIds = jdbcTemplate.queryForList(
                    "select c.id from comment c join comment_group cg on c.comment_group_id = cg.id where cg.vote_id = ?",
                    Long.class, voteId);
            for (Long commentId : commentIds) {
                jdbcTemplate.update("delete from comment_like where comment_id = ?", commentId);
                jdbcTemplate.update("delete from report where report_type = 'COMMENT' and target_id = ?", commentId);
            }
            jdbcTemplate.update("update comment c set parent_id = null where c.comment_group_id in (select cg.id from comment_group cg where cg.vote_id = ?)", voteId);
            jdbcTemplate.update("delete from comment where comment_group_id in (select cg.id from comment_group cg where cg.vote_id = ?)", voteId);
            jdbcTemplate.update("delete from member_vote_option where vote_id = ?", voteId);
            jdbcTemplate.update("delete from vote_option where vote_id = ?", voteId);
            jdbcTemplate.update("delete from comment_group where vote_id = ?", voteId);
            jdbcTemplate.update("delete from report where report_type = 'VOTE' and target_id = ?", voteId);
            jdbcTemplate.update("delete from vote where id = ?", voteId);
        }
        return voteIds.size();
    }

    private int purgeMembers(LocalDateTime cutoff) {
        List<Long> memberIds = findIds("select id from member where deleted_at < ? order by id limit ?", cutoff);
        for (Long memberId : memberIds) {
            enqueueImageDeletes(
                    jdbcTemplate.queryForList(
                            "select profile_image_url from member where id = ? and profile_image_url is not null",
                            String.class,
                            memberId),
                    "MEMBER",
                    memberId);
            jdbcTemplate.update("delete from comment_like where user_id = ?", memberId);
            jdbcTemplate.update("delete from member_vote_option where member_id = ?", memberId);
            jdbcTemplate.update("delete from point_history where member_id = ?", memberId);
            jdbcTemplate.update("delete from activity_event where member_id = ?", memberId);
            jdbcTemplate.update("delete from anonymous_user_link where member_id = ?", memberId);
            jdbcTemplate.update("delete from report where member_id = ?", memberId);
            jdbcTemplate.update("delete from report where report_type = 'COMMENT' and target_id in (select id from comment where member_id = ?)", memberId);
            jdbcTemplate.update("delete from report where report_type = 'VOTE' and target_id in (select id from vote where member_id = ?)", memberId);
            jdbcTemplate.update("update comment set member_id = null where member_id = ?", memberId);
            jdbcTemplate.update("update vote set member_id = null where member_id = ?", memberId);
            jdbcTemplate.update("delete from member_profile_title where member_profile_id = ?", memberId);
            jdbcTemplate.update("delete from member_profile where member_id = ?", memberId);
            jdbcTemplate.update("delete from member where id = ?", memberId);
        }
        return memberIds.size();
    }

    private List<Long> findIds(String sql, LocalDateTime cutoff) {
        return jdbcTemplate.queryForList(sql, Long.class, Timestamp.valueOf(cutoff), properties.batchSize());
    }

    private void enqueueImageDeletes(List<String> urls, String sourceType, Long sourceId) {
        LocalDateTime now = LocalDateTime.now();
        urls.forEach(url -> storageDeleteTaskRepository.enqueue(url, sourceType, sourceId, now));
    }

    private long valueOrZero(Long value) {
        return value == null ? 0L : value;
    }
}
