import { Subject } from 'rxjs';

import { Component, OnDestroy, OnInit } from '@angular/core';

import { takeUntil } from 'rxjs/operators';

import { DotLicenseService, DotUnlicensedPortletData } from '@dotcms/data-access';

@Component({
    selector: 'dot-not-licensed-component',
    styleUrls: ['./not-licensed.component.scss'],
    templateUrl: 'not-licensed.component.html'
})
export class NotLicensedComponent implements OnInit, OnDestroy {
    unlicenseData: DotUnlicensedPortletData;

    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor(private dotLicense: DotLicenseService) {}

    ngOnInit() {
        this.dotLicense.unlicenseData
            .pipe(takeUntil(this.destroy$))
            .subscribe((unlicenseData: DotUnlicensedPortletData) => {
                this.unlicenseData = unlicenseData;
            });
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }
}
