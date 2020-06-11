import { pluck, map, take } from 'rxjs/operators';
import { Injectable } from '@angular/core';
import { RequestMethod } from '@angular/http';
import { CoreWebService } from 'dotcms-js';
import { Observable, Subject, BehaviorSubject } from 'rxjs';

export interface DotUnlicensedPortletData {
    icon: string;
    titleKey: string;
    url: string;
}

const enterprisePorlets: DotUnlicensedPortletData[] = [
    {
        icon: 'tune',
        titleKey: 'com.dotcms.repackage.javax.portlet.title.rules',
        url: '/rules'
    },
    {
        icon: 'cloud_upload',
        titleKey: 'com.dotcms.repackage.javax.portlet.title.publishing-queue',
        url: '/c/publishing-queue'
    },
    {
        icon: 'find_in_page',
        titleKey: 'com.dotcms.repackage.javax.portlet.title.site-search',
        url: '/c/site-search'
    },
    {
        icon: 'update',
        titleKey: 'com.dotcms.repackage.javax.portlet.title.time-machine',
        url: '/c/time-machine'
    },
    {
        icon: 'device_hub',
        titleKey: 'com.dotcms.repackage.javax.portlet.title.workflow-schemes',
        url: '/c/workflow-schemes'
    },
    {
        icon: 'find_in_page',
        titleKey: 'com.dotcms.repackage.javax.portlet.title.es-search',
        url: '/c/es-search'
    },
    {
        icon: 'business',
        titleKey: 'Forms-and-Form-Builder',
        url: '/forms'
    }
];

/**
 * Handle license information of current logged in user
 * @export
 * @class DotLicenseService
 */
@Injectable()
export class DotLicenseService {
    unlicenseData: Subject<DotUnlicensedPortletData> = new BehaviorSubject({
        icon: '',
        titleKey: '',
        url: ''
    });
    private licenseURL: string;

    constructor(private coreWebService: CoreWebService) {
        this.licenseURL = 'v1/appconfiguration';
    }

    /**
     * Gets if current user has an enterprise license
     *
     * @returns Observable<boolean>
     * @memberof DotLicenseService
     */
    isEnterprise(): Observable<boolean> {
        return this.getLicense().pipe(map((license) => license['level'] >= 200));
    }

    /**
     * Verifies if an url is an enterprise portlet and user has enterprise
     *
     * @param string url
     * @returns Observable<boolean>
     * @memberof DotLicenseService
     */
    canAccessEnterprisePortlet(url: string): Observable<boolean> {
        return this.isEnterprise().pipe(
            take(1),
            map((isEnterpriseUser: boolean) => {
                const urlMatch = this.checksIfEnterpriseUrl(url);
                return urlMatch ? urlMatch && isEnterpriseUser : true;
            })
        );
    }

    private checksIfEnterpriseUrl(url: string): boolean {
        const urlMatch = enterprisePorlets.filter((item) => {
            return url.indexOf(item.url) === 0;
        });
        if (!!urlMatch.length) {
            this.unlicenseData.next(...urlMatch);
        }
        return !!urlMatch.length;
    }

    private getLicense(): Observable<any> {
        return this.coreWebService
            .requestView({
                method: RequestMethod.Get,
                url: this.licenseURL
            })
            .pipe(pluck('entity', 'config', 'license'));
    }
}
