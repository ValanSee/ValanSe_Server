-- Keep at most one active pin. Prefer an existing TRENDING pin; otherwise
-- promote the most recently created HOT pin before removing the legacy value.
SET @trending_pin_to_keep = (
    SELECT COALESCE(
        (
            SELECT MAX(`id`)
            FROM `vote`
            WHERE `pin_type` = 'TRENDING'
              AND `deleted_at` IS NULL
        ),
        (
            SELECT MAX(`id`)
            FROM `vote`
            WHERE `pin_type` = 'HOT'
              AND `deleted_at` IS NULL
        )
    )
);

UPDATE `vote`
SET `pin_type` = CASE
    WHEN `id` = @trending_pin_to_keep THEN 'TRENDING'
    ELSE 'NONE'
END
WHERE `pin_type` IN ('HOT', 'TRENDING');

ALTER TABLE `vote`
    MODIFY COLUMN `pin_type` ENUM('TRENDING', 'NONE') NOT NULL;
