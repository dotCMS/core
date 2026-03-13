const isCi = process.env.CI !== undefined;
if (!isCi) {
    const { execSync } = require('child_process');

    // Undo husky's core.hooksPath so git uses the default .git/hooks/ location.
    try {
        execSync('git config --unset core.hooksPath', { stdio: 'ignore' });
    } catch (_) {
        // Already unset — fine
    }

    // Let lefthook install its own hooks into .git/hooks/.
    // Lefthook is provided by mise (see .mise.toml) and auto-installed on cd.
    try {
        execSync('lefthook install', { stdio: 'ignore' });
    } catch (_) {
        // lefthook not available yet — mise will install it on next cd
    }
}