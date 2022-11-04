import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { DotIconModule } from '@dotcms/ui';
import { SkeletonModule } from 'primeng/skeleton';
import { CommonModule } from '@angular/common';
import { Router, RouterLinkWithHref } from '@angular/router';

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
    backUrl: string;

    @Input()
    isLoading: boolean;

    constructor(private readonly router: Router) {}

    goToTheRoute() {
        this.router.navigate([this.backUrl], { queryParamsHandling: 'preserve' });
    }
}
