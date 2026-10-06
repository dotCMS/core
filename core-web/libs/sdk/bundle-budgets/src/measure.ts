/* eslint-disable no-console -- printing the report is this script's entire purpose */
/**
 * Prints the current size of every probe. Use it to set or review `budgets.json`, and to
 * produce the before/after numbers for a bundle report.
 *
 * Usage: npx tsx libs/sdk/bundle-budgets/src/measure.ts
 */
import { modulesMatching, probe, stageSdkPackages } from './bundle-probe.ts';
import { PROBES } from './probes.ts';

async function main() {
    stageSdkPackages([...new Set(PROBES.flatMap((p) => p.packages))]);

    for (const spec of PROBES) {
        const result = await probe(spec.name, spec.source);
        const leaked = modulesMatching(result, spec.forbidden);

        console.log(
            `${spec.name.padEnd(20)} ${String(result.rawBytes).padStart(7)} raw  ` +
                `${String(result.gzipBytes).padStart(6)} gzip  ${result.modules.length} modules` +
                (leaked.length ? `  LEAKED: ${leaked.join(', ')}` : '')
        );
    }
}

main();
