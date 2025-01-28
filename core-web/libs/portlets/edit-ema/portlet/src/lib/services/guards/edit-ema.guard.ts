import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivateFn, Params, Router } from '@angular/router';

import { DEFAULT_PERSONA } from '../../shared/consts';

type EmaQueryParams = {
    url: string;
    language_id: number;
    'com.dotmarketing.persona.id': string;
    variantName: string;
};

export const editEmaGuard: CanActivateFn = (route: ActivatedRouteSnapshot) => {
    const [content] = route.firstChild.url;

    const router = inject(Router);

    const { didQueryParamsGetCompleted, newQueryParams } = confirmQueryParams(route.queryParams);

    if (didQueryParamsGetCompleted) {
        router.navigate([`/edit-page/${content.path}`], {
            queryParams: {
                ...route.queryParams,
                ...newQueryParams
            },
            replaceUrl: true
        });
    }

    return true;
};

function confirmQueryParams(queryParams: Params): {
    newQueryParams: EmaQueryParams | null;
    didQueryParamsGetCompleted: boolean;
} {
    const { missing, ...missingQueryParams } = DEFAULT_QUERY_PARAMS.reduce(
        (acc, { key, value }) => {
            if (!queryParams[key]) {
                acc[key] = value;
                acc.missing = true;

                return acc;
            }

            if (key === 'url' && queryParams[key] === '/') {
                acc[key] = 'index';
                acc.missing = true;

                return acc;
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
