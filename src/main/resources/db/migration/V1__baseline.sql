-- Production schema baseline captured from the valanse database on 2026-07-14.
-- Existing databases must be explicitly baselined at version 1 before Flyway is enabled.
-- This migration is only for creating a new, empty schema.
/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `activity_event` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `anonymous_id` varchar(100) DEFAULT NULL,
  `event_type` enum('PAGE_VIEW','VOTE_CREATE','VOTE_PARTICIPATE','COMMENT_CREATE','COMMENT_LIKE') NOT NULL,
  `page_path` varchar(500) DEFAULT NULL,
  `member_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKjfxbil4dpakyvrdchkbtk3e03` (`member_id`),
  CONSTRAINT `FKjfxbil4dpakyvrdchkbtk3e03` FOREIGN KEY (`member_id`) REFERENCES `member` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `anonymous_user_link` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `anonymous_id` varchar(100) NOT NULL,
  `member_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_lyxw16oimv35lsbp07ben341e` (`anonymous_id`),
  KEY `FKksuv31n1obk4ctg9es8m0lw4o` (`member_id`),
  CONSTRAINT `FKksuv31n1obk4ctg9es8m0lw4o` FOREIGN KEY (`member_id`) REFERENCES `member` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `comment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci,
  `is_deleted` bit(1) DEFAULT NULL,
  `like_count` int DEFAULT NULL,
  `reply_count` int DEFAULT NULL,
  `comment_group_id` bigint DEFAULT NULL,
  `member_id` bigint DEFAULT NULL,
  `parent_id` bigint DEFAULT NULL,
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `vote_option_id` bigint DEFAULT NULL,
  `purged_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKmfc1nmj8gvio88e1ljgpe3vu8` (`comment_group_id`),
  KEY `FKmrrrpi513ssu63i2783jyiv9m` (`member_id`),
  KEY `FKde3rfu96lep00br5ov0mdieyt` (`parent_id`),
  KEY `FKj6pxfe29rme61uod4hku772np` (`vote_option_id`),
  KEY `idx_comment_purge` (`deleted_at`,`purged_at`),
  CONSTRAINT `FKde3rfu96lep00br5ov0mdieyt` FOREIGN KEY (`parent_id`) REFERENCES `comment` (`id`),
  CONSTRAINT `FKj6pxfe29rme61uod4hku772np` FOREIGN KEY (`vote_option_id`) REFERENCES `vote_option` (`id`),
  CONSTRAINT `FKmfc1nmj8gvio88e1ljgpe3vu8` FOREIGN KEY (`comment_group_id`) REFERENCES `comment_group` (`id`),
  CONSTRAINT `FKmrrrpi513ssu63i2783jyiv9m` FOREIGN KEY (`member_id`) REFERENCES `member` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `comment_group` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `total_comment_count` int DEFAULT NULL,
  `vote_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_mc6115o3n9k2rmg10gsvj6m22` (`vote_id`),
  CONSTRAINT `FKhp0w8tydq9f4mktrsfi1vjn3g` FOREIGN KEY (`vote_id`) REFERENCES `vote` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `comment_like` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `comment_id` bigint DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_comment_like_user_comment` (`user_id`,`comment_id`),
  KEY `FKqlv8phl1ibeh0efv4dbn3720p` (`comment_id`),
  KEY `FKsp4x20abr7s0n1kj6gjhlp2l7` (`user_id`),
  CONSTRAINT `FKqlv8phl1ibeh0efv4dbn3720p` FOREIGN KEY (`comment_id`) REFERENCES `comment` (`id`),
  CONSTRAINT `FKsp4x20abr7s0n1kj6gjhlp2l7` FOREIGN KEY (`user_id`) REFERENCES `member` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `member` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `kakao_access_token` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `kakao_refresh_token` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `profile_image_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `role` enum('ADMIN','USER') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `social_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `social_type` enum('KAKAO') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `nickname` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_member_deleted_at` (`deleted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `member_profile` (
  `member_id` bigint NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `age` enum('TEN','TWENTY','THIRTY','OVER_FORTY') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `gender` enum('MALE','FEMALE') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `mbti` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `mbti_ie` enum('I','E') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `mbti_tf` enum('T','F') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `nickname` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `point` bigint NOT NULL,
  PRIMARY KEY (`member_id`),
  CONSTRAINT `FKkrtcs7wdtv954e99w88uga9mq` FOREIGN KEY (`member_id`) REFERENCES `member` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `member_profile_title` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `acquired_at` datetime(6) DEFAULT NULL,
  `equipped` bit(1) NOT NULL,
  `member_profile_id` bigint NOT NULL,
  `title_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_member_profile_title_profile_title` (`member_profile_id`,`title_id`),
  KEY `FKbyrsmq8f0n18qxhv3bemqv0t5` (`title_id`),
  CONSTRAINT `FKbyrsmq8f0n18qxhv3bemqv0t5` FOREIGN KEY (`title_id`) REFERENCES `title` (`id`),
  CONSTRAINT `FKomcxm249uq805vvlwjyisvftc` FOREIGN KEY (`member_profile_id`) REFERENCES `member_profile` (`member_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `member_vote_option` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `member_id` bigint DEFAULT NULL,
  `vote_id` bigint DEFAULT NULL,
  `vote_option_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKbbcsjbdl3n5xa5v5gw2yuksct` (`member_id`),
  KEY `FK7tmc3qvvegfw6xyhj1rfqjj0f` (`vote_id`),
  KEY `FKn6hdn1rtvlw1y5himvhyw3gav` (`vote_option_id`),
  CONSTRAINT `FK7tmc3qvvegfw6xyhj1rfqjj0f` FOREIGN KEY (`vote_id`) REFERENCES `vote` (`id`),
  CONSTRAINT `FKbbcsjbdl3n5xa5v5gw2yuksct` FOREIGN KEY (`member_id`) REFERENCES `member` (`id`),
  CONSTRAINT `FKn6hdn1rtvlw1y5himvhyw3gav` FOREIGN KEY (`vote_option_id`) REFERENCES `vote_option` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `point_history` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `amount` bigint DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `type` varchar(30) NOT NULL,
  `member_id` bigint DEFAULT NULL,
  `remaining_point` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKn7yuk3drkv7qwgq0op21olmwy` (`member_id`),
  CONSTRAINT `FKn7yuk3drkv7qwgq0op21olmwy` FOREIGN KEY (`member_id`) REFERENCES `member` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `report` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `report_type` enum('VOTE','COMMENT') DEFAULT NULL,
  `target_id` bigint DEFAULT NULL,
  `member_id` bigint DEFAULT NULL,
  `content` text,
  `reason` enum('SPAM','SEXUAL_CONTENT','HATE_OR_HARASSMENT','VIOLENCE_OR_THREAT','ILLEGAL_OR_HARMFUL','PERSONAL_INFORMATION','ETC') DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_report_member_type_target` (`member_id`,`report_type`,`target_id`),
  KEY `FKel7y5wyx42a6njav1dbe2torl` (`member_id`),
  CONSTRAINT `FKel7y5wyx42a6njav1dbe2torl` FOREIGN KEY (`member_id`) REFERENCES `member` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `title` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `acquisition_type` enum('DEFAULT','POINT_PURCHASE','ACHIEVEMENT','SEASON','EVENT') NOT NULL,
  `active` bit(1) NOT NULL,
  `code` varchar(255) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `display_order` int NOT NULL,
  `name` varchar(255) NOT NULL,
  `price` bigint NOT NULL,
  `requirement_text` varchar(255) DEFAULT NULL,
  `tier` enum('BASIC','TIER_1','TIER_2','TIER_3','SEASON','RARE') NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_loi574bd9qm7c5kwhmfwyapab` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `vote` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `category` enum('ALL','FOOD','LOVE','BUY','SPORT','WORRY','ETC') DEFAULT NULL,
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `total_vote_count` int DEFAULT NULL,
  `member_id` bigint DEFAULT NULL,
  `is_deleted` bit(1) DEFAULT NULL,
  `reactivity_score` int DEFAULT NULL,
  `reactivity_updated_at` datetime(6) DEFAULT NULL,
  `content` text,
  `pin_type` enum('HOT','TRENDING','NONE') NOT NULL,
  `hot_issue_voted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `FKgkbgl6xp2rpgwghb7mtyuv48h` (`member_id`),
  KEY `idx_vote_deleted_at` (`deleted_at`),
  CONSTRAINT `FKgkbgl6xp2rpgwghb7mtyuv48h` FOREIGN KEY (`member_id`) REFERENCES `member` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `vote_option` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `content` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `label` enum('A','B','C','D') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `vote_count` int DEFAULT NULL,
  `vote_id` bigint DEFAULT NULL,
  `image_url` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKtbgkw2wpeeygnh7sjwuda3g4a` (`vote_id`),
  CONSTRAINT `FKtbgkw2wpeeygnh7sjwuda3g4a` FOREIGN KEY (`vote_id`) REFERENCES `vote` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
