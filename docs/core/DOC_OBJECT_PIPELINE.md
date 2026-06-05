# Doc Object Pipeline

Commit-bound documentation summaries generated at merge time and stored via `git notes`.

## Overview

When a PR merges to `main`, the `ai_claude-rollback-safety` workflow generates a structured
"doc object" — YAML frontmatter plus markdown prose — summarizing the change in documentation
terms. The object is attached to the squash commit SHA in a separate git ref that does not
appear in normal `git log` output:

    git notes --ref=refs/notes/doc-objects add -F doc-object.md <sha>

The pre-merge Claude job generates the doc object and stores it as a collapsible PR comment.
The post-merge `doc-object-attach` job reads that comment, substitutes the real merge SHA,
and pushes the note.

## Fetching doc objects locally

    git fetch origin refs/notes/doc-objects:refs/notes/doc-objects
    git notes --ref=refs/notes/doc-objects show <sha>

To walk all doc objects across a commit range (e.g. for release-notes input):

    git rev-list v25.04..v25.05 | while read sha; do
      git notes --ref=refs/notes/doc-objects show "$sha" 2>/dev/null && echo "---"
    done

## Schema

Each doc object has two parts:

**YAML frontmatter** — machine-readable fields used for filtering and routing:

| Field | Required | Description |
|---|---|---|
| `commit` | yes | 7-char short SHA |
| `title` | yes | Conventional-commit-style title |
| `type` | yes | `feature \| bugfix \| refactor \| docs \| chore \| security \| ci \| test \| revert` |
| `module` | yes | Affected product area in plain English |
| `customer_visible` | yes | `yes \| no \| indirect` |
| `security_relevant` | yes | `true \| false` |
| `breaking_change` | yes | `true \| false` |
| `severity` | bugs only | `critical \| high \| medium \| low` |
| `feature_flag` | when applicable | Flag constant name |
| `release_notes.audience` | yes | `customer \| internal \| both \| skip` |
| `release_notes.priority` | yes | `high \| medium \| low` |

**Markdown body** — human- and LLM-readable prose with sections chosen from a defined menu
(`## What changed`, `## Why it matters`, `## API surface`, `## Configuration`, etc.).

See `prompts/doc_object_generation.md` for the full schema, classification heuristics,
and body-section guidance the generation model follows.

## Audience filtering

The `release_notes.audience` field lets downstream pipelines pull only what is relevant.
A release-notes pipeline targeting customers excludes `audience: internal` and `audience: skip`
entries automatically, without re-reading any diffs.

## Provenance

Every doc object includes a `provenance:` block stamped with the model ID, prompt version,
and generation timestamp. This makes prompt-version upgrades auditable: regenerate with a
newer prompt version and the `provenance` block marks the difference.

