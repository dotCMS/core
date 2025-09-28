import {
    patchState,
    signalStore,
    withComputed,
    withHooks,
    withMethods,
    withState
} from '@ngrx/signals';

import { DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute } from '@angular/router';

import { UVE_MODE } from '@dotcms/types';

import { PERSONA_KEY, UVE_STATUS, UVEConfiguration, UVEState } from './model';

import { getConfiguration } from '../utils';

const initialConfiguration: UVEConfiguration = {
    url: '',
    device: '',
    publishDate: '',
    language_id: '1',
    mode: UVE_MODE.EDIT,
    [PERSONA_KEY]: ''
};

const initialState: UVEState = {
    languages: [],
    isEnterprise: false,
    editorStatus: UVE_STATUS.LOADING,
    configuration: initialConfiguration
};

/**
 * Store for the UVE state
 */
export const UVEStore = signalStore(
    {
        providedIn: 'root'
    },
    withState<UVEState>(initialState),
    withComputed(() => ({})),
    withMethods((store) => {
        return {
            setUveStatus(editorStatus: UVE_STATUS) {
                patchState(store, { editorStatus });
            }
        };
    }),
    withHooks((store) => {
        const activatedRoute = inject(ActivatedRoute);
        const destroyRef = inject(DestroyRef);

        return {
            onInit: () => {
                activatedRoute.queryParams
                    .pipe(takeUntilDestroyed(destroyRef))
                    .subscribe((queryParams) => {
                        const configuration = getConfiguration(queryParams);
                        patchState(store, { configuration });
                    });
            }
        };
    })
);
