import { Subject } from 'rxjs';

import { ChangeDetectionStrategy, Component, OnDestroy, OnInit } from '@angular/core';

import { ButtonModule } from 'primeng/button';

import { takeUntil } from 'rxjs/operators';

import { DotLicenseService, DotUnlicensedPortletData } from '@dotcms/data-access';

import { DotMessagePipe } from '../../dot-message/dot-message.pipe';

@Component({
    selector: 'dot-not-license',
    standalone: true,
    imports: [ButtonModule, DotMessagePipe],
    templateUrl: './dot-not-license.component.html',
    styleUrl: './dot-not-license.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotNotLicenseComponent implements OnInit, OnDestroy {
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
