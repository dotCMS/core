import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component } from '@angular/core';

import { CardModule } from 'primeng/card';

import { DotMessagePipeModule } from '@dotcms/ui';

@Component({
    selector: 'dot-experiments-goals-coming-soon',
    standalone: true,
    imports: [CommonModule, CardModule, DotMessagePipeModule],
    templateUrl: './dot-experiments-goals-coming-soon.component.html',
    styleUrls: ['./dot-experiments-goals-coming-soon.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsGoalsComingSoonComponent {}
