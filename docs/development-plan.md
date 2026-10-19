# Credit Decision Engine — Development Plan

## 1. Objective & Engineering Approach

This plan outlines the step-by-step implementation of the commercial 5-stage **Credit Decision Pipeline** specified in the [Technical Design](design.md).

Each stage delivers an isolated, unit-tested functional layer:
`Policy & Domain Models` $\to$ `Hard-Stop Rules & Risk-Based Pricing` $\to$ `Dual-Constraint Affordability` $\to$ `Annuity Loan Calculator` $\to$ `Smart Counter-Offer Matrix & Pipeline Orchestrator` $\to$ `PostgreSQL Persistence (Flyway V4)` $\to$ `REST API, Idempotency & Docker Containerization`.

---

## 2. Implementation Roadmap & Status

| Stage | Milestone | Key Deliverables | Status |
|---|---|---|---|
| **0** | **Architecture & Pipeline Design** | 5-stage pipeline specification, financial formulas, API contracts, and DB schema | Completed |
| **1** | **Policy, Hard-Stop Rules & Risk-Based Pricing** | `CreditPolicy`, `RiskGrade`, `HardStopEvaluator`, `RiskGradeResolver` + unit tests | Completed |
| **2** | **Dual-Constraint Affordability** | `AffordabilityCalculator` (50% DTI ceiling + 85,000 KZT subsistence minimum protection) + unit tests | Completed |
| **3** | **Annuity Loan Calculator** | `AnnuityLoanCalculator` (exact schedule summary & reverse principal calculation with 10,000 KZT step) + unit tests | Completed |
| **4** | **Offer Matrix Generator & Decision Pipeline** | `DecisionPipeline` (`APPROVED`, `COUNTER_OFFER` by amount and term, `REJECTED`) + unit tests | Completed |
| **5** | **Persistence & Idempotency (PostgreSQL + Flyway V4)** | `V4` migration, immutable `CreditDecision` JPA entity (`JSONB`), repository, and `CreditDecisionService` | Completed |
| **6** | **REST API, Validation & Containerization** | `CreditDecisionController`, DTOs, `requests.http`, multi-stage `Dockerfile`, and `docker-compose.yml` | Completed |
