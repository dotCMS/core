# Sidecars and turn budgets

Read at Stage 4, before writing any prompt. Stage 4b and Stage 5 rely on what it sets up.

A reviewer that hits `maxTurns` mid-thought, or spends its whole budget investigating, returns no report. Each one therefore appends every finding to its own **sidecar** file the moment it confirms it, and the sidecars, not the final messages, are the system of record.

**The reviewer agents own the contract.** Each file in `.claude/agents/dotcms-pr-*.md` has a "Sidecar and budget" section with its row shape, the `echo`-only write rule, the verbatim-`evidence` rule, its turn budget and stop point, and the shape of its final message. A subagent sees only its own file and its prompt, so the contract lives there, and the agents stay launchable without this skill: launched with no sidecar path (another workflow's sub-agent, a direct launch), an agent skips the sidecar and answers in the shape its caller asks for. Changing the contract means changing those sections; this file only says where each sidecar goes.

## The sidecar line (end of every prompt)

Each prompt ends with this line, with the instance's own sidecar as a full absolute path (an agent given a bare file name writes it in the repo root):

```
Your sidecar: <SIDECAR>. Write it as your "Sidecar and budget" section says.
```

The sidecars live in `<SIDECAR_DIR>`, inside the fresh run directory SKILL.md creates for every run, so a `.jsonl` left by an earlier review never mixes into this one.

| Agent | `<SIDECAR>` |
|---|---|
| `dotcms-pr-code-reviewer` | `<SIDECAR_DIR>/code-reviewer-<i>.jsonl` |
| `dotcms-pr-test-reviewer` | `<SIDECAR_DIR>/test-reviewer-<i>.jsonl` |
| `dotcms-pr-historical-reviewer` | `<SIDECAR_DIR>/historical-reviewer.jsonl` |
| `dotcms-pr-silent-failure-hunter` | `<SIDECAR_DIR>/silent-failure-hunter.jsonl` |
| `dotcms-pr-comment-analyzer` | `<SIDECAR_DIR>/comment-analyzer.jsonl` |
| `dotcms-pr-logic-reviewer` | `<SIDECAR_DIR>/logic-reviewer-<i>.jsonl` |
| `dotcms-pr-java-code-reviewer` | `<SIDECAR_DIR>/java-code-reviewer-<i>.jsonl` |
| `dotcms-pr-java-api-reviewer` | `<SIDECAR_DIR>/java-api-reviewer-<i>.jsonl` |
| `dotcms-pr-java-persistence-reviewer` | `<SIDECAR_DIR>/java-persistence-reviewer-<i>.jsonl` |
| `dotcms-pr-java-security-reviewer` | `<SIDECAR_DIR>/java-security-reviewer.jsonl` |
| `dotcms-pr-java-test-reviewer` | `<SIDECAR_DIR>/java-test-reviewer-<i>.jsonl` |
| `dotcms-pr-ac-validator` | `<SIDECAR_DIR>/ac-validator.jsonl` |
| `dotcms-pr-test-case-designer` | `<SIDECAR_DIR>/test-case-designer.jsonl` |
| `dotcms-pr-ci-reviewer` | `<SIDECAR_DIR>/ci-reviewer-<i>.jsonl` |
| `dotcms-pr-maven-reviewer` | `<SIDECAR_DIR>/maven-reviewer.jsonl` |
| `dotcms-pr-docs-reviewer` | `<SIDECAR_DIR>/docs-reviewer-<i>.jsonl` |

`<i>` is the 1-based instance index of a multi-instance agent: two instances never share a sidecar.

Three row shapes come back, and `merge-sidecars.sh --help` lists them: the finding row of every reviewer, the AC-coverage row of the ac-validator (one per AC), and the test-design row of the test-case designer (one per `TC-###`).

## After the batch returns

Check the sidecars before anything else (Stage 4b reads the result):

```bash
bash .claude/skills/dot-pr-review/scripts/merge-sidecars.sh --dir <SIDECAR_DIR> --out <RUN_DIR>/merged --roster <RUN_DIR>/roster.txt
```

It prints the rows and invalid lines per sidecar, `MISSING` for a launched agent with no sidecar and `EMPTY` for one with no valid row. An `EMPTY` or `MISSING` sidecar from an agent whose final message lists findings means that message is the only copy of them: Stage 4b turns it into rows before anything is merged.
