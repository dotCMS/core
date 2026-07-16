import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { AccordionModule } from 'primeng/accordion';
import { CardModule } from 'primeng/card';
import { ChipModule } from 'primeng/chip';

import { DotColorIconComponent, DotMessagePipe } from '@dotcms/ui';

import { PageScannerA11yItem, PageScannerA11yResponse } from '../dot-page-scanner.service';
import { A11yGroup } from '../models';

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
        const items: PageScannerA11yItem[] = data.findings?.items ?? data.issues ?? [];
        const map = new Map<string, A11yGroup>();

        for (const item of items) {
            if (map.has(item.code)) {
                const existingGroup = map.get(item.code);

                if (!existingGroup) {
                    continue;
                }

                existingGroup.items.push(item);
                existingGroup.count++;
            } else {
                const impact = item.runnerExtras?.impact ?? '';
                const type = item.type;
                map.set(item.code, {
                    message: item.runnerExtras?.description ?? '',
                    code: item.code,
                    type,
                    impact,
                    helpUrl: item.runnerExtras?.helpUrl ?? '',
                    items: [item],
                    count: 1
                });
            }
        }

        return Array.from(map.values());
    }
}
