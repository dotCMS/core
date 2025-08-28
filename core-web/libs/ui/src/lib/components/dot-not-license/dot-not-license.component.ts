import { Subject } from 'rxjs';

import { ChangeDetectionStrategy, Component, OnDestroy, OnInit, inject } from '@angular/core';

import { ButtonModule } from 'primeng/button';

import { takeUntil } from 'rxjs/operators';

import { DotLicenseService, DotUnlicensedPortletData } from '@dotcms/data-access';

import { DotMessagePipe } from '../../dot-message/dot-message.pipe';

@Component({
    selector: 'dot-not-license',
    imports: [ButtonModule, DotMessagePipe],
    templateUrl: './dot-not-license.component.html',
    styleUrl: './dot-not-license.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotNotLicenseComponent implements OnInit, OnDestroy {
    private dotLicense = inject(DotLicenseService);

    unlicenseData: DotUnlicensedPortletData;

    private destroy$: Subject<boolean> = new Subject<boolean>();

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
