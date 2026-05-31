-- V1__init.sql
-- Initial schema for AI PR Review Assistant
-- All timestamps use TIMESTAMPTZ for timezone-aware storage
-- UUIDs for public-facing IDs (gen_random_uuid() requires pgcrypto or pg 13+)

-- ============================================================
-- reviews — PR review task aggregate root
-- ============================================================
CREATE TABLE reviews (
    id                   UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    repository           TEXT         NOT NULL,
    pr_number            INTEGER      NOT NULL,
    pr_head_sha          TEXT,
    pr_title             TEXT,
    pr_author            TEXT,
    status               TEXT         NOT NULL DEFAULT 'PENDING',
    analysis_profile     TEXT         NOT NULL DEFAULT 'STANDARD',
    requested_categories JSONB,
    idempotency_key      TEXT,
    progress             NUMERIC(4,3) NOT NULL DEFAULT 0,
    files_total          INTEGER      NOT NULL DEFAULT 0,
    files_analyzed       INTEGER      NOT NULL DEFAULT 0,
    failure_reason       TEXT,
    callback_url         TEXT,
    submitted_by         TEXT,
    deleted              BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    started_at           TIMESTAMPTZ,
    completed_at         TIMESTAMPTZ
);

-- Indexes for reviews
CREATE INDEX idx_reviews_repo_pr       ON reviews (repository, pr_number, created_at DESC);
CREATE INDEX idx_reviews_active_status ON reviews (status) WHERE status IN ('PENDING', 'RUNNING');
CREATE INDEX idx_reviews_repo_sha      ON reviews (repository, pr_head_sha) WHERE deleted = FALSE;
CREATE UNIQUE INDEX uq_reviews_idem_key ON reviews (idempotency_key)
    WHERE idempotency_key IS NOT NULL AND deleted = FALSE;

-- ============================================================
-- change_summaries — PR change summary (1:1 with reviews)
-- ============================================================
CREATE TABLE change_summaries (
    id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    review_id        UUID        NOT NULL UNIQUE REFERENCES reviews(id) ON DELETE CASCADE,
    headline         TEXT        NOT NULL,
    inferred_purpose TEXT,
    primary_type     TEXT,
    affected_modules JSONB,
    risk_highlights  JSONB,
    payload          JSONB,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_change_summaries_review ON change_summaries (review_id);

-- ============================================================
-- risk_items — identified risk findings
-- ============================================================
CREATE TABLE risk_items (
    id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    review_id        UUID        NOT NULL REFERENCES reviews(id) ON DELETE CASCADE,
    file_path        TEXT        NOT NULL,
    start_line       INTEGER,
    end_line         INTEGER,
    category         TEXT        NOT NULL,
    severity         TEXT        NOT NULL,
    confidence       TEXT        NOT NULL,
    confidence_score NUMERIC(4,3),
    detection_source TEXT        NOT NULL,
    rule_id          TEXT,
    pattern_key      TEXT,
    description      TEXT        NOT NULL,
    rationale        TEXT,
    code_snippet     TEXT,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_risk_items_review     ON risk_items (review_id);
CREATE INDEX idx_risk_items_review_sev ON risk_items (review_id, severity, confidence);
CREATE INDEX idx_risk_items_pattern    ON risk_items (pattern_key) WHERE pattern_key IS NOT NULL;

-- ============================================================
-- review_suggestions — actionable fix suggestions (1:1 with risk_items)
-- ============================================================
CREATE TABLE review_suggestions (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    risk_item_id    UUID        NOT NULL UNIQUE REFERENCES risk_items(id) ON DELETE CASCADE,
    explanation     TEXT        NOT NULL,
    recommendation  TEXT        NOT NULL,
    suggested_patch TEXT,
    "references"    JSONB,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_suggestions_risk_item ON review_suggestions (risk_item_id);

-- ============================================================
-- review_feedbacks — user feedback for calibration loop
-- ============================================================
CREATE TABLE review_feedbacks (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    risk_item_id  UUID        NOT NULL REFERENCES risk_items(id) ON DELETE CASCADE,
    review_id     UUID        NOT NULL REFERENCES reviews(id) ON DELETE CASCADE,
    pattern_key   TEXT,
    verdict       TEXT        NOT NULL,
    comment       TEXT,
    submitted_by  TEXT,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_feedbacks_risk_item ON review_feedbacks (risk_item_id);
CREATE INDEX idx_feedbacks_pattern   ON review_feedbacks (pattern_key, verdict);

-- ============================================================
-- feedback_calibrations — materialized calibration factors
-- ============================================================
CREATE TABLE feedback_calibrations (
    pattern_key        TEXT        PRIMARY KEY,
    accepted_count     INTEGER     NOT NULL DEFAULT 0,
    false_pos_count    INTEGER     NOT NULL DEFAULT 0,
    ignored_count      INTEGER     NOT NULL DEFAULT 0,
    calibration_factor NUMERIC(4,3) NOT NULL DEFAULT 1.000,
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================================
-- analyzed_files — per-file analysis tracking
-- ============================================================
CREATE TABLE analyzed_files (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    review_id       UUID        NOT NULL REFERENCES reviews(id) ON DELETE CASCADE,
    file_path       TEXT        NOT NULL,
    change_type     TEXT        NOT NULL,
    additions       INTEGER     NOT NULL DEFAULT 0,
    deletions       INTEGER     NOT NULL DEFAULT 0,
    analysis_status TEXT        NOT NULL,
    model_tier_used TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_analyzed_files_review ON analyzed_files (review_id);

-- ============================================================
-- model_invocations — LLM call metering (cost/latency observability)
-- ============================================================
CREATE TABLE model_invocations (
    id                  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    review_id           UUID        REFERENCES reviews(id) ON DELETE CASCADE,
    task_type           TEXT        NOT NULL,
    model_tier          TEXT        NOT NULL,
    provider            TEXT        NOT NULL,
    model_name          TEXT        NOT NULL,
    prompt_tokens       INTEGER     NOT NULL DEFAULT 0,
    completion_tokens   INTEGER     NOT NULL DEFAULT 0,
    total_tokens        INTEGER     NOT NULL DEFAULT 0,
    latency_ms          INTEGER,
    estimated_cost_usd  NUMERIC(10,5),
    outcome             TEXT        NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_model_inv_review ON model_invocations (review_id);
CREATE INDEX idx_model_inv_cost   ON model_invocations (created_at, provider, model_tier);
