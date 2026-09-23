CREATE TABLE loan_decision (
    id UUID PRIMARY KEY,
    income NUMERIC(19, 2) NOT NULL,
    requested_amount NUMERIC(19, 2) NOT NULL,
    term_months INTEGER NOT NULL,
    risk_score INTEGER NOT NULL,
    approved BOOLEAN NOT NULL
);