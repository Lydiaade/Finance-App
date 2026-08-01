# Finance App — Project Context for AI Coding Assistants

This document gives Claude Code (or any AI coding assistant) the context needed to work on Finance App consistently with its architecture, conventions, and priorities. Read this before making changes.

## Project Overview

**What it is:** A personal finance manager built to replace an Excel-based tracker. It's a solo project by Lydia (project lead + full-stack engineer), built partly to gain full control over personal finance tracking and partly as a vehicle to practice languages/tech not used daily. There's an open door to add more advanced features and possibly monetize it eventually — so code should be written as if it might grow beyond "just for me," not as disposable scratch work.

**Stage:** Active development, currently for personal/local use only (no auth, no public deployment yet).

**Team:** Solo. No other contributors currently.

## Tech Stack & Architecture

### Backend — `fm-backend/`
- **Java 21**, **Spring Boot 3.0.2**, Maven
- **PostgreSQL**, run locally via `docker-compose.yml` (currently uses plaintext dev credentials — fine for local, must change before any real deployment)
- **Spring Data JPA / Hibernate**, `ddl-auto: update` (acceptable for dev; flag if a task moves toward production — recommend Flyway or Liquibase migrations at that point rather than continuing to rely on auto-DDL)
- **Layered architecture**, strictly: `controller → service → repository`, with DTOs separated into `dto`, `dto.request`, and `dto.response` packages
- REST API on port 8080. **No authentication/authorization yet** (see Auth & Security below)
- Existing domains: bank accounts, transactions, CSV upload/upload history, segments (budget categories)

### Frontend — `fm-ui/`
- **React 18**, Create React App (`react-scripts`) — **no TypeScript**
- `react-router-dom` v6, `react-bootstrap` + Bootstrap 5 for UI, `recharts` for charts
- Component style: the codebase currently mixes class components (e.g. `App.js`) and functional components. **Both are fine going forward** — don't force a rewrite of existing class components unless a task specifically calls for touching that file
- Backend URL is set in `src/config.js`, currently hardcoded to `http://localhost:8080` — flag this if a task involves deployment or environment configuration

### Domain model & CSV upload flow (backend)

- **Entities** (`dto/`): `BankAccount` is the root — sort code + account number, plus `isMainBankAccount`. `Transaction` belongs to a `BankAccount` and to a `FileUpload` (both `@ManyToOne`, `@JsonIgnore`'d) — every transaction traces back to the upload that created it. `segment` on `Transaction` is a denormalized string (default `"Undefined"`), not a FK to `Segment` — `Segment` is currently just a name lookup. `FileUpload` (table `file_uploads`) records one CSV import: filename, timestamp, success/failure counts, and cascades deletes to its transactions (`orphanRemoval = true`).
- **CSV upload flow**: `UploadController` → `UploadService.saveFile` → `CSVHelper.csvToTransactions`. The file is written to a temp file, parsed line-by-line (header skipped), then the temp file is deleted. Each row is validated against the selected `BankAccount`'s sort code/account number — a mismatch throws `IllegalArgumentException` → `422`. Malformed rows (wrong column count) are caught individually and counted as failures rather than aborting the whole upload, so partial success is normal and reported via `successfulTransactions`/`failedTransactions` on the response. **The parser is position-based** (`split(",")` then fixed column indices) — there's no header-name validation, so reordering CSV columns silently breaks parsing. Keep this in mind before changing anything in `CSVHelper` or the expected upload format.

### Repo structure & workflow
- Monorepo: `fm-backend/` and `fm-ui/` as top-level folders
- Planning happens on a Notion kanban board
- Commit messages and PR titles follow the ticket format: `[FM-##] Description of change` — match the ticket number when one is known; don't invent one if it isn't provided

## Local Dev & Commands

**Backend (`fm-backend/`):**
1. `docker-compose up -d` — starts Postgres (mapped to host port `5332`, db `transaction`, user/pass `lydia`/`password` — dev-only plaintext creds, see Auth & Security)
2. Run `FinanceManagerApplication`, or `./mvnw spring-boot:run` — serves on port `8080`
3. Port conflict: `lsof -i :8080` to find and kill whatever's already bound

Use the `./mvnw` wrapper rather than a global `mvn` install, so the Maven version matches what the project expects.

- Run all tests: `./mvnw test`
- Run a single test class: `./mvnw test -Dtest=TransactionServiceTest`
- Run a single test method: `./mvnw test -Dtest=TransactionServiceTest#getTransactions`
- Build: `./mvnw clean package`

**Frontend (`fm-ui/`):**
- `npm install`, then `npm start` — serves on port `3000`
- Tests: `npm test`
- Build: `npm run build`

## Conventions

- **Backend:** business logic belongs in the service layer, never in controllers. Controllers should stay thin — validate input, delegate to a service, return a response. New endpoints follow the existing shape: DTO(s) → service method → repository method (if new queries are needed) → controller wiring, matching how existing controllers/services/repositories are structured.
- **Frontend:** use `react-bootstrap` components instead of raw HTML/CSS where an equivalent component exists.
- **Readability over cleverness.** Since part of the point of this project is learning, prefer idiomatic, explicit code over dense one-liners or overly abstracted patterns.
- Match existing package/folder conventions when adding new domain objects — don't introduce a new pattern for a single feature.

## Testing Expectations

**Write tests alongside every change — this applies to all new work, not just complex logic.**
- Backend: JUnit 5 (`junit-jupiter`); H2 is available as the test-scope database.
- Frontend: `@testing-library/react` + Jest via `react-scripts test`.
- A feature isn't done until it has reasonable test coverage — don't treat tests as optional cleanup for later.
- **Ownership:** the implementing agent (Senior Developer 1/2, Senior UI Developer 1/2) writes and runs tests for their own code as part of implementation. The QA Analyst then independently re-runs the full suite and checks coverage against acceptance criteria — QA does not just trust that tests exist, and adds tests itself for any real gap it finds. Testing is a gate, not a courtesy step — see the Delivery Process below for exactly where this happens.

## Auth & Security (upcoming priority)

There's no authentication in place yet, and this is planned as a near-term addition. Since this handles financial data, treat security as a first-class concern once this work starts, not an afterthought.

No approach has been locked in yet. Recommended default: **Spring Security + JWT**, self-managed — it fits the existing Spring Boot layered architecture well, keeps full control over the auth logic (useful for a project that's also about learning), and avoids a third-party dependency for a currently single-user, self-hosted app. If ease of setup or eventual multi-user/social login becomes a priority, a third-party provider (Auth0, Clerk, Firebase Auth) or OAuth2 is a reasonable alternative worth surfacing — present the tradeoff rather than assuming.

Whatever approach is chosen:
- No hardcoded secrets — use environment variables or Spring profiles
- Plan to move the Postgres credentials out of `docker-compose.yml` and into environment variables once auth work begins
- Don't add auth headers/logic speculatively to unrelated work before this is actually built

## Known Gotchas / Guardrails

- `ddl-auto: update` is fine for now, but don't let it quietly persist if a task pushes this toward production — call it out and recommend proper migrations.
- The hardcoded backend URL in `fm-ui/src/config.js` should move to environment-based config when deployment work starts — flag it rather than silently working around it.
- No auth exists yet — don't assume authenticated requests, tokens, or user context exist in either the frontend or backend unless a task is specifically building that.
- `fm-backend/pom.xml` declares `<java.version>21</java.version>` but the `maven-compiler-plugin` overrides `<source>`/`<target>` to `16` — the project actually compiles to Java 16 bytecode regardless of the runtime. Don't assume Java 17-21-only language features will compile; flag this mismatch rather than "fixing" one side of it unprompted.
- *(This section is intentionally light — update it as specific "Claude got this wrong" moments come up, so guardrails stay grounded in real mistakes rather than speculative ones.)*

## Current Focus

Not specified yet. When priority is unclear, infer from the most recent commits/branch or the active Notion ticket, and ask rather than guess if it's genuinely ambiguous.

## Delivery Process: Multi-Agent Workflow

Tickets are worked by a team of six specialized agents rather than one generalist pass. The goal is higher-quality output through genuine peer critique, and fewer interruptions to the project lead — check-ins happen at defined checkpoints, not continuously. **The project lead does not want to be asked questions mid-ticket; the team is expected to work autonomously between the Amigos round and PR-ready.**

To run this process, use `/work-ticket [FM-## and ticket details]` (see `.claude/commands/work-ticket.md`). It walks the whole sequence below in order so the process doesn't depend on remembering to invoke each step manually.

### The Team

| Agent | Role | Default Lens |
|---|---|---|
| Senior Developer 1 | Backend implementation & review | Correctness / business-logic soundness |
| Senior Developer 2 | Backend implementation & review | Architecture / maintainability / edge cases |
| Senior UI Developer 1 | Frontend implementation & review | UX correctness — matches acceptance criteria / user flow |
| Senior UI Developer 2 | Frontend implementation & review | Component architecture / accessibility / UI edge cases |
| QA Analyst | Quality gate | Acceptance criteria verification, edge/negative-case coverage |
| Business Analyst | Requirements | Ticket → acceptance criteria, intent vs. delivery check |

Agent definitions live in `.claude/agents/`: `senior-developer-1.md`, `senior-developer-2.md`, `senior-ui-developer-1.md`, `senior-ui-developer-2.md`, `qa-analyst.md`, `business-analyst.md`.

Both Senior Developers are equally capable, full backend seniors, and both Senior UI Developers are equally capable, full frontend seniors — the lens splits above exist so each pair's cross-review is genuinely useful rather than two identical opinions agreeing with each other. Any agent can and should raise issues outside their default lens too. Adjust or drop the splits if they don't hold up in practice.

**Folder boundaries are a convention, not an enforced restriction.** Senior Developers 1/2 work in `fm-backend/`, Senior UI Developers 1/2 work in `fm-ui/`, by default — but none of them are tool-restricted to their folder. If a ticket genuinely needs someone to touch the other side (e.g. a Senior Developer adjusting `config.js` for a new endpoint), that's fine — just flag it as a deliberate exception rather than a habit.

### The Process

1. **Ticket intake** — Project lead provides a ticket (ideally with an `[FM-##]` number).
2. **Three Amigos session (once, upfront)** — Business Analyst leads. All agents contribute clarifying questions from their own lens — feasibility/data-model questions from the Senior Developers, UX/edge-case questions from the Senior UI Developers, testability/acceptance-criteria questions from QA. **These are batched into a single round of questions to the project lead.** After this, no further questions are expected until the PR is ready — see Escalation below for the one exception.
3. **Acceptance criteria & branch creation** — Business Analyst turns the clarified ticket into explicit, testable acceptance criteria the rest of the team builds and tests against. A feature branch is created off `main`: `feature/FM-##-short-description`.
4. **Implementation** — Senior Developers split or pair on backend work (whichever fits the ticket); Senior UI Developers split or pair on frontend work the same way, building against the same acceptance criteria and the backend API contract. Tests are written **alongside** implementation, not after — see Testing Expectations above. Each implementing agent runs their own tests locally before handing anything off for review, and commits their own work onto the feature branch using the `[FM-##] Description` format; "I wrote tests" isn't done until "I ran them and they pass."
5. **Critique loop** — nobody's work is accepted at face value:
   - Senior Developer 1 and 2 review each other's backend work with real scrutiny — does this match the acceptance criteria, is this the right approach, what breaks it — not a rubber stamp.
   - Senior UI Developer 1 and 2 review each other's frontend work the same way — one checking UX/acceptance-criteria fit, the other checking architecture, accessibility, and edge cases (loading/error/empty states).
   - A Senior Developer and a Senior UI Developer cross-check that the API contract actually matches what the frontend needs (either UI dev — whichever is doing the integration work at the time).
   - Reviewers check the tests themselves, not just the implementation — do the tests actually assert the right thing, or do they just exercise the code without checking behavior?
   - Disagreements are resolved with evidence (re-reading the ticket, running the code, checking acceptance criteria) — not by seniority or deference. A disagreement the team can't resolve is an Escalation, not a coin flip.
6. **Testing gate (QA Analyst)** — this is a distinct, non-skippable step, not a footnote inside sign-off:
   - QA independently runs the full test suite (backend: `mvn test`; frontend: `npm test`) — never takes "it passed for me" from an implementing agent at face value.
   - QA maps existing tests against the acceptance criteria and flags anything untested — happy path, error/negative paths, edge cases (empty states, invalid input, boundary values).
   - QA writes or requests additional tests for any real gap found — a feature is not passed through with "acceptable" coverage if a meaningful case is untested.
   - QA sends work back to the relevant agent (with the specific gap named) rather than pass something through because "the devs said it's done."
   - The gate only opens when: acceptance criteria are met, the full suite is green, edge/negative cases are covered, and there are no known regressions.
7. **Business Analyst sign-off & PR opened** — Before anything goes to the project lead, the Business Analyst confirms the result solves the ticket's actual intent, not just that code exists and tests pass — then opens the pull request (`gh pr create`, title `[FM-##] Description`) against `main`.
8. **PR ready → contact project lead.** This is the first point of contact after step 2 (barring an Escalation). The PR description covers: what was built, the key decisions and reasoning behind them (the project lead will want to confirm these), **what was tested and how** (not just "tests added"), and anything explicitly deferred or flagged.
9. **Review loop** — Project lead tests and gives feedback. Feedback routes to the specific agent(s) it concerns, who push additional commits to the same branch/PR — this doesn't restart the full Amigos session unless the feedback reveals a genuine misunderstanding of the requirement, and the PR isn't closed and reopened per round. Repeat until satisfied.
10. **Project lead merges → next ticket.** The team never merges its own PR — that's the project lead's call, and it's what makes the review loop a real gate. Fresh Amigos session begins for the next ticket.

### Git & PR Conventions

- **Branch:** one per ticket, created at step 3, off `main`: `feature/FM-##-short-description` (e.g. `feature/FM-23-segment-budgets`). **This is the go-forward convention** — existing branches predate it and don't follow this pattern (e.g. `FM-23_new_transaction`, no `feature/` prefix, underscore instead of hyphens); don't rename them retroactively, just use the new pattern from here on.
- **Commits:** each implementing agent commits their own work onto that branch as they go — `[FM-##] Description of change`, matching the existing convention.
- **PR:** opened once (step 7), only after the Testing gate and Business Analyst sign-off are both complete — never before the ticket is genuinely ready. Title: `[FM-##] Description`, matching commit format.
- **Merging is exclusively the project lead's action.** The agent team creates the branch, commits, and opens the PR — it never merges it, and never pushes directly to `main`.
- If review feedback requires changes, push additional commits to the same branch/PR rather than closing and reopening.

### Escalation (the exception to "don't keep asking")

The project lead should only hear from the team at the Amigos round and at PR-ready. The one exception is a genuine blocker — the ticket contradicts existing behavior, a dependency is missing, or the team hits a disagreement it can't resolve with evidence. When that happens, say so plainly and explain what's blocking, rather than silently guessing or silently stalling.

### Technical note: how this actually runs

Each subagent starts with its own fresh, isolated context — it doesn't automatically see the main conversation or other agents' output. So the main session acts as facilitator: it runs the Amigos session directly with the project lead, then explicitly carries the relevant context (the ticket, the acceptance criteria, another agent's actual diff or decisions) into each subagent's prompt when invoking it. The critique loop depends on this — when Senior Developer 2 reviews Senior Developer 1's work, the actual implementation needs to be in that prompt, not assumed as shared memory. Git state (branch, commits) is shared through the actual repo on disk, so agents don't need to be told about it conversationally — they can check it directly. Keep intermediate agent-to-agent exchanges out of the conversation with the project lead — only the Amigos questions and the final PR summary should surface there.

## How Claude Should Work Here

1. Match the existing architecture and conventions above rather than introducing new patterns for a single feature.
2. Always include tests with new work.
3. Reference the `[FM-##]` ticket format in commit messages/PR titles when a ticket number is known.
4. Flag (don't silently fix or silently ignore) anything that touches: DDL/migrations, the hardcoded backend URL, or security-sensitive areas — these are known transition points for this project.
5. Be conservative about introducing third-party services/libraries that would involve sharing financial data externally — this is personal financial data, so default to self-hosted/self-managed solutions unless asked otherwise.
6. When something in this document is out of date (stack, priorities, gotchas), say so rather than quietly working around the mismatch — this file should evolve with the project.
7. For any ticket-sized piece of work, follow the Delivery Process above rather than working solo end-to-end — delegate to the relevant agents in `.claude/agents/` at each step.

---
*Update this document as the project evolves — especially "Known Gotchas," "Current Focus," and the agent lens split, which are most likely to go stale.*
