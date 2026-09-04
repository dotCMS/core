/**
 * Fails the build when `src/tools/` holds anything that is not a tool.
 *
 * xmcp loads EVERY module under the configured tools path (see `paths.tools` in
 * xmcp.config.ts) and awaits them all at startup — its discovery glob is hard-coded to
 * `<toolsPath>/**\/*.{ts,tsx}` and offers no exclude option, so a colocated test file is
 * bundled into the published artifact and executed as a tool. `describe` does not exist at
 * runtime, so the server dies with `ReferenceError: describe is not defined` before it reads
 * any configuration, for every user. That is exactly what shipped as @dotcms/mcp-server@0.1.0
 * (issue #37337).
 *
 * The rule this enforces: tests for tool code live in `src/lib/` next to the logic they
 * cover, never in `src/tools/`.
 */
import { readdirSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const TOOLS_DIR = join(dirname(fileURLToPath(import.meta.url)), '..', 'src', 'tools');
const FORBIDDEN = /\.(spec|test)\.tsx?$/;

const offenders = readdirSync(TOOLS_DIR, { recursive: true, withFileTypes: true })
    .filter((entry) => entry.isFile() && FORBIDDEN.test(entry.name))
    .map((entry) => join(entry.parentPath ?? entry.path, entry.name));

if (offenders.length > 0) {
    console.error(
        [
            '',
            'xmcp loads every module under src/tools/ as a tool at startup, so a test file there',
            'is bundled into the published package and crashes the server on boot for every user.',
            '',
            'Move these out of src/tools/ — put the logic and its test in src/lib/:',
            ...offenders.map((file) => `  ✗ ${file}`),
            ''
        ].join('\n')
    );
    process.exit(1);
}
