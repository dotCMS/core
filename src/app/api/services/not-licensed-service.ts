import { Injectable } from '@angular/core';
import { CoreWebService, HttpCode } from 'dotcms-js';
import { DotRouterService } from './dot-router/dot-router.service';

@Injectable()
export class NotLicensedService {
    constructor(private coreWebService: CoreWebService, private router: DotRouterService) {}

    public init(): void {
        this.coreWebService
            .subscribeToHttpError(HttpCode.FORBIDDEN)
            .subscribe(() => this.router.goToNotLicensed());
    }
}
