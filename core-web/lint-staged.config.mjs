import path from 'node:path';

const coreWebDir = path.resolve(import.meta.dirname);

const toRelative = (files) => files.map((f) => path.relative(coreWebDir, f)).join(',');

// Globs use `**/*.{...}` (not bare `*.{...}`) to match at any depth without
// depending on minimatch's matchBase fallback. Files outside core-web/ are
// already filtered out by lint-staged because the hook runs from core-web/
// (lint-staged ignores staged files outside its cwd).
//
// Globs are non-overlapping: lint-staged runs different keys in parallel,
// and two parallel `nx ... --files=<csv>` against the same file race on
// git's index.lock when they re-stage results. Keeping TS/JS in one key
// runs lint and format sequentially via the returned array.
//
// `--files=` is quoted so a path with a space (unusual in this repo, but
// not impossible) doesn't break shell parsing. Commas inside the CSV are
// shell-safe.
export default {
    '**/*.{ts,js,mjs,cjs,tsx,jsx}': (files) => {
        const list = toRelative(files);
        return [
            `yarn nx affected -t lint --exclude=tag:skip:lint --fix --files="${list}"`,
            `yarn nx format:write --files="${list}"`
        ];
    },
    '**/*.{json,html,css,scss,md,yaml,yml}': (files) => {
        const list = toRelative(files);
        return `yarn nx format:write --files="${list}"`;
    }
};
