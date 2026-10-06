import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { AccordionModule } from 'primeng/accordion';
import { CardModule } from 'primeng/card';
import { ChipModule } from 'primeng/chip';
import { AccordionPassThrough } from 'primeng/types/accordion';

import { DotColorIconComponent, DotMessagePipe } from '@dotcms/ui';

import { buildA11yGroups } from '../a11y-groups';
import { PageScannerA11yResponse } from '../dot-page-scanner.service';

@Component({
    selector: 'dot-page-scanner-a11y-report',
    imports: [AccordionModule, CardModule, ChipModule, DotColorIconComponent, DotMessagePipe],
    templateUrl: './dot-page-scanner-a11y-report.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotPageScannerA11yReportComponent {
    a11yData = input.required<PageScannerA11yResponse>();

    protected a11yGroups = computed(() => buildA11yGroups(this.a11yData()));

    /** One accordion group per axe rule that flagged at least one element. */
    protected errorCount = computed(() =>
        this.a11yGroups()
            .filter((group) => group.type === 'error')
            .reduce((total, group) => total + group.count, 0)
    );

    /** Elements axe could not conclusively check (its `incomplete` results). */
    protected warningCount = computed(() =>
        this.a11yGroups()
            .filter((group) => group.type === 'warning')
            .reduce((total, group) => total + group.count, 0)
    );

    /**
     * PrimeNG types `pt.motion` as `MotionOptions` (animation config), which has no way to
     * express the wrapper attributes below — but the runtime spreads this straight into the
     * motion options, and the collapse fix documented above was verified against it. The value
     * is therefore left byte-for-byte as it was; only the cast is new.
     *
     * TODO(#37120): confirm with PrimeNG whether `pt.motion` still honours `root`, then drop
     * either the cast or the workaround.
     */
    protected readonly accordionPt = {
        motion: {
            root: {
                style: {
                    overflow: 'hidden'
                }
            }
        }
    } as unknown as AccordionPassThrough;
}
