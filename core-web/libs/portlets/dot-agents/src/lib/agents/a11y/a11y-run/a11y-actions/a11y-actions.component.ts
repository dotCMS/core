import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { ToggleSwitchModule } from 'primeng/toggleswitch';

import { DotMessagePipe } from '@dotcms/ui';

import { StudioPhase } from '../../models/accessibility-studio.models';

/**
 * The scanner panel's footer: a line of copy about where the run stands, over the
 * one set of controls that phase allows (scan → stop → fix → stop → review files).
 *
 * Every control is an output — the run screen decides what they do, so this stays a
 * pure "what can I press right now" surface.
 */
@Component({
    selector: 'dot-a11y-actions',
    imports: [FormsModule, ButtonModule, ToggleSwitchModule, DotMessagePipe],
    templateUrl: './a11y-actions.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    // Scanner actions sit at the bottom of the panel's flex column and never grow.
    host: { class: 'flex-none px-4' }
})
export class DotA11yActionsComponent {
    /** Which controls to offer, and which footer copy to show. */
    readonly phase = input.required<StudioPhase>();

    /** Page path, interpolated into the footer sub-line. */
    readonly pagePath = input('');

    /** Open violations — the number the `scanned` footer offers to fix. */
    readonly openCount = input(0);

    /** Violations the run closed, for the post-run footer copy. */
    readonly fixedCount = input(0);

    /** Violations left for a human, for the post-run footer copy. */
    readonly reportedCount = input(0);

    /** Whether the agent should report CSS contrast issues instead of fixing them. */
    readonly skipCss = input(false);

    readonly scan = output<void>();
    readonly stopScan = output<void>();
    readonly fix = output<void>();
    readonly stopAgent = output<void>();
    /** "Review files" — the run screen opens the changed-files panel. */
    readonly reviewFiles = output<void>();
    /** "All pages" — back to the page list once published. */
    readonly allPages = output<void>();
    readonly skipCssChange = output<boolean>();

    /** Footer title + sub keys derived from the current phase — single switch. */
    protected readonly $footerKeys = computed(() => {
        const p = this.phase();
        const base = `accessibility.studio.footer.${p}`;
        return { titleKey: `${base}.title`, subKey: `${base}.sub` };
    });

    /** Interpolation args for the footer title, by phase. */
    protected readonly $footerArgs = computed<string[]>(() => {
        switch (this.phase()) {
            case 'scanned':
                return [this.openCount().toString()];
            case 'fixing':
            case 'done':
            case 'published':
                return [this.fixedCount().toString(), this.reportedCount().toString()];
            default:
                return [];
        }
    });

    /** Small leading icon + bubble color for the footer copy, by phase. */
    protected readonly $footerIcon = computed<{ icon: string; cls: string } | null>(() => {
        switch (this.phase()) {
            case 'scanned':
                return { icon: 'pi pi-sparkles', cls: 'bg-primary-50 text-primary' };
            case 'fixing':
                return { icon: 'pi pi-bolt', cls: 'bg-orange-50 text-orange-600' };
            default:
                return null;
        }
    });
}
