import { forkJoin, Observable } from 'rxjs';

import { Injectable } from '@angular/core';
import { Resolve } from '@angular/router';

import { map, take } from 'rxjs/operators';

import { DotLicenseService } from '@dotcms/data-access';
import { DotCacheProviderListResolverData } from '@dotcms/dotcms-models';
import { DotCacheProvider } from '@dotcms/dotcms-models';
import { DotCacheService } from '@services/dot-cache/dot-cache.service';

@Injectable()
export class DotCacheListResolver implements Resolve<DotCacheProviderListResolverData> {
    constructor(
        public dotLicenseService: DotLicenseService,
        public dotCacheService: DotCacheService
    ) {}

    resolve(): Observable<DotCacheProviderListResolverData> {
        return forkJoin([
            this.dotLicenseService.isEnterprise(),
            this.dotCacheService.getAllProviders().pipe(
                map((providers: DotCacheProvider[]) => !!providers.length),
                take(1)
            )
        ]);
    }
}
