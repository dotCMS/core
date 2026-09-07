#!/usr/bin/env node
/**
 * strict-gate — diff-scoped strict typecheck. Spike harness for issue #37401.
 * Command contract: specs/37401-diff-scoped-strict-typecheck-gate/contracts/cli.md
 */
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { workspaceRoot } from './lib/resolve-tools.mjs';
import { resolveChangedFiles } from './lib/changed-files.mjs';
import { mapFilesToProjects, readProjects } from './lib/project-map.mjs';
import { selectConfigs } from './lib/config-select.mjs';
import { checkTypeScript } from './lib/check-ts.mjs';
import { checkAngularTemplates } from './lib/check-ng.mjs';
import { selectMode } from './lib/mode-select.mjs';
import { filterDiagnostics } from './lib/filter.mjs';
import { buildReport } from './lib/report.mjs';
import { FORMATTERS } from './lib/format.mjs';

const DEFAULT_REPO_DIR = path.resolve(workspaceRoot, '..');

/**
 * @param {{ repoDir?: string, base: string, head?: string,
 *           flagSet?: string, granularity?: 'file'|'line', templates?: boolean }} options
 */
/**
 * Everything expensive: resolve the diff, map it, and compile. Granularity is NOT an input here —
 * it only affects filtering, so the matrix can compile once and filter twice instead of paying
 * for a second identical compilation.
 */
export async function collectDiagnostics({
    repoDir = DEFAULT_REPO_DIR,
    base,
    head = 'HEAD',
    flagSet = 'strict',
    templates = false,
    scope = 'core-web'
} = {}) {
    const started = performance.now();
    let typescriptMs = 0;
    let templateAwareMs = 0;

    const { files: allFiles, base: baseSha, head: headSha } = await resolveChangedFiles({ repoDir, base, head });
    const files = scope ? allFiles.filter((f) => f.path === scope || f.path.startsWith(`${scope}/`)) : allFiles;

    if (files.length === 0) {
        return {
            files, baseSha, headSha, targets: [], unmapped: [], diagnostics: [], projectRoots: [],
            durationMs: { total: performance.now() - started, typescript: 0, templateAware: 0 }
        };
    }

    const projects = await readProjects({ workspaceDir: workspaceRoot, repoDir });
    const { targets, unmapped } = mapFilesToProjects({ projects, files });

    const resolvedTargets = [];
    const diagnostics = [];

    for (const target of targets) {
        const configs = await selectConfigs({
            workspaceDir: workspaceRoot,
            repoDir,
            project: { name: target.project, root: target.root },
            files: target.files
        });

        if (configs.length === 0) {
            for (const file of target.files) {
                unmapped.push({
                    path: file,
                    reason: `project '${target.project}' has no configuration that includes this file`
                });
            }
            continue;
        }

        for (const config of configs) {
            // Reported, never assumed: a project that falls back to TypeScript-only appears in the
            // report as having done so, because a silent fallback means unchecked templates behind
            // a PASS.
            const decision = await selectMode({ configPath: config.configPath, templates });
            const started = performance.now();

            const { diagnostics: raw } =
                decision.mode === 'template-aware'
                    ? await checkAngularTemplates({ configPath: config.configPath, flagSet })
                    : await checkTypeScript({
                          workspaceDir: workspaceRoot,
                          configPath: config.configPath,
                          flagSet
                      });

            const elapsed = performance.now() - started;
            if (decision.mode === 'template-aware') templateAwareMs += elapsed;
            else typescriptMs += elapsed;

            resolvedTargets.push({
                ...config,
                configPath: path.relative(repoDir, config.configPath),
                mode: decision.mode
            });
            diagnostics.push(...raw.map((d) => ({ ...d, file: path.relative(repoDir, d.file) })));
        }
    }

    return {
        files, baseSha, headSha,
        targets: resolvedTargets,
        unmapped,
        diagnostics,
        projectRoots: targets.map((t) => t.root),
        durationMs: {
            total: performance.now() - started,
            typescript: typescriptMs,
            templateAware: templateAwareMs
        }
    };
}

/** Builds one report from a collected pass, at a given granularity. */
export function reportFrom(collected, { flagSet, granularity }) {
    const { findings, discarded } = filterDiagnostics({
        diagnostics: collected.diagnostics,
        changedFiles: collected.files,
        granularity,
        projectRoots: collected.projectRoots
    });
    return buildReport({
        base: collected.baseSha,
        head: collected.headSha,
        flagSet,
        granularity,
        targets: collected.targets,
        unmapped: collected.unmapped,
        findings,
        discarded,
        durationMs: collected.durationMs
    });
}

export async function runGate({
    repoDir = DEFAULT_REPO_DIR,
    base,
    head = 'HEAD',
    flagSet = 'strict',
    granularity = 'line',
    templates = false,
    // Hard scope. The gate is a frontend concern: a pull request that touches only backend code
    // must be a no-op, and the harness must never wander outside core-web even if a stray .ts
    // exists elsewhere in the repo. CI additionally gates the whole job on the same path filter.
    scope = 'core-web'
} = {}) {
    const collected = await collectDiagnostics({ repoDir, base, head, flagSet, templates, scope });
    return reportFrom(collected, { flagSet, granularity });
}

function parseArgs(argv) {
    const options = {};
    for (let i = 0; i < argv.length; i += 1) {
        const [flag, inlineValue] = argv[i].split('=');
        const value = inlineValue ?? argv[i + 1];
        const consume = () => {
            if (inlineValue === undefined) i += 1;
        };
        switch (flag) {
            case '--base': options.base = value; consume(); break;
            case '--head': options.head = value; consume(); break;
            case '--flags': options.flagSet = value; consume(); break;
            case '--granularity': options.granularity = value; consume(); break;
            case '--templates': options.templates = value === 'on'; consume(); break;
            case '--report': options.report = value; consume(); break;
            case '--format': options.format = value; consume(); break;
            case '--scope': options.scope = value === 'none' ? null : value; consume(); break;
            default: throw new Error(`unknown option '${flag}' — see contracts/cli.md`);
        }
    }
    return options;
}

async function main(argv) {
    const { report: reportPath, format = 'text', ...options } = parseArgs(argv);
    if (!options.base) throw new Error('--base is required');

    const render = FORMATTERS[format];
    if (!render) throw new Error(`unknown format '${format}' — one of ${Object.keys(FORMATTERS).join(', ')}`);

    const report = await runGate(options);

    // The JSON is the machine record; the chosen format is what a reader (human or agent) acts on.
    if (reportPath && reportPath !== '-') {
        const { writeFile } = await import('node:fs/promises');
        await writeFile(reportPath, JSON.stringify(report, null, 2), 'utf8');
    }
    process.stdout.write(`${render(report)}\n`);

    // GitHub Actions: annotations go to the log, the summary goes to the run page.
    if (process.env.GITHUB_STEP_SUMMARY && format === 'github') {
        const { appendFile } = await import('node:fs/promises');
        await appendFile(process.env.GITHUB_STEP_SUMMARY, `${FORMATTERS.markdown(report)}\n`, 'utf8');
    }
    return report.exitCode;
}

if (process.argv[1] === fileURLToPath(import.meta.url)) {
    main(process.argv.slice(2)).then(
        (code) => process.exit(code),
        (error) => {
            // Exit 2, never 1: a harness that could not run must never look like a clean gate.
            process.stderr.write(`strict-gate: ${error.message}\n`);
            process.exit(2);
        }
    );
}
