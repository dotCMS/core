# Contract: what the skill tells the user

The user-facing contract of `dot-ui-vtl-migration` after this change. `SKILL.md` must encode every row; [../scenarios.md](../scenarios.md) checks them.

## Mode resolution

Keywords are read only from the user's own words, never from pasted VTL, code blocks or file contents (FR-005).

| Request contains | Mode | Path needed? |
|---|---|---|
| nothing about shape | **inline** (default, FR-003) | no (FR-004) |
| "inline", "single file", "one file", "un solo archivo", "en línea" | inline | no |
| "three files", "separate", "router", "#parse", "tres archivos", "separado(s)", "enrutador" | three-file (FR-005) | yes — ask if missing |
| signals for both | ask which one (FR-007) | — |
| "convert to inline" / "collapse" + a router or pair | collapse | only to locate files |
| "split" + an inline file | split | yes — server path for the router |

## Every answer starts with a mode line (FR-006)

- `Output: inline, single file — both branches are parsed together; checks found nothing blocking.`
- `Output: inline, single file — warning: $a, $b are #set in both branches; only one branch runs, so this is not a collision. Three files is the alternative.`
- `Output: three files with router (_old.vtl, _new.vtl, <name>.vtl).`
- `Output: none — inline blocked: <finding>. <why it reaches both edit modes>. Three files avoids it. Reply "inline anyway" to force, or "three files".`

## Blocking behavior (FR-010)

On a blocking finding the skill produces **no file**. It names the macro or the line, explains that inline parses both branches so the problem reaches the new edit mode too, and offers three files. It emits inline only after the user explicitly chooses it in reply.

## Unchanged (FR-005)

The three-file output — file names, router shape, full-path `#parse`, and the question for a missing server path — is byte-for-byte today's "File Output Pattern".
