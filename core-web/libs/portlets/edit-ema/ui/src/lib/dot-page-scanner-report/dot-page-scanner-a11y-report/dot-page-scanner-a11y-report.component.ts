import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { AccordionModule } from 'primeng/accordion';
import { ChipModule } from 'primeng/chip';

import { DotMessagePipe } from '@dotcms/ui';

import { CHIP_STYLES } from '../chip-styles';
import { PageScannerA11yItem, PageScannerA11yResponse } from '../dot-page-scanner.service';
import { A11yGroup } from '../models';

@Component({
    selector: 'dot-page-scanner-a11y-report',
    standalone: true,
    imports: [AccordionModule, ChipModule, DotMessagePipe],
    templateUrl: './dot-page-scanner-a11y-report.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotPageScannerA11yReportComponent {
    a11yData = input.required<PageScannerA11yResponse>();

    protected a11yGroups = computed(() => this.buildA11yGroups(this.a11yData()));

    private getImpactChipStyle(impact: string): Record<string, string> {
        if (impact === 'critical' || impact === 'serious') return CHIP_STYLES.red;
        if (impact === 'moderate') return CHIP_STYLES.yellow;
        return CHIP_STYLES.blue;
    }

    private getTypeChipStyle(type: string): Record<string, string> {
        if (type === 'error') return CHIP_STYLES.red;
        if (type === 'warning') return CHIP_STYLES.yellow;
        return CHIP_STYLES.blue;
    }

    private buildA11yGroups(data: PageScannerA11yResponse): A11yGroup[] {
        const items: PageScannerA11yItem[] = data.findings?.items ?? data.issues ?? [];
        const map = new Map<string, A11yGroup>();

        for (const item of items) {
            if (map.has(item.code)) {
                map.get(item.code)!.items.push(item);
                map.get(item.code)!.count++;
            } else {
                const impact = item.runnerExtras?.impact ?? '';
                const type = item.type;
                map.set(item.code, {
                    code: item.code,
                    type,
                    impact,
                    helpUrl: item.runnerExtras?.helpUrl ?? '',
                    items: [item],
                    count: 1,
                    impactChipStyle: this.getImpactChipStyle(impact),
                    typeChipStyle: this.getTypeChipStyle(type)
                });
            }
        }

        return Array.from(map.values());
    }
}
