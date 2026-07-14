-- Reconcile legacy soft-delete columns left behind by Hibernate ddl-auto=update.
-- The statements are conditional because the development schema no longer has these columns.

SET @comment_is_deleted_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'comment'
      AND column_name = 'is_deleted'
);

SET @drop_comment_is_deleted = IF(
    @comment_is_deleted_exists > 0,
    'ALTER TABLE `comment` DROP COLUMN `is_deleted`',
    'SELECT 1'
);

PREPARE drop_comment_is_deleted_statement FROM @drop_comment_is_deleted;
EXECUTE drop_comment_is_deleted_statement;
DEALLOCATE PREPARE drop_comment_is_deleted_statement;

SET @vote_is_deleted_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'vote'
      AND column_name = 'is_deleted'
);

SET @drop_vote_is_deleted = IF(
    @vote_is_deleted_exists > 0,
    'ALTER TABLE `vote` DROP COLUMN `is_deleted`',
    'SELECT 1'
);

PREPARE drop_vote_is_deleted_statement FROM @drop_vote_is_deleted;
EXECUTE drop_vote_is_deleted_statement;
DEALLOCATE PREPARE drop_vote_is_deleted_statement;
