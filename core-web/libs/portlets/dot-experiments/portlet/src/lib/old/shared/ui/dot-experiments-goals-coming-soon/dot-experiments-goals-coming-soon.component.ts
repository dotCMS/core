import { ChangeDetectionStrategy, Component } from '@angular/core';

import { CardModule } from 'primeng/card';

import { DotMessagePipe } from '@dotcms/ui';

interface ComingSoonCard {
    titleKey: string;
    descriptionKey: string;
    icon: string;
}

@Component({
    selector: 'dot-experiments-goals-coming-soon',
    imports: [CardModule, DotMessagePipe],
    templateUrl: './dot-experiments-goals-coming-soon.component.html',
    host: {
        class: 'bg-[var(--gray-100)] rounded-sm p-3 flex flex-col items-center gap-4'
    },
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsGoalsComingSoonComponent {
    readonly cards: ComingSoonCard[] = [
        {
            titleKey: 'experiments.configure.coming.soon.time.page',
            descriptionKey: 'experiments.configure.coming.soon.time.page.description',
            icon: 'pi pi-clock'
        },
        {
            titleKey: 'experiments.configure.coming.soon.number.pages',
            descriptionKey: 'experiments.configure.coming.soon.number.pages.description',
            icon: 'pi pi-check-square'
        },
        {
            titleKey: 'experiments.configure.coming.soon.rule.based',
            descriptionKey: 'experiments.configure.coming.soon.rule.based.description',
            icon: 'pi pi-chart-line'
        },
        {
            titleKey: 'experiments.configure.coming.soon.javascript',
            descriptionKey: 'experiments.configure.coming.soon.javascript.description',
            icon: 'pi pi-code'
        }
    ];
}
