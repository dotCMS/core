import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { ToggleSwitchModule } from 'primeng/toggleswitch';
import { TooltipModule } from 'primeng/tooltip';

import { DotMessagePipe, SafeUrlPipe } from '@dotcms/ui';

import { FixResult } from '../models/accessibility-studio.models';
import { AccessibilityStudioStore } from '../store/accessibility-studio.store';

/** A human-readable line in the Agent Recipe log. */
interface RecipeStep {
    icon: string;
    text: string;
    sub?: string;
    /** 'fixed' | 'reported' | 'info' — drives the bubble color. */
    tone: 'fixed' | 'reported' | 'info';
}

/**
 * The Studio run screen (§7): the agent column (score widget + recipe log +
 * state-driven action footer) beside a live preview pane.
 *
 * S3 is request/response with mock data — no SSE, no overlays, no before/after
 * split, no score animation. Those land in S4/S5. The score widget and recipe
 * log render the mock §6 report statically.
 */
@Component({
    selector: 'dot-accessibility-studio-run',
    standalone: true,
    imports: [
        FormsModule,
        ButtonModule,
        ToggleSwitchModule,
        TooltipModule,
        DotMessagePipe,
        SafeUrlPipe
    ],
    templateUrl: './dot-accessibility-studio-run.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'grid h-full min-h-0 grid-cols-[412px_1fr]' }
})
export class DotAccessibilityStudioRunComponent {
    readonly store = inject(AccessibilityStudioStore);

    /** Score ring geometry — circumference of r=54 circle. */
    private readonly RING_CIRCUMFERENCE = 339.292;

    /** The number shown in the ring center: open violations, or a dash pre-scan. */
    readonly centerCount = computed<string | number>(() => {
        if (this.store.isScanning()) {
            return '–';
        }
        if (!this.store.scanned()) {
            return '–';
        }
        const report = this.store.report();
        if (!report) {
            return '–';
        }
        // While "scanned" (pre-fix) show the before count; after a fix show after.
        return this.store.isScanned()
            ? report.scan.before.violations
            : report.scan.after.violations;
    });

    /** Ring fill 0→1 based on issues cleared (before − current) / before. */
    readonly ringProgress = computed(() => {
        if (!this.store.isDone() && !this.store.isPublished()) {
            return 0;
        }
        const before = this.store.beforeCount();
        const cleared = before - this.store.afterCount();
        return before > 0 ? Math.min(1, Math.max(0, cleared / before)) : 0;
    });

    readonly ringOffset = computed(
        () => this.RING_CIRCUMFERENCE * (1 - this.ringProgress())
    );

    readonly ringClass = computed(() => {
        if (!this.store.scanned()) {
            return 'stroke-surface-300';
        }
        return this.ringProgress() >= 1 ? 'stroke-green-500' : 'stroke-primary';
    });

    /** The Agent Recipe step log — derived from the report, rendered statically. */
    readonly recipeSteps = computed<RecipeStep[]>(() => {
        if (!this.store.isDone() && !this.store.isPublished()) {
            return [];
        }
        const report = this.store.report();
        if (!report) {
            return [];
        }

        const steps: RecipeStep[] = [
            {
                icon: 'pi pi-search',
                text: 'Scanned page against WCAG 2.2 AA',
                sub: `${report.scan.before.violations} issues found`,
                tone: 'info'
            },
            {
                icon: 'pi pi-sitemap',
                text: 'Located source templates & containers',
                tone: 'info'
            }
        ];

        for (const r of this.store.fixedResults()) {
            steps.push({
                icon: 'pi pi-check',
                text: r.review ?? `Fixed ${r.ruleId}`,
                sub: this.ruleAndFile(r),
                tone: 'fixed'
            });
        }

        for (const r of this.store.reportedResults()) {
            steps.push({
                icon: 'pi pi-flag',
                text: r.review ?? r.reason ?? `Flagged ${r.ruleId}`,
                sub: this.ruleAndFile(r),
                tone: 'reported'
            });
        }

        steps.push({
            icon: 'pi pi-verified',
            text: 'Re-scanned working copy — fixes confirmed',
            sub: `${report.scan.before.violations} → ${report.scan.after.violations} violations`,
            tone: 'info'
        });

        return steps;
    });

    /** Footer headline copy by phase. */
    readonly footerTitleKey = computed(() => {
        switch (this.store.phase()) {
            case 'ready':
                return 'accessibility.studio.footer.ready.title';
            case 'scanning':
                return 'accessibility.studio.footer.scanning.title';
            case 'scanned':
                return 'accessibility.studio.footer.scanned.title';
            case 'fixing':
                return 'accessibility.studio.footer.fixing.title';
            case 'done':
                return 'accessibility.studio.footer.done.title';
            case 'published':
                return 'accessibility.studio.footer.published.title';
            default:
                return '';
        }
    });

    readonly footerSubKey = computed(() => {
        switch (this.store.phase()) {
            case 'ready':
                return 'accessibility.studio.footer.ready.sub';
            case 'scanning':
                return 'accessibility.studio.footer.scanning.sub';
            case 'scanned':
                return 'accessibility.studio.footer.scanned.sub';
            case 'fixing':
                return 'accessibility.studio.footer.fixing.sub';
            case 'done':
                return 'accessibility.studio.footer.done.sub';
            case 'published':
                return 'accessibility.studio.footer.published.sub';
            default:
                return '';
        }
    });

    /**
     * Preview URL for the iframe — the page rendered in EDIT_MODE (§8.2).
     * Prefixed with the `/dot-page` sentinel so it loads same-origin: in dev the
     * proxy strips the prefix and forwards to the BE page renderer (see
     * apps/dotcms-ui/proxy-dev.conf.mjs); in prod the portlet is served from the
     * BE origin so the proxy rewrite is equivalent. `host_id` disambiguates which
     * site's copy renders; EDIT_MODE is the working-version render the agent re-scans.
     */
    readonly previewUrl = computed(() => {
        const page = this.store.selected();
        if (!page) {
            return '';
        }
        const path = page.path.startsWith('/') ? page.path : `/${page.path}`;
        return `/dot-page${path}?host_id=${page.hostId}&language_id=${page.languageId}&mode=EDIT_MODE`;
    });

    backToPicker(): void {
        this.store.backToPicker();
    }

    runScan(): void {
        this.store.runScan();
    }

    startFix(): void {
        this.store.startFix();
    }

    publish(): void {
        this.store.publish();
    }

    discard(): void {
        this.store.discard();
    }

    onSkipCssChange(value: boolean): void {
        this.store.setSkipCss(value);
    }

    private ruleAndFile(r: FixResult): string {
        const file = r.file ? r.file.split('/').pop() : undefined;
        return file ? `${r.ruleId} · ${file}` : r.ruleId;
    }
}
