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
# Smart hook dispatcher — works across old (husky) and new (lefthook) branches.
# Written by core-web/prepare.js on yarn install.

root="$(git rev-parse --show-toplevel)"

if [ -f "$root/lefthook.yml" ]; then
    # --- mise is required: lefthook commands use 'mise exec --' for toolchain ---
    if ! command -v mise >/dev/null 2>&1; then
        shell_name="$(basename "\${SHELL:-/bin/sh}")"
        case "$shell_name" in
            zsh)  rc_file="~/.zshrc";    activate_cmd='eval "$(mise activate zsh)"' ;;
            bash) rc_file="~/.bashrc";   activate_cmd='eval "$(mise activate bash)"' ;;
            fish) rc_file="~/.config/fish/config.fish"; activate_cmd='mise activate fish | source' ;;
            *)    rc_file="your shell rc"; activate_cmd="eval \\"\\$(mise activate $shell_name)\\"" ;;
        esac

        echo ""
        echo "dotCMS uses mise to manage development tools (Node, Java, lefthook, etc.)."
        echo ""
        echo "1. Install mise:"
        echo "   curl https://mise.run | sh"
        echo ""
        echo "2. Activate in your shell ($shell_name):"
        echo "   echo '$activate_cmd' >> $rc_file"
        echo "   source $rc_file"
        echo ""
        echo "3. Install project tools:"
        echo "   mise trust && mise install"
        echo ""
        echo "Skip this hook with: LEFTHOOK=0 git ${gitCmd(hookName)}"
        exit 1
    fi

    # --- Auto-install lefthook and project tools if missing ---
    if ! command -v lefthook >/dev/null 2>&1 && ! mise which lefthook >/dev/null 2>&1; then
        echo "Project tools not installed. Running 'mise install'..."
        mise trust --yes 2>/dev/null
        if ! mise install; then
            echo ""
            echo "ERROR: 'mise install' failed. Fix the issue above, then retry your command."
            echo "Skip this hook with: LEFTHOOK=0 git ${gitCmd(hookName)}"
            exit 1
        fi
        # Verify lefthook is now available
        if ! command -v lefthook >/dev/null 2>&1 && ! mise which lefthook >/dev/null 2>&1; then
            echo ""
            echo "ERROR: lefthook not found after 'mise install'."
            echo "Try: mise install lefthook"
            exit 1
        fi
        echo ""
    fi

    if command -v lefthook >/dev/null 2>&1; then
        exec lefthook run ${hookName} --no-auto-install "$@"
    else
        exec mise exec -- lefthook run ${hookName} --no-auto-install "$@"
    fi
elif [ -f "$root/core-web/.husky/${hookName}" ]; then
    # Old branch: delegate to the husky hook script
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
