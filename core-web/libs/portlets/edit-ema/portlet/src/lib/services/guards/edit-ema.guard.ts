// eslint-disable-next-line @nx/enforce-module-boundaries
import { DEFAULT_PERSONA } from 'libs/portlets/edit-ema/portlet/src/lib/shared/consts';

import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivateFn, Params, Router } from '@angular/router';

import { map } from 'rxjs/operators';

import { EmaAppConfigurationService } from '@dotcms/data-access';

type EmaQueryParams = {
    url: string;
    language_id: number;
    'com.dotmarketing.persona.id': string;
};

export const editEmaGuard: CanActivateFn = (route: ActivatedRouteSnapshot) => {
    const [content] = route.firstChild.url;

    const router = inject(Router);

    const { didQueryParamsGetCompleted, newQueryParams } = confirmQueryParams(route.queryParams);

    const url = didQueryParamsGetCompleted ? newQueryParams.url : route.queryParams.url;

    return inject(EmaAppConfigurationService)
        .get(url)
        .pipe(
            map((value) => {
                if (value) {
                    if (didQueryParamsGetCompleted) {
                        router.navigate([`/edit-ema/${content.path}`], {
                            queryParams: {
                                ...route.queryParams,
                                ...newQueryParams
                            },
                            replaceUrl: true
                        });

                        return true;
                    }

                    return true;
                }

                return router.createUrlTree(['/pages']);
            })
        );
};

function confirmQueryParams(queryParams: Params): {
    newQueryParams: EmaQueryParams | null;
    didQueryParamsGetCompleted: boolean;
} {
    const { missing, ...missingQueryParams } = DEFAULT_QUERY_PARAMS.reduce(
        (acc, curr) => {
            if (!queryParams[curr.key]) {
                acc[curr.key] = curr.value;
                acc.missing = true;
            }

            return acc;
        },
        {
            missing: false
        }
    );

    if (missing) {
        return {
            didQueryParamsGetCompleted: true,
            newQueryParams: missingQueryParams as EmaQueryParams
        };
    }

    return {
        didQueryParamsGetCompleted: false,
        newQueryParams: null
    };
}

const DEFAULT_QUERY_PARAMS = [
    {
        key: 'language_id',
        value: 1
    },
    {
        key: 'url',
        value: 'index'
    },
    {
        key: 'com.dotmarketing.persona.id',
        value: DEFAULT_PERSONA.identifier
    }
];
