import { Component, OnInit, OnDestroy } from '@angular/core';
import {
    DotUnlicensedPortletData,
    DotLicenseService
} from '@services/dot-license/dot-license.service';
import { DotMessageService } from '@services/dot-messages-service';
import { take, takeUntil } from 'rxjs/operators';
import { Subject } from 'rxjs';

@Component({
    selector: 'dot-not-licensed-component',
    styleUrls: ['./not-licensed.component.scss'],
    templateUrl: 'not-licensed.component.html'
})
export class NotLicensedComponent implements OnInit, OnDestroy {
    messagesKey: { [key: string]: string } = {};
    unlicenseData: DotUnlicensedPortletData;

    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor(
        private dotMessageService: DotMessageService,
        private dotLicense: DotLicenseService
    ) {}

    ngOnInit() {
        this.dotLicense.unlicenseData
            .pipe(takeUntil(this.destroy$))
            .subscribe((unlicenseData: DotUnlicensedPortletData) => {
                this.unlicenseData = unlicenseData;
                this.dotMessageService
                    .getMessages([
                        this.unlicenseData.titleKey,
                        ...[
                            'request.a.trial.license',
                            'Contact-Us-for-more-Information',
                            'Learn-more-about-dotCMS-Enterprise',
                            'only-available-in-enterprise'
                        ]
                    ])
                    .pipe(take(1))
                    .subscribe((messages: { [key: string]: string }) => {
                        this.messagesKey = messages;
                    });
            });
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }
}
