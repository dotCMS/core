import cfonts from 'cfonts';
import chalk from 'chalk';

import type { TargetOutcome } from './types';

/** Matches `create-app`'s printWelcomeScreen() so the two commands read as one tool. */
export function printBanner(): void {
    cfonts.say('dotCMS', { font: 'block', align: 'left', colors: ['red', 'white'], space: false });
    process.stdout.write('  Connect your AI coding agent to a dotCMS instance\n\n');
}

export interface VersionControlSummary {
    files: string[];
    inRepository: boolean;
    excluded: boolean;
    warnings: string[];
}

export interface SummaryInput {
    outcomes: TargetOutcome[];
    versionControl?: VersionControlSummary;
    connection: 'ok' | 'failed' | 'skipped';
    connectionReason?: string;
    nextStep?: string;
}

const RESULT_MARK: Record<TargetOutcome['result'], string> = {
    written: chalk.green('✓ written '),
    replaced: chalk.green('✓ replaced'),
    skipped: chalk.dim('· skipped '),
    failed: chalk.red('✗ failed  ')
};

/**
 * The end-of-run summary (FR-028).
 *
 * With no status command and no verbose mode, this is most of what the developer ever sees, so
 * it must never overstate: never "ready" unless the connection was actually confirmed
 * (FR-024e), never "skills installed" for a location we have not verified (FR-027), and never
 * silence about permissions we could not apply (research R5).
 */
export function renderSummary(input: SummaryInput): string {
    const lines: string[] = [];

    for (const o of input.outcomes) {
        const bits = [
            `  ${RESULT_MARK[o.result]}`,
            o.targetId.padEnd(13),
            o.scope.padEnd(7),
            o.path ?? '—'
        ];
        lines.push(bits.join('  '));
        if (o.result === 'failed' && o.reason) lines.push(`      ${o.reason}`);
        if (!o.permissionsApplied && o.path) {
            lines.push('      could not restrict file permissions on this platform');
        }
        if (o.skillsInstalled === 'unverified') {
            lines.push('      skills location unverified for this editor — not confirmed installed');
        }
    }

    if (input.versionControl?.files.length) {
        const vc = input.versionControl;
        lines.push('');
        lines.push('  These files now contain an access token:');
        for (const f of vc.files) lines.push(`      ${f}`);
        if (vc.excluded) lines.push(chalk.green('  ✓ added to .gitignore'));
        else if (vc.inRepository) lines.push(chalk.yellow('  ! not excluded from version control'));
        for (const w of vc.warnings) lines.push(chalk.yellow(`  ! ${w}`));
    }

    lines.push('');
    if (input.connection === 'ok') {
        lines.push(chalk.green('  ✓ server responded'));
    } else if (input.connection === 'skipped') {
        lines.push('  · connection check skipped');
    } else {
        // Deliberately avoids the word "ready" — the run did not reach that state.
        lines.push(chalk.red(`  ✗ ${input.connectionReason ?? 'the server did not start'}`));
        lines.push('    Configuration was written and left in place; the server did not come up.');
    }

    const allGood =
        input.connection === 'ok' && input.outcomes.every((o) => o.result !== 'failed');
    if (allGood) {
        lines.push('');
        lines.push(`  Ready — ${input.nextStep ?? 'open your editor and start using dotCMS.'}`);
    }

    return lines.join('\n');
}

/**
 * The summary is the product's primary output, not a debug log — but the workspace lint rule
 * reserves `console` for warn/error. Writing to stdout directly says what is meant: this is
 * the command's result, and it belongs on stdout so it can be piped.
 */
export function writeOut(text: string): void {
    process.stdout.write(`${text}\n`);
}
