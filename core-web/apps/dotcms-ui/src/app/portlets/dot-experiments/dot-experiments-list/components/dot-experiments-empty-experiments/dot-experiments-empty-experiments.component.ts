import { NgIf } from '@angular/common';
import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';

import { ButtonModule } from 'primeng/button';

import { DotIconModule, DotMessagePipeModule } from '@dotcms/ui';

// TODO: make more generic
@Component({
    standalone: true,
    selector: 'dot-experiments-empty-experiments',
    templateUrl: './dot-experiments-empty-experiments.component.html',
    styleUrls: ['./dot-experiments-empty-experiments.component.scss'],
    imports: [NgIf, ButtonModule, DotIconModule, DotMessagePipeModule],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsEmptyExperimentsComponent {
    @Input()
    description: string;

    @Input()
    showButton = true;

    @Output()
    addNew = new EventEmitter<void>();
}
