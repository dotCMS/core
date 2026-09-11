/**
 * TypeScript-only checking, with strictness forced IN MEMORY.
 *
 * No overlay config is written: a crash mid-run would leave one behind and break the harness's
 * central promise that the working tree is byte-identical afterwards (SC-010).
 */
import path from 'node:path';
import { loadTypeScript, parseConfigFile } from './resolve-tools.mjs';

/**
 * `--strict` is an umbrella over eight flags and does NOT include the four below. Verified against
 * ts.optionDeclarations, and it matters: on the acceptance case bare --strict finds 2 of 5.
 * The `strict` set here mirrors tsconfig.base.json on PR #37198 — the gate must measure with the
 * same yardstick as the destination, or it passes debt that the migration will later have to fix.
 */
export const FLAG_SETS = {
    'null-checks': {
        noImplicitAny: true,
        strictNullChecks: true
    },
    strict: {
        strict: true,
        noPropertyAccessFromIndexSignature: true,
        noImplicitOverride: true,
        noImplicitReturns: true,
        noFallthroughCasesInSwitch: true
    },
    // Beyond #37198. Measured for a future ratchet; not the blocking set — a gate stricter than
    // the destination blocks pull requests for debt the destination does not consider debt.
    'strict-max': {
        strict: true,
        noPropertyAccessFromIndexSignature: true,
        noImplicitOverride: true,
        noImplicitReturns: true,
        noFallthroughCasesInSwitch: true,
        noUncheckedIndexedAccess: true,
        exactOptionalPropertyTypes: true
    }
};

/**
 * The single gate on flag-set names. Both checkers route through it so an unknown name can never
 * resolve to a default: the run would then measure one flag set while the report named another,
 * and the number would be wrong in a way nothing surfaces.
 */
export function resolveFlagSet(flagSet) {
    const overrides = FLAG_SETS[flagSet];
    if (!overrides) {
        throw new Error(
            `unknown flag set '${flagSet}' — one of ${Object.keys(FLAG_SETS).join(', ')}`
        );
    }
    return overrides;
}

export function toDiagnostic(ts, diagnostic, layer = 'source') {
    const file = diagnostic.file;
    const { line, character } = file && diagnostic.start !== undefined
        ? file.getLineAndCharacterOfPosition(diagnostic.start)
        : { line: 0, character: 0 };
    return {
        file: file ? path.resolve(file.fileName) : '<unknown>',
        line: line + 1,
        column: character + 1,
        code: `TS${diagnostic.code}`,
        message: ts.flattenDiagnosticMessageText(diagnostic.messageText, ' '),
        layer
    };
}

/**
 * @param {{ workspaceDir: string, configPath: string, flagSet?: keyof typeof FLAG_SETS }} input
 * @returns {Promise<{ diagnostics: object[] }>} `file` is absolute; the caller relativizes.
 */
export async function checkTypeScript({ configPath, flagSet = 'strict' }) {
    const ts = await loadTypeScript();
    const overrides = resolveFlagSet(flagSet);

    const parsed = await parseConfigFile(configPath);
    if (!parsed) throw new Error(`cannot parse ${configPath}`);

    const program = ts.createProgram({
        rootNames: parsed.fileNames,
        options: { ...parsed.options, ...overrides, noEmit: true, incremental: false },
        projectReferences: parsed.projectReferences
    });

    const raw = [...program.getSyntacticDiagnostics(), ...program.getSemanticDiagnostics()];
    return { diagnostics: raw.filter((d) => d.file).map((d) => toDiagnostic(ts, d)) };
}
