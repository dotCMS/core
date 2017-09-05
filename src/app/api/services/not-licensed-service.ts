import { Injectable } from '@angular/core';
import { CoreWebService, HttpCode } from 'dotcms-js/dotcms-js';
import { DotRouterService } from './dot-router-service';

@Injectable()
export class NotLicensedService {
    constructor(
        private coreWebService: CoreWebService,
        private router: DotRouterService
    ) {}

    public init(): void {
        this.coreWebService
            .subscribeTo(HttpCode.FORBIDDEN)
            .subscribe(res => this.router.goToNotLicensed());
    }
}
