Use the `dotcms-worktree` skill to manage isolated development worktrees via worktrunk (`wt`).

**Do not use the `EnterWorktree` tool** — it bypasses worktrunk's post-create hooks and creates cold-start worktrees missing build artifacts, dependencies, and git hooks. Always use `wt switch --create` via Bash.

User's request: $ARGUMENTS
