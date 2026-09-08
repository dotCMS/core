/**
 * Template-aware checking, via the Angular compiler API.
 *
 * A separate mechanism from check-ts by necessity, not preference. Angular's strictness settings
 * are not TypeScript compiler options: `ngc` parses its arguments with `ts.parseCommandLine` and
 * tolerates only five non-TypeScript options (i18nFile, i18nFormat, locale, missingTranslation,
 * watch), so `--strictTemplates` is rejected outright. They can only reach the compiler through
 * configuration — and `readConfiguration(project, existingOptions)` spreads `existingOptions`
 * last, above everything read from the extends chain. That forces them in memory, with no
 * overlay file to leave behind if the process dies.
 */
import path from 'node:path';
import { loadAngularCompiler, loadTypeScript } from './resolve-tools.mjs';
import { resolveFlagSet } from './check-ts.mjs';

/**
 * The four settings the workspace already treats as its Angular convention: 30 project configs
 * declare strictTemplates, 9 declare typeCheckHostBindings. `extendedDiagnostics` is deliberately
 * NOT here — promoting a whole diagnostic category to errors makes a future framework minor able
 * to fail pull requests for something they did not change (FR-018).
 */
export const ANGULAR_STRICT = {
    strictTemplates: true,
    strictInjectionParameters: true,
    strictInputAccessModifiers: true,
    typeCheckHostBindings: true
};

/**
 * Angular encodes its error codes as negative TypeScript codes: NG8002 becomes -998002
 * (`'-99' + code`). Recovering the display form keeps the report readable and greppable.
 */
export function formatCode(code) {
    if (code >= 0) return `TS${code}`;
    const recovered = Math.abs(code) - 990000;
    return recovered > 0 ? `NG${recovered}` : `NG${Math.abs(code)}`;
}

function toDiagnostic(ts, diagnostic) {
    const file = diagnostic.file;
    const { line, character } =
        file && diagnostic.start !== undefined
            ? file.getLineAndCharacterOfPosition(diagnostic.start)
            : { line: 0, character: 0 };
    const fileName = file ? path.resolve(file.fileName) : '<unknown>';
    return {
        file: fileName,
        line: line + 1,
        column: character + 1,
        code: formatCode(diagnostic.code),
        message: ts.flattenDiagnosticMessageText(diagnostic.messageText, ' '),
        // A diagnostic is template-layer if it came from Angular, or if it landed in a template
        // file. An inline template reports against the component source, so the code decides.
        layer: diagnostic.code < 0 || fileName.endsWith('.html') ? 'template' : 'source'
    };
}

/**
 * @param {{ configPath: string, flagSet?: string, forceTemplates?: boolean }} input
 * @returns {Promise<{ diagnostics: object[] }>} `file` is absolute; the caller relativizes.
 */
export async function checkAngularTemplates({ configPath, flagSet = 'strict', forceTemplates = true }) {
    const ng = await loadAngularCompiler();
    const ts = await loadTypeScript();

    const overrides = {
        ...resolveFlagSet(flagSet),
        ...(forceTemplates ? ANGULAR_STRICT : {}),
        noEmit: true
    };

    const config = ng.readConfiguration(configPath, overrides);
    if (config.errors?.length) {
        throw new Error(`cannot read ${configPath}: ${ts.flattenDiagnosticMessageText(config.errors[0].messageText, ' ')}`);
    }

    const host = ng.createCompilerHost({ options: config.options });
    const program = ng.createProgram({ rootNames: config.rootNames, options: config.options, host });

    // Structural diagnostics must be requested before the semantic ones, or ngtsc has not yet
    // analysed the component scopes the template check depends on.
    const collected = [];
    for (const method of [
        'getNgStructuralDiagnostics',
        'getTsSyntacticDiagnostics',
        'getTsSemanticDiagnostics',
        'getNgSemanticDiagnostics'
    ]) {
        if (typeof program[method] === 'function') {
            collected.push(...(await program[method]()));
        }
    }

    return { diagnostics: collected.filter((d) => d.file).map((d) => toDiagnostic(ts, d)) };
}
