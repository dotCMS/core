import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { RouterLink } from '@angular/router';

import { SkeletonModule } from 'primeng/skeleton';

import { DotIconModule } from '@dotcms/ui';

@Component({
    standalone: true,
    selector: 'dot-experiments-header',
    templateUrl: './dot-experiments-ui-header.component.html',
    styleUrls: ['./dot-experiments-ui-header.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [DotIconModule, SkeletonModule, CommonModule, RouterLink]
})
export class DotExperimentsUiHeaderComponent {
    @Input()
    title = '';

    @Input()
    isLoading: boolean;

    @Output()
    goBack = new EventEmitter<boolean>();
}
