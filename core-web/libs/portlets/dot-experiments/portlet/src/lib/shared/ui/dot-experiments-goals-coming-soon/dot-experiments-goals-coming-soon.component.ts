import { ChangeDetectionStrategy, Component } from '@angular/core';

import { CardModule } from 'primeng/card';

import { DotMessagePipe } from '@dotcms/ui';

@Component({
    selector: 'dot-experiments-goals-coming-soon',
    imports: [CardModule, DotMessagePipe],
    templateUrl: './dot-experiments-goals-coming-soon.component.html',
    styleUrls: ['./dot-experiments-goals-coming-soon.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsGoalsComingSoonComponent {}
