# The WhyScan harness

Everything in this directory exists to make an agent's work on WhyScan **repeatable and checkable**
rather than improvised. It is version-controlled on purpose: a harness that lives in someone's
temporary folder is rebuilt from scratch every session and drifts from what CI actually enforces —
that was debt D23, and `tools/checks.py` is the fix that taught the lesson.

The normative rules are in [`AGENTS.md`](../AGENTS.md). This directory is how those rules become
*executable*.

```
.claude/
├── settings.json          Shared team settings: permissions, hooks. Checked in.
├── settings.local.json    Personal overrides. Git-ignored, never committed.
├── hooks/
│   └── preflight.sh       Runs tools/checks.py after any source edit
├── commands/              Slash commands — deterministic entry points to recurring work
├── agents/                Subagents — focused reviewers with their own context window
└── skills/                Skills — procedural knowledge loaded on demand
```

## Settings

`settings.json` is the shared configuration. Two things matter in it:

- **Permissions.** Read-only inspection and the offline checks are pre-approved so an agent does not
  stop to ask about `git status` forty times a session. The `deny` list encodes the *hard stops*
  from `AGENTS.md` that a permission rule can actually enforce: force-pushes, and reading signing
  material or `local.properties`. A rule cannot enforce "don't work on iOS" — that one is on you.
- **Hooks.** `PostToolUse` runs [`hooks/preflight.sh`](hooks/preflight.sh) after every `Edit`,
  `Write` or `NotebookEdit`. If the edited file is Kotlin, Gradle Kotlin DSL or XML, it runs
  `python3 tools/checks.py` and feeds any finding straight back to the agent. This is the single
  highest-value piece of the harness: in an environment where **nothing compiles**, this is the only
  feedback loop faster than a full CI round trip — five to fifteen minutes each.

Put personal preferences in `settings.local.json`. It is git-ignored.

## Slash commands

| Command | What it does |
|---|---|
| `/preflight` | Runs the offline checks and interprets the findings against project rules |
| `/pr-ready` | Walks the `AGENTS.md` definition of done over the current diff before you push |
| `/adr-new` | Drafts an ADR from the template, with the number and index entry already correct |
| `/spec-propose` | Opens an OpenSpec change: proposal, tasks and spec delta |
| `/spec-apply` | Folds an implemented change into `openspec/specs/` and archives it |
| `/docs-sync` | Finds divergence between code and `docs/` after a behaviour change |
| `/engine-add` | The nine-step checklist for adding a scanning engine, end to end |

## Subagents

Subagents get their own context window, which is the point: a reviewer that has not read the diff
being defended gives a more honest answer.

| Subagent | Use it for |
|---|---|
| `kotlin-reviewer` | Reviewing a Kotlin/Compose diff against this project's conventions and past defects |
| `docs-auditor` | Finding divergence between code and `docs/` — catalog, SDD, ROADMAP, ADR references |
| `spec-reviewer` | Checking an OpenSpec change is well-formed and actually scoped before implementation |

## Skills

| Skill | Loaded when |
|---|---|
| `whyscan-verification` | You need to know what can be proven here and how to say what you did not run |
| `whyscan-engine-authoring` | You are adding or changing a scanning engine |
| `whyscan-adr` | You are writing or superseding an architecture decision record |

## Extending this

Add a command when the same instructions get typed twice. Add a subagent when a task needs to *not*
inherit the current context. Add a skill when the knowledge is procedural and reusable, not a single
action. If a new rule can be checked mechanically, it belongs in `tools/checks.py` instead of any of
the three — a rule enforced by a script does not depend on anyone remembering it.
