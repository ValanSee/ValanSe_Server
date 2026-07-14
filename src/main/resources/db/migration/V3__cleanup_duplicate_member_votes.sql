-- Keep exactly one member vote per vote using the business priority A > B > C > D.
-- The lowest id breaks a tie when duplicate rows have the same option label.

DROP TEMPORARY TABLE IF EXISTS flyway_v3_member_votes_to_delete;

CREATE TEMPORARY TABLE flyway_v3_member_votes_to_delete (
    id BIGINT NOT NULL PRIMARY KEY,
    vote_id BIGINT NOT NULL
);

INSERT INTO flyway_v3_member_votes_to_delete (id, vote_id)
SELECT ranked.id, ranked.vote_id
FROM (
    SELECT
        mvo.id,
        mvo.vote_id,
        ROW_NUMBER() OVER (
            PARTITION BY mvo.member_id, mvo.vote_id
            ORDER BY
                CASE vo.label
                    WHEN 'A' THEN 1
                    WHEN 'B' THEN 2
                    WHEN 'C' THEN 3
                    WHEN 'D' THEN 4
                    ELSE 5
                END,
                mvo.id
        ) AS row_rank
    FROM member_vote_option mvo
    LEFT JOIN vote_option vo ON vo.id = mvo.vote_option_id
    WHERE mvo.member_id IS NOT NULL
      AND mvo.vote_id IS NOT NULL
) ranked
WHERE ranked.row_rank > 1;

START TRANSACTION;

DELETE mvo
FROM member_vote_option mvo
JOIN flyway_v3_member_votes_to_delete duplicate_vote
  ON duplicate_vote.id = mvo.id;

UPDATE vote_option vo
JOIN (
    SELECT DISTINCT vote_id
    FROM flyway_v3_member_votes_to_delete
) affected_vote ON affected_vote.vote_id = vo.vote_id
LEFT JOIN (
    SELECT vote_option_id, COUNT(*) AS actual_vote_count
    FROM member_vote_option
    WHERE vote_option_id IS NOT NULL
    GROUP BY vote_option_id
) actual_count ON actual_count.vote_option_id = vo.id
SET vo.vote_count = COALESCE(actual_count.actual_vote_count, 0);

UPDATE vote v
JOIN (
    SELECT DISTINCT vote_id
    FROM flyway_v3_member_votes_to_delete
) affected_vote ON affected_vote.vote_id = v.id
LEFT JOIN (
    SELECT vote_id, COUNT(*) AS actual_total_vote_count
    FROM member_vote_option
    WHERE vote_id IS NOT NULL
    GROUP BY vote_id
) actual_count ON actual_count.vote_id = v.id
SET v.total_vote_count = COALESCE(actual_count.actual_total_vote_count, 0);

COMMIT;

DROP TEMPORARY TABLE flyway_v3_member_votes_to_delete;
