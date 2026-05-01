import path from 'node:path';

const coreWebDir = path.resolve(import.meta.dirname);

const toRelative = (files) =>
    files.map((f) => path.relative(coreWebDir, f)).join(',');

// Globs are non-overlapping on purpose: lint-staged runs different keys in
// parallel by default, and two parallel `nx ... --files=<csv>` invocations
// against the same file race on git's index.lock when they re-stage results.
// Keeping TS/JS in one key lets lint and format run sequentially via the
// returned array.
export default {
    '*.{ts,js,tsx,jsx}': (files) => {
        const list = toRelative(files);
        return [
            `yarn nx affected -t lint --exclude=tag:skip:lint --fix --files=${list}`,
            `yarn nx format:write --files=${list}`
        ];
    },
    '*.{json,html,css,scss}': (files) => {
        const list = toRelative(files);
        return `yarn nx format:write --files=${list}`;
    }
};
