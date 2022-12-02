import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { DotIconModule } from '@dotcms/ui';
import { SkeletonModule } from 'primeng/skeleton';
import { CommonModule } from '@angular/common';
import { RouterLinkWithHref } from '@angular/router';

@Component({
    standalone: true,
    selector: 'dot-experiments-header',
    templateUrl: './dot-experiments-ui-header.component.html',
    styleUrls: ['./dot-experiments-ui-header.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [DotIconModule, SkeletonModule, CommonModule, RouterLinkWithHref]
})
export class DotExperimentsUiHeaderComponent {
    @Input()
    title = '';

    @Input()
    isLoading: boolean;

    @Output()
    goBack = new EventEmitter<void>();
}
