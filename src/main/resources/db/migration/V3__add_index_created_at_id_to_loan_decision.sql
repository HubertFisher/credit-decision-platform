CREATE INDEX idx_loan_decision_created_at_id
    ON loan_decision (created_at DESC, id DESC );
