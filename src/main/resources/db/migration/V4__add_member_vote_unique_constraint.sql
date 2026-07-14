-- Prevent a member from recording more than one choice for the same vote after duplicate cleanup.
-- Existing duplicate rows cause the ALTER TABLE statement to fail and must be reviewed manually.

SET @member_vote_unique_exists = (
    SELECT COUNT(*)
    FROM (
        SELECT index_name, non_unique
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'member_vote_option'
        GROUP BY index_name, non_unique
        HAVING non_unique = 0
           AND GROUP_CONCAT(column_name ORDER BY seq_in_index) = 'member_id,vote_id'
    ) AS matching_unique_indexes
);

SET @add_member_vote_unique = IF(
    @member_vote_unique_exists = 0,
    'ALTER TABLE `member_vote_option` ADD CONSTRAINT `uk_member_vote_option_member_vote` UNIQUE (`member_id`, `vote_id`)',
    'SELECT 1'
);

PREPARE add_member_vote_unique_statement FROM @add_member_vote_unique;
EXECUTE add_member_vote_unique_statement;
DEALLOCATE PREPARE add_member_vote_unique_statement;
