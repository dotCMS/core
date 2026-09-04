import type { TargetOutcome } from './types';

export interface SummaryInput {
    outcomes: TargetOutcome[];
    connection: 'ok' | 'failed' | 'skipped';
    connectionReason?: string;
    nextStep?: string;
}

const RESULT_MARK: Record<TargetOutcome['result'], string> = {
    written: '✓ written',
    replaced: '✓ replaced',
    skipped: '· skipped',
    failed: '✗ failed'
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

    lines.push('');
    if (input.connection === 'ok') {
        lines.push('  ✓ server responded');
    } else if (input.connection === 'skipped') {
        lines.push('  · connection check skipped');
    } else {
        // Deliberately avoids the word "ready" — the run did not reach that state.
        lines.push(`  ✗ ${input.connectionReason ?? 'the server did not start'}`);
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
