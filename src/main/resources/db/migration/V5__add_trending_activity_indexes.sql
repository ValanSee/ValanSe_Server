CREATE INDEX `idx_member_vote_option_trending`
    ON `member_vote_option` (`created_at`, `vote_id`, `deleted_at`);

CREATE INDEX `idx_comment_trending`
    ON `comment` (`created_at`, `comment_group_id`, `deleted_at`);
