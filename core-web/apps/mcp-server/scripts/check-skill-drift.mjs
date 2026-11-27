/**
 * The VTL skill served by this app is a vendored copy of
 * `dotCMS/agent-toolkit` at the commit in `skill-upstream.json`.
 *
 * That repository is the source of truth. This check downloads those files and
 * fails when the copy here differs from them. The JSON lists the only edits
 * this server is allowed to make (frontmatter the MCP Skills extension rejects,
 * and two phrases that cite a dotCMS checkout). Each edit must match the
 * upstream bytes exactly once. Anything else is drift.
 *
 * The build runs this before `xmcp build`, so a stale skill never ships.
 */
import { createHash } from 'node:crypto';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const APP_ROOT = join(dirname(fileURLToPath(import.meta.url)), '..');
const PIN = JSON.parse(readFileSync(join(APP_ROOT, 'scripts/skill-upstream.json'), 'utf8'));

const sha256 = (bytes) => createHash('sha256').update(bytes).digest('hex');

/**
 * @param {string} repository `owner/name`
 * @param {string} commit
 * @param {string} upstreamPath
 * @returns {Promise<Buffer>}
 */
async function fetchUpstream(repository, commit, upstreamPath) {
    const url = `https://raw.githubusercontent.com/${repository}/${commit}/${upstreamPath}`;
    const response = await fetch(url, { signal: AbortSignal.timeout(30_000) });
    if (!response.ok) {
        throw new Error(`${url} returned HTTP ${response.status}`);
    }
    return Buffer.from(await response.arrayBuffer());
}

/**
 * @param {Buffer} bytes
 * @param {{ from: string, to: string, reason: string }[]} replacements
 * @param {string} label
 */
function applyReplacements(bytes, replacements, label) {
    let text = bytes.toString('utf8');
    for (const replacement of replacements) {
        const found = text.split(replacement.from).length - 1;
        if (found !== 1) {
            throw new Error(
                `${label}: expected the allowed edit to match once, found ${found}.\n` +
                    `  reason: ${replacement.reason}\n` +
                    `  looking for: ${JSON.stringify(replacement.from).slice(0, 180)}`
            );
        }
        text = text.replace(replacement.from, replacement.to);
    }
    return Buffer.from(text, 'utf8');
}

const failures = [];

for (const file of PIN.files) {
    const localPath = join(APP_ROOT, file.served);
    let upstream;
    try {
        upstream = await fetchUpstream(PIN.repository, PIN.commit, file.upstream);
    } catch (error) {
        failures.push(
            `${file.upstream}: ${(error instanceof Error ? error : new Error(String(error))).message}`
        );
        continue;
    }

    let expected;
    try {
        expected = applyReplacements(upstream, file.replacements ?? [], file.upstream);
    } catch (error) {
        failures.push(error instanceof Error ? error.message : String(error));
        continue;
    }

    let local;
    try {
        local = readFileSync(localPath);
    } catch (error) {
        failures.push(`${file.served}: ${error instanceof Error ? error.message : String(error)}`);
        continue;
    }

    if (!local.equals(expected)) {
        failures.push(
            [
                `${file.served} does not match ${PIN.repository}@${PIN.commit.slice(0, 12)}:${file.upstream}`,
                `  after the ${file.replacements?.length ?? 0} allowed edit(s).`,
                `  upstream+edits sha256 ${sha256(expected)} (${expected.byteLength} bytes)`,
                `  served          sha256 ${sha256(local)} (${local.byteLength} bytes)`
            ].join('\n')
        );
    }
}

if (failures.length > 0) {
    console.error(
        [
            '',
            `Skill drift: served files must match ${PIN.repository}@${PIN.commit}`,
            'except the edits listed in scripts/skill-upstream.json.',
            '',
            ...failures.map((failure) => failure),
            ''
        ].join('\n')
    );
    process.exit(1);
}

console.log(
    `skill drift: ${PIN.files.length} file(s) match ${PIN.repository}@${PIN.commit.slice(0, 12)}`
);
