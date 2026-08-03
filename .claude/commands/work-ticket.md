---
description: Run the full multi-agent ticket delivery process (Three Amigos → Implementation → Testing Gate → QA → PR) for a given Finance App ticket
argument-hint: [FM-## and ticket details, or pasted ticket content]
allowed-tools: Bash(git:*), Bash(gh:*), Bash(mvn:*), Bash(npm:*), Agent, Read, Grep, Glob
---

You are orchestrating Finance App's multi-agent delivery process for the ticket below, exactly as defined in `CLAUDE.md`'s "Delivery Process: Multi-Agent Workflow" section. Read `CLAUDE.md` now if it isn't already loaded this session.

**Ticket:** $ARGUMENTS

Follow these steps in order. Do not skip ahead, and do not contact the project lead outside of the two points marked below (or a genuine Escalation).

1. **Three Amigos.** Gather clarifying questions from all six agent perspectives — feasibility/data-model concerns (Senior Developers), UX/edge-case questions (Senior UI Developers), testability/acceptance-criteria questions (QA), and any ambiguity in the ticket itself (Business Analyst). Present them to the project lead as **one batched round of questions**, then wait for answers. This is the first point of contact.
2. **Acceptance criteria, branch, and size the ticket.** Have `business-analyst` turn the clarified ticket into explicit, testable acceptance criteria. Create the feature branch off `main`: `feature/FM-##-short-description` (ask for a ticket number first if one wasn't given). At this point, size the ticket per CLAUDE.md's "Scaling the process to ticket size" — small/contained work gets a lighter critique loop and QA pass (see steps 4-5); anything touching schema/migrations, auth, or a cross-cutting refactor gets the full heavy version regardless of how small it looks on paper.
3. **Implementation.** Delegate to `senior-developer-1`, `senior-developer-2`, `senior-ui-developer-1`, and `senior-ui-developer-2` per the Delivery Process. Each writes and runs their own tests before handoff, and commits their own work to the branch using the `[FM-##] Description` format. Carry the actual ticket and acceptance criteria into each subagent's prompt — they don't share your context automatically.
4. **Critique loop.** Have Senior Developer 1 and 2 review each other's backend work, and Senior UI Developer 1 and 2 review each other's frontend work. For a small/contained ticket, fold the API-contract cross-check into one of these reviews rather than spinning up a separate dedicated agent call; for a larger/riskier ticket, run it as its own pass. When delegating a review, include the actual diff/decisions being reviewed in the prompt. Tell reviewers to default to reading code and running the existing test suite — reserve hands-on infra reproduction (throwaway databases, separate worktrees, full environment rebuilds) for a claim that's genuinely high-risk or actively disputed, not routine due diligence; if two reviewers would independently reproduce the same thing, only one needs to. Resolve disagreements with evidence; if the team can't resolve one, that's an Escalation.
5. **Testing gate.** Delegate to `qa-analyst`. It must independently run the full suite (`mvn test`, `npm test`), map tests to acceptance criteria, and either fix or send back any real gap. For a small/contained ticket, QA can lean on the implementers'/reviewers' own coverage work rather than fully re-deriving it from scratch — it still must independently execute the suite itself, that part never gets skipped. The gate only opens when the suite is green, acceptance criteria are covered, and edge/negative cases are handled.
6. **Sign-off & PR.** Delegate to `business-analyst` to confirm the result matches the ticket's actual intent, then open the PR itself (`gh pr create`, title `[FM-##] Description`, against `main`) with a description covering what was built, key decisions and reasoning, what was tested and how, and anything deferred/flagged.
7. **Contact the project lead** that the PR is ready for review. This is the second and final point of contact, barring an Escalation.
8. **Review loop.** On feedback, route it to the specific agent(s) it concerns and push additional commits to the same branch/PR. Don't restart the Amigos round unless the feedback reveals a genuine misunderstanding of the requirement.

**Never merge the PR yourself** — that's the project lead's action only, after they approve in the review loop.

If at any point the team hits a genuine blocker (contradictory requirement, missing dependency, an unresolvable disagreement), say so plainly and explain what's blocking, rather than guessing or stalling silently — see the Escalation section of CLAUDE.md.
