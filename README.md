# durion-hr
People manager mode for durion

## Purpose

Provide the Moqui-side screens and services for managing employees and roles, aligned to the People domain business rules for user/employee lifecycle, scoped role assignments, effective-dated location assignments, and timekeeping approvals.

## Scope

In scope:
- Employee profile management (identity, contactability, status)
- Role and role assignment administration with scope constraints (global vs location)
- Location assignments with effective dating and primary flags
- Offboarding workflows that revoke access while preserving historical auditability
- Timekeeping surfaces (breaks/time entries) and manager approval flows where implemented

Out of scope:
- Authentication and authorization enforcement (owned by Security service/components)
- Scheduling/dispatch decisions (consumers of people data)
- Work execution facts and operational state machines (owned by Work Execution/Shop Management)
