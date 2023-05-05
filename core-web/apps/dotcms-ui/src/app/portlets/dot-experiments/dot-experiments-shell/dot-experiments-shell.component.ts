import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';

@Component({
    selector: 'dot-experiments-shell',
    templateUrl: 'dot-experiments-shell.component.html',
    styleUrls: ['./dot-experiments-shell.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsShellComponent implements OnInit {
    constructor(private readonly router: Router) {}

    ngOnInit() {
        this.removeVariantQueryParams();
    }

    private removeVariantQueryParams() {
        this.router.navigate([], {
            queryParams: {
                mode: null,
                variantName: null,
                experimentId: null
            },
            queryParamsHandling: 'merge',
            replaceUrl: true
        });
    }
}
