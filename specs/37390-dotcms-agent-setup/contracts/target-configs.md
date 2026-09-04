# Contract: per-target configuration files

The seven registry entries, in full. These are files the tool does not own — every write merges into whatever is already there. This table is the whole of what differs between targets; the setup flow branches on none of it (FR-013).

---

## Registry

| id | `skills` id | Global config (`-g`) | Folder config (default) | Format | Container key |
|---|---|---|---|---|---|
| `claude-code` | `claude-code` | `~/.claude.json` | `.mcp.json` | JSON | `mcpServers` |
| `cursor` | `cursor` | `~/.cursor/mcp.json` | `.cursor/mcp.json` | JSON | `mcpServers` |
| `vscode` | `github-copilot` | see platform paths below | `.vscode/mcp.json` | JSON | **`servers`** |
| `codex` | `codex` | `$CODEX_HOME/config.toml`, else `~/.codex/config.toml` | `.codex/config.toml` | **TOML** | `mcp_servers` |
| `antigravity` | `antigravity` | `~/.gemini/config/mcp_config.json` | `.agents/mcp_config.json` | JSON | `mcpServers` |
| `devin` | `devin` | `~/.config/devin/mcp_config.json` | `.devin/mcp_config.local.json` | JSON | `mcpServers` |
| `opencode` | `opencode` | `~/.config/opencode/opencode.json` | `./opencode.json` | JSON | **`mcp`** |

**VS Code global path, per platform** — all three need a test with `os.homedir()` and `process.platform` mocked:

| Platform | Path |
|---|---|
| macOS | `~/Library/Application Support/Code/User/mcp.json` |
| Windows | `%APPDATA%\Code\User\mcp.json` |
| Linux | `~/.config/Code/User/mcp.json` |

**Detection probes** — mirroring the `skills` CLI's, plus the VS Code user directory. Advisory: an undetected editor is still explicitly selectable.

`~/.claude` · `~/.cursor` · `~/.codex` · `~/.copilot` · `~/.gemini` · `~/.config/devin` · `~/.config/opencode` · the VS Code user dir above.

---

## Entry written

Under the key `dotcms` inside the container key. Exactly one per file (FR-011b).

**Standard — `claude-code`, `cursor`, `vscode`, `antigravity`, `devin`:**

```json
{
  "type": "stdio",
  "command": "npx",
  "args": ["-y", "@dotcms/mcp-server@latest"],
  "env": {
    "DOTCMS_URL": "https://demo.dotcms.com",
    "AUTH_TOKEN": "<token>"
  }
}
```

**`opencode`** — different shape, not just a different key:

```json
{
  "type": "local",
  "command": ["npx", "-y", "@dotcms/mcp-server@latest"],
  "enabled": true,
  "environment": {
    "DOTCMS_URL": "https://demo.dotcms.com",
    "AUTH_TOKEN": "<token>"
  }
}
```

**`codex`** — TOML:

```toml
[mcp_servers.dotcms]
command = "npx"
args = ["-y", "@dotcms/mcp-server@latest"]

[mcp_servers.dotcms.env]
DOTCMS_URL = "https://demo.dotcms.com"
AUTH_TOKEN = "<token>"
```

> `AUTH_TOKEN`, **not** `DOTCMS_TOKEN`. That is the exact name `runtimeFromEnv()` reads in `core-web/apps/mcp-server/src/lib/runtime.ts`. Getting this wrong produces a server that starts and then fails every call — which is precisely the failure FR-024a's connection check exists to catch.

---

## Write rules

Apply to every target.

1. **Merge, never clobber.** Parse the existing document, insert or replace only the `dotcms` key, leave every other server and unrelated setting byte-for-byte identical (FR-016).
2. **Unparseable input is a named error.** Identify the file and the remedy, leave it untouched, never overwrite (FR-018). Example: ``~/.cursor/mcp.json is not valid JSON — fix it or re-run with --skip-mcp``.
3. **An existing `dotcms` entry prompts before replacement**, unless `--force` or `-y` (FR-017).
4. **Create missing files and parent directories** (FR-019).
5. **Owner-only permissions** — `0600` on files, `0700` on created directories. POSIX only; on Windows `chmod` does not touch ACLs, so the step is skipped and the summary says the file could not be restricted (FR-021, research R5).
6. **Two targets resolving to one path are written once**, not twice (spec Edge Cases).
7. **JSON is written with 2-space indentation.** TOML round-trips via `smol-toml`, preserving comments and unrelated tables (research R6).
8. **One target's failure does not stop the others** and does not roll back what already succeeded (FR-020a, FR-020d).

## Version-control safety — folder scope

Folder scope is the **default**, so this is the common path, not an edge case.

- Name every file a token was written into, and offer to add them to `.gitignore`, defaulting to yes. `-y` takes the safe answer (exclude) rather than skipping the step (FR-023).
- Not a git repository: still name the files and warn they are unprotected (FR-023a).
- `.mcp.json` at a repository root is conventionally **committed** — warn explicitly that a token there risks publication (FR-024). This is the one place the ordinary default is actively wrong.
