-- Enforce "at most one TRENDING vote" at the DB level via a UNIQUE index on
-- pin_type. Unpinned votes are stored as NULL instead of 'NONE' so MySQL's
-- "UNIQUE allows multiple NULLs" behavior leaves them unconstrained.

-- A soft-deleted vote must not keep occupying the single TRENDING slot.
UPDATE `vote`
SET `pin_type` = 'NONE'
WHERE `pin_type` = 'TRENDING'
  AND `deleted_at` IS NOT NULL;

-- Defensive cleanup: if the pre-existing race already produced more than one
-- active TRENDING vote, keep only the most recently created one.
UPDATE `vote`
SET `pin_type` = 'NONE'
WHERE `pin_type` = 'TRENDING'
  AND `id` <> (
      SELECT `id` FROM (
          SELECT `id` FROM `vote`
          WHERE `pin_type` = 'TRENDING' AND `deleted_at` IS NULL
          ORDER BY `id` DESC
          LIMIT 1
      ) AS keep_pin
  );

ALTER TABLE `vote`
    MODIFY COLUMN `pin_type` ENUM('TRENDING', 'NONE') NULL;

UPDATE `vote`
SET `pin_type` = NULL
WHERE `pin_type` = 'NONE';

ALTER TABLE `vote`
    MODIFY COLUMN `pin_type` ENUM('TRENDING') NULL;

ALTER TABLE `vote`
    ADD UNIQUE INDEX `uq_vote_pin_type` (`pin_type`);
