# dotcms

One command to connect an AI coding agent to a dotCMS instance.

```bash
npx dotcms agent setup
```

It asks where your instance is, how to authenticate, and which editors to wire up — then mints
and verifies a token, writes the dotCMS MCP server into each editor's own config file, installs
the dotCMS skills, and launches the server to confirm it responds before reporting success.

Replaces four manual steps: find the admin panel, mint a token by hand, hand-edit whichever
config file your editor reads, install the skills separately.

## Supported editors

Claude Code · Cursor · VS Code (Copilot) · Codex · Antigravity · Devin · OpenCode

Installed editors are detected and used by default; `--agent <id>` selects specific ones.

## Authentication

Two mutually exclusive modes. Passing both is a usage error, not a silent preference.

```bash
# mint a token from credentials
npx dotcms agent setup --url https://demo.dotcms.com --user admin@dotcms.com --password '…'

# or use a token you already have
npx dotcms agent setup --url https://demo.dotcms.com --authToken '…'
```

The URL plus one auth mode are the only required inputs — supply both and the command runs
without prompting, whether or not a terminal is attached.

> Passing a secret as a flag makes it visible in the process list and shell history. Prefer
> `DOTCMS_PASSWORD` / `DOTCMS_AUTH_TOKEN`, or let it prompt.

## Scope

Writes to the **current folder** by default, so each project can point at its own instance.
`-g/--global` writes to your user account instead. Folder scope offers to add the files it
touched to `.gitignore`, because they contain a token.

## Where the token goes

Only into the editor config files listed in the summary, restricted to your user where the
platform supports it. It is never logged in full, never passed as a process argument, and a
password is never written anywhere.

## Status

Not yet released. See `specs/37390-dotcms-agent-setup/` for the specification.
