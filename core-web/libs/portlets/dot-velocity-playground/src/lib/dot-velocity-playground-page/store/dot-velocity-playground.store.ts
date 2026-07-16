import { tapResponse } from '@ngrx/operators';
import {
    patchState,
    signalStore,
    withComputed,
    withHooks,
    withMethods,
    withState
} from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { pipe } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { computed, inject } from '@angular/core';

import { switchMap, tap } from 'rxjs/operators';

import { DotHttpErrorManagerService } from '@dotcms/data-access';
import { ComponentStatus } from '@dotcms/dotcms-models';

import {
    DEFAULT_SPLITTER_RATIO,
    dedupeAndCap,
    formatBody,
    HISTORY_STORAGE_KEY,
    isValidHistory,
    isValidRatio,
    readJson,
    removeKey,
    SPLITTER_STORAGE_KEY,
    WRAP_STORAGE_KEY,
    writeJson
} from '../../dot-velocity-playground.utils';
import {
    DotVelocityPlaygroundResponse,
    DotVelocityResponseContentType
} from '../../models/dot-velocity-playground.models';
import { DotVelocityPlaygroundService } from '../../services/dot-velocity-playground.service';

const TIMER_PREFIX = '#set($dotTimer = $date.date.time)\n';
const TIMER_SUFFIX = '\n--\n$math.sub($date.date.time, $dotTimer)ms';

export interface VelocityPlaygroundState {
    code: string;
    wrapCode: boolean;
    splitterRatio: [number, number];
    history: string[];
    status: ComponentStatus;
    output: string;
    outputContentType: DotVelocityResponseContentType;
    elapsedMs: number | null;
    errorMessage: string | null;
}

const initialState: VelocityPlaygroundState = {
    code: '',
    wrapCode: true,
    splitterRatio: [...DEFAULT_SPLITTER_RATIO] as [number, number],
    history: [],
    status: ComponentStatus.INIT,
    output: '',
    outputContentType: 'plaintext',
    elapsedMs: null,
    errorMessage: null
};

export const DotVelocityPlaygroundStore = signalStore(
    withState<VelocityPlaygroundState>(initialState),
    withComputed((store) => ({
        isLoading: computed(() => store.status() === ComponentStatus.LOADING),
        hasOutput: computed(
            () => store.status() === ComponentStatus.LOADED && store.output().length > 0
        ),
        hasError: computed(() => store.errorMessage() !== null),
        canRun: computed(
            () => store.code().trim().length > 0 && store.status() !== ComponentStatus.LOADING
        ),
        hasHistory: computed(() => store.history().length > 0)
    })),
    withMethods(
        (
            store,
            service = inject(DotVelocityPlaygroundService),
            httpErrorManager = inject(DotHttpErrorManagerService)
        ) => ({
            setCode(code: string): void {
                patchState(store, { code });
            },

            setWrapCode(wrapCode: boolean): void {
                patchState(store, { wrapCode });
                writeJson(WRAP_STORAGE_KEY, wrapCode);
            },

            setSplitterRatio(ratio: [number, number]): void {
                if (!isValidRatio(ratio)) return;
                patchState(store, { splitterRatio: ratio });
                writeJson(SPLITTER_STORAGE_KEY, ratio);
            },

            selectHistoryEntry(entry: string): void {
                if (!store.history().includes(entry)) return;
                patchState(store, { code: entry });
            },

            clearHistory(): void {
                patchState(store, { history: [] });
                removeKey(HISTORY_STORAGE_KEY);
            },

            runScript: rxMethod<void>(
                pipe(
                    tap(() =>
                        patchState(store, {
                            status: ComponentStatus.LOADING,
                            output: '',
                            errorMessage: null,
                            elapsedMs: null
                        })
                    ),
                    switchMap(() => {
                        const originalCode = store.code();
                        const wrapped = `${TIMER_PREFIX}${originalCode}${TIMER_SUFFIX}`;

                        return service.runScript({ velocity: wrapped }).pipe(
                            tapResponse({
                                next: (response: DotVelocityPlaygroundResponse) => {
                                    const nextHistory = dedupeAndCap(store.history(), originalCode);
                                    writeJson(HISTORY_STORAGE_KEY, nextHistory);
                                    patchState(store, {
                                        status: ComponentStatus.LOADED,
                                        output: formatBody(response.body, response.contentType),
                                        outputContentType: response.contentType,
                                        elapsedMs: response.elapsedMs,
                                        history: nextHistory
                                    });
                                },
                                error: (error: HttpErrorResponse) => {
                                    // responseType: 'text' → error.error holds the raw VTL error body.
                                    const serverBody =
                                        typeof error?.error === 'string' && error.error.trim()
                                            ? error.error
                                            : null;
                                    patchState(store, {
                                        status: ComponentStatus.LOADED,
                                        errorMessage:
                                            serverBody ??
                                            error?.error?.message ??
                                            error?.message ??
                                            'velocityPlayground.error.unknown'
                                    });
                                    httpErrorManager.handle(error);
                                }
                            })
                        );
                    })
                )
            )
        })
    ),
    withHooks({
        onInit(store) {
            const history = readJson<string[]>(HISTORY_STORAGE_KEY, []);
            const splitterRatio = readJson<[number, number]>(SPLITTER_STORAGE_KEY, [
                ...DEFAULT_SPLITTER_RATIO
            ] as [number, number]);
            const wrapCode = readJson<boolean>(WRAP_STORAGE_KEY, true);

            const sanitizedHistory = isValidHistory(history) ? history : [];
            const sanitizedRatio = isValidRatio(splitterRatio)
                ? splitterRatio
                : ([...DEFAULT_SPLITTER_RATIO] as [number, number]);

            patchState(store, {
                history: sanitizedHistory,
                splitterRatio: sanitizedRatio,
                wrapCode: typeof wrapCode === 'boolean' ? wrapCode : true,
                code: sanitizedHistory[0] ?? ''
            });
        }
    })
);
