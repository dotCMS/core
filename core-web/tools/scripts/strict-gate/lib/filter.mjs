/**
 * The diff-scoped filter — the spike's hypothesis in code:
 * the dependency's errors do not need to be FIXED, they need to stop COUNTING.
 */

const inSpans = (line, spans) => spans.some(([start, end]) => line >= start && line <= end);

/**
 * Diagnostics that are never strictness violations, whatever the flags.
 *
 * Found by adjudicating the corpus: TS2307 fired on a pre-registered clean pull request, and it
 * appears with plain `tsc` too — the module simply does not resolve. Reporting it as strict debt
 * is crying wolf, and a gate that cries wolf gets switched off. Kept as a short, closed list of
 * resolution failures rather than a heuristic; anything broader would start hiding real findings.
 */
export const INFRASTRUCTURE_CODES = new Set([
    'TS2307', // Cannot find module
    'TS2688', // Cannot find type definition file
    'TS6053'  // File not found
]);

/**
 * @param {{
 *   diagnostics: object[],
 *   changedFiles: {path:string,changedLines:[number,number][]}[],
 *   granularity?: 'file'|'line',
 *   projectRoots?: string[]
 * }} input
 */
export function filterDiagnostics({ diagnostics, changedFiles, granularity = 'file', projectRoots }) {
    const changed = new Map(changedFiles.map((f) => [f.path, f]));

    // Distinguishing "another project's file" from "an untouched file of this project" needs to
    // know what this project owns. When the caller supplies roots we use them; otherwise we fall
    // back to the directories the diff touched, which is enough to keep the counts meaningful.
    const owned = projectRoots?.length
        ? (file) => projectRoots.some((r) => file === r || file.startsWith(`${r}/`))
        : (() => {
              const dirs = new Set(changedFiles.map((f) => f.path.slice(0, f.path.lastIndexOf('/'))));
              return (file) => [...dirs].some((d) => file.startsWith(`${d}/`));
          })();

    const findings = [];
    const discarded = {
        byOrigin: { dependency: 0, untouched: 0, infrastructure: 0 },
        byLayer: { source: 0, template: 0 }
    };

    for (const diagnostic of diagnostics) {
        const hit = changed.get(diagnostic.file);
        let origin;

        if (INFRASTRUCTURE_CODES.has(diagnostic.code)) {
            origin = 'infrastructure';
        } else if (!hit) {
            origin = owned(diagnostic.file) ? 'untouched' : 'dependency';
        } else if (granularity === 'line' && !inSpans(diagnostic.line, hit.changedLines)) {
            // Pre-existing debt on a line this pull request did not write. Whole-file granularity
            // would make whoever touched the file inherit it; line-level does not. New files are
            // unaffected — every line of an added file is a changed line.
            origin = 'untouched';
        } else {
            origin = 'changed';
        }

        if (origin === 'changed') {
            findings.push({ ...diagnostic, origin });
        } else {
            discarded.byOrigin[origin] += 1;
            discarded.byLayer[diagnostic.layer === 'template' ? 'template' : 'source'] += 1;
        }
    }

    return { findings, discarded };
}
