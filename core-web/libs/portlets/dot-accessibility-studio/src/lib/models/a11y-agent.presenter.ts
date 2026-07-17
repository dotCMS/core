import { AgentMessage, AgentMessagePresenter } from '@dotcms/ai-ui';
import { DotMessageService } from '@dotcms/data-access';
import { AgentRunStep } from '@dotcms/dotcms-models';

import { FixReport, FixResult, StudioStepPhase } from './accessibility-studio.models';

/** Icon for each live agent step phase (SSE `step` events). */
const STEP_PHASE_ICON: Record<StudioStepPhase, string> = {
    scan: 'pi pi-search',
    locate: 'pi pi-sitemap',
    read: 'pi pi-file',
    fix: 'pi pi-wrench',
    rescan: 'pi pi-verified'
};

/** Fix statuses that are surfaced as "reported" (not auto-fixed) in the log. */
const REPORTED_STATUSES: ReadonlyArray<FixResult['status']> = [
    'reported',
    'skipped',
    'regressed',
    'failed'
];

/**
 * Maps the accessibility agent's stream + {@link FixReport} into activity-log
 * bubbles. This is the a11y agent's implementation of the shared
 * {@link AgentMessagePresenter} seam — the only place that knows about axe rules,
 * fix statuses, and the scan→fix→rescan recipe. Depends only on
 * {@link DotMessageService} for i18n, so it can be constructed anywhere.
 */
export class A11yAgentPresenter implements AgentMessagePresenter<FixReport> {
    constructor(private readonly dm: DotMessageService) {}

    /** A live step → an info bubble, icon chosen from the step's `phase` meta. */
    liveStep(step: AgentRunStep, index: number): AgentMessage {
        const phase = step.meta?.['phase'] as StudioStepPhase | undefined;
        return {
            id: index,
            icon: phase ? STEP_PHASE_ICON[phase] : 'pi pi-wrench',
            text: step.message,
            tone: 'info'
        };
    }

    /**
     * The final report as bubbles: a scan header, a locate row, one row per fixed
     * rule (success) and per reported rule (warning), then a rescan footer with
     * the before/after counts.
     */
    resultMessages(report: FixReport): AgentMessage[] {
        const fixed = report.results.filter((r) => r.status === 'fixed-to-working');
        const reported = report.results.filter((r) => REPORTED_STATUSES.includes(r.status));

        return [
            {
                id: 'scan',
                icon: 'pi pi-search',
                text: this.dm.get('accessibility.studio.recipe.scan'),
                sub: this.dm.get(
                    'accessibility.studio.recipe.scan.sub',
                    String(report.scan.before.violations)
                ),
                tone: 'info'
            },
            {
                id: 'locate',
                icon: 'pi pi-sitemap',
                text: this.dm.get('accessibility.studio.recipe.locate'),
                tone: 'info'
            },
            ...fixed.map((r, i) => ({
                id: `fixed-${i}`,
                icon: 'pi pi-check',
                text: r.review ?? this.dm.get('accessibility.studio.recipe.fixed', r.ruleId),
                sub: this.ruleAndFile(r),
                tone: 'success' as const
            })),
            ...reported.map((r, i) => ({
                id: `reported-${i}`,
                // Distinct icon: reverted/regressed → undo, everything else → flag.
                icon: r.reverted || r.status === 'regressed' ? 'pi pi-replay' : 'pi pi-flag',
                text:
                    r.review ??
                    r.reason ??
                    this.dm.get('accessibility.studio.recipe.flagged', r.ruleId),
                sub: this.ruleAndFile(r),
                tone: 'warning' as const
            })),
            {
                id: 'rescan',
                icon: 'pi pi-verified',
                text: this.dm.get('accessibility.studio.recipe.rescan'),
                sub: this.dm.get(
                    'accessibility.studio.recipe.rescan.sub',
                    String(report.scan.before.violations),
                    String(report.scan.after.violations)
                ),
                tone: 'info'
            }
        ];
    }

    private ruleAndFile(r: FixResult): string {
        const file = r.file ? r.file.split('/').pop() : undefined;
        return file ? `${r.ruleId} · ${file}` : r.ruleId;
    }
}
