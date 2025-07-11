SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE `comment`;
TRUNCATE TABLE `comment_like`;
TRUNCATE TABLE `comment_group`;
TRUNCATE TABLE `member_vote_option`;
TRUNCATE TABLE `member_profile`;
TRUNCATE TABLE `vote_option`;
TRUNCATE TABLE `vote`;
TRUNCATE TABLE `member`;

SET FOREIGN_KEY_CHECKS = 1;