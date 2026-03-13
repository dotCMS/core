const isCi = process.env.CI !== undefined;
if (!isCi) {
    const { execSync } = require('child_process');
    const fs = require('fs');
    const path = require('path');

    // Determine the git hooks directory (.git/hooks for main repo or worktree)
    let hooksDir;
    try {
        // git rev-parse --git-path hooks resolves correctly for worktrees
        hooksDir = execSync('git rev-parse --git-path hooks', { encoding: 'utf8' }).trim();
    } catch (_) {
        hooksDir = '.git/hooks';
    }

    // Remove core.hooksPath so git uses the standard .git/hooks/ location.
    // Old branches' prepare.js may re-set this — that's fine, their checked-out
    // core-web/.husky/pre-commit contains the full husky script.
    try {
        execSync('git config --unset core.hooksPath', { stdio: 'ignore' });
    } catch (_) {
        // Already unset or not configurable — fine
    }

    // Write branch-aware hook scripts to .git/hooks/
    // These detect whether the current worktree uses lefthook (new) or husky (old).
    // LEFTHOOK_NO_AUTO_INSTALL=1 prevents lefthook from overwriting these hooks
    // with its own generated versions when `lefthook run` is called.
    const gitCmd = (name) =>
        ({ 'pre-commit': 'commit', 'pre-push': 'push', 'post-merge': 'merge' })[name] || name;
    const hookScript = (hookName) => `#!/bin/sh
# Hook dispatcher — works across old (husky) and new (lefthook) branches.
# Written by core-web/prepare.js on yarn install.

root="$(git rev-parse --show-toplevel)"

if [ -f "$root/lefthook.yml" ]; then
    # New branch: dispatch to lefthook (requires mise for toolchain)
    if ! command -v lefthook >/dev/null 2>&1 && ! mise which lefthook >/dev/null 2>&1; then
        echo "ERROR: lefthook not found. Run: mise install"
        echo "Skip this hook with: LEFTHOOK=0 git ${gitCmd(hookName)}"
        exit 1
    fi
    if command -v lefthook >/dev/null 2>&1; then
        exec lefthook run ${hookName} --no-auto-install "$@"
    else
        exec mise exec -- lefthook run ${hookName} --no-auto-install "$@"
    fi
elif [ -f "$root/core-web/.husky/${hookName}" ]; then
    # Old branch: delegate to the husky hook script as-is
    exec "$root/core-web/.husky/${hookName}" "$@"
fi
`;

    try {
        fs.mkdirSync(hooksDir, { recursive: true });
        for (const hook of ['pre-commit', 'pre-push', 'post-merge']) {
            const hookPath = path.join(hooksDir, hook);
            fs.writeFileSync(hookPath, hookScript(hook));
            fs.chmodSync(hookPath, 0o755);
        }
    } catch (_) {
        // Non-fatal — hooks are a convenience, not a hard requirement
    }
}
