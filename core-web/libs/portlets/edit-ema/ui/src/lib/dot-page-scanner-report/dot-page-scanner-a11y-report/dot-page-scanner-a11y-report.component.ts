import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { AccordionModule } from 'primeng/accordion';
import { CardModule } from 'primeng/card';
import { ChipModule } from 'primeng/chip';

import { DotColorIconComponent, DotMessagePipe } from '@dotcms/ui';

import { AxeRule, PageScannerA11yResponse } from '../dot-page-scanner.service';
import { A11yFindingType, A11yGroup } from '../models';

@Component({
    selector: 'dot-page-scanner-a11y-report',
    standalone: true,
    imports: [AccordionModule, CardModule, ChipModule, DotColorIconComponent, DotMessagePipe],
    templateUrl: './dot-page-scanner-a11y-report.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotPageScannerA11yReportComponent {
    a11yData = input.required<PageScannerA11yResponse>();

    protected a11yGroups = computed(() => this.buildA11yGroups(this.a11yData()));

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

    protected readonly accordionPt = {
        motion: {
            root: {
                style: {
                    overflow: 'hidden'
                }
            }
        }
    };

    private buildA11yGroups(data: PageScannerA11yResponse): A11yGroup[] {
        const axe = data.axe;

        return [
            ...this.mapRules(axe?.violations ?? [], 'error'),
            ...this.mapRules(axe?.incomplete ?? [], 'warning')
        ];
    }

    /**
     * Flatten raw axe rules into display groups. Each rule already groups the
     * elements it flagged in its `nodes` array, so one rule maps to one group.
     */
    private mapRules(rules: AxeRule[], type: A11yFindingType): A11yGroup[] {
        return rules.map((rule) => ({
            code: rule.id,
            type,
            message: rule.description ?? rule.help ?? '',
            impact: rule.impact ?? null,
            helpUrl: rule.helpUrl ?? '',
            items: (rule.nodes ?? []).map((node) => ({
                context: node.html,
                selector: node.target?.join(', ') ?? ''
            })),
            count: rule.nodes?.length ?? 0
        }));
    }
}
