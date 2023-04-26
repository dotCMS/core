import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';

// TODO: make more generic
@Component({
    standalone: true,
    selector: 'dot-experiments-empty-experiments',
    templateUrl: './dot-experiments-empty-experiments.component.html',
    styleUrls: ['./dot-experiments-empty-experiments.component.scss'],
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
