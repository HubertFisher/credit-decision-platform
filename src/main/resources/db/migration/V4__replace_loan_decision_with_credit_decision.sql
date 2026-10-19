DROP TABLE IF EXISTS loan_decision;

CREATE TABLE credit_decision (
                                 id                           UUID            NOT NULL,
                                 request_id                   UUID            NOT NULL,
                                 status                       VARCHAR(32)     NOT NULL,
                                 applicant_age_years          INTEGER         NOT NULL,
                                 monthly_income               NUMERIC(19, 2) NOT NULL,
                                 existing_monthly_payments    NUMERIC(19, 2) NOT NULL,
                                 credit_score                 INTEGER         NOT NULL,
                                 has_active_delinquency       BOOLEAN         NOT NULL,
                                 max_delinquency_days_12m     INTEGER         NOT NULL,
                                 requested_amount             NUMERIC(19, 2) NOT NULL,
                                 requested_term_months        INTEGER         NOT NULL,
                                 risk_grade                   VARCHAR(16)     NOT NULL,
                                 assigned_annual_rate         NUMERIC(7, 4)  NOT NULL,
                                 risk_exposure_cap            NUMERIC(19, 2) NOT NULL,
                                 current_dti_ratio            NUMERIC(7, 4)  NOT NULL,
                                 available_monthly_budget     NUMERIC(19, 2) NOT NULL,
                                 reason_codes                 JSONB          NOT NULL,
                                 offers                       JSONB          NOT NULL,
                                 created_at                   TIMESTAMPTZ    NOT NULL,
                                 CONSTRAINT pk_credit_decision PRIMARY KEY (id),
                                 CONSTRAINT uk_credit_decision_request_id UNIQUE (request_id)
);

CREATE INDEX idx_credit_decision_created_at_id
    ON credit_decision (created_at DESC, id DESC);

