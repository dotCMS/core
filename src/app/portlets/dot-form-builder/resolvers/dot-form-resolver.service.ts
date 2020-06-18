import { Observable, of } from 'rxjs';
import { Injectable } from '@angular/core';
import { Resolve, ActivatedRouteSnapshot } from '@angular/router';
import { take, switchMap } from 'rxjs/operators';
import { DotLicenseService } from '@services/dot-license/dot-license.service';
import { DotMessageService } from '@services/dot-message/dot-messages.service';

export interface DotUnlicensedPortlet {
    title: string;
    description: string;
    links: {
        text: string;
        link: string;
    }[];
}

/**
 * Check is there is license to view the form portlet
 *
 * @export
 * @class DotFormResolver
 * @implements {(Resolve<DotCMSPortletFallback>)}
 */
@Injectable()
export class DotFormResolver implements Resolve<DotUnlicensedPortlet> {
    constructor(
        private dotLicenseService: DotLicenseService,
        private dotMessageService: DotMessageService
    ) {}

    resolve(_route: ActivatedRouteSnapshot): Observable<DotUnlicensedPortlet> {
        return this.dotLicenseService
            .isEnterprise()
            .pipe(
                switchMap(
                    (isEnterprise: boolean) =>
                        isEnterprise ? of(null) : this.getUnlicensePortlet()
                ),
                take(1)
            );
    }

    private getUnlicensePortlet(): Observable<DotUnlicensedPortlet> {
        return of({
            title: this.dotMessageService.get('Forms-and-Form-Builder'),
            description: this.dotMessageService.get('Forms-and-Form-Builder-in-Enterprise'),
            links: [
                {
                    text: this.dotMessageService.get('Learn-more-about-dotCMS-Enterprise'),
                    link: 'https://dotcms.com/product/features/feature-list'
                },
                {
                    text: this.dotMessageService.get('Contact-Us-for-more-Information'),
                    link: 'https://dotcms.com/contact-us/'
                }
            ]
        });
    }
}
