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

import {
    DotHttpErrorManagerService,
    readJson,
    removeKey,
    withPersistedQuery,
    writeJson
} from '@dotcms/data-access';
import { ComponentStatus } from '@dotcms/dotcms-models';

import {
    DEFAULT_SPLITTER_RATIO,
    dedupeAndCap,
    formatBody,
    HISTORY_STORAGE_KEY,
    isValidHistory,
    isValidRatio,
    parseVelocityError,
    SPLITTER_STORAGE_KEY,
    WRAP_STORAGE_KEY
} from '../../dot-velocity-playground.utils';
import {
    DotVelocityPlaygroundError,
    DotVelocityPlaygroundResponse,
    DotVelocityResponseContentType,
    VelocityWarning
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
    error: DotVelocityPlaygroundError | null;
    warnings: VelocityWarning[];
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
    error: null,
    warnings: []
};

export const DotVelocityPlaygroundStore = signalStore(
    withState<VelocityPlaygroundState>(initialState),
    withPersistedQuery({ portletKey: 'velocity-playground', field: 'code' }),
    withComputed((store) => ({
        isLoading: computed(() => store.status() === ComponentStatus.LOADING),
        hasOutput: computed(
            () => store.status() === ComponentStatus.LOADED && store.output().length > 0
        ),
        hasError: computed(() => store.error() !== null),
        hasWarnings: computed(() => store.warnings().length > 0),
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
                            error: null,
                            warnings: [],
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
                                        warnings: response.warnings,
                                        history: nextHistory
                                    });
                                },
                                error: (error: HttpErrorResponse) => {
                                    // The backend returns a structured 400
                                    // ({ errors: [...], warnings: [...] }) for Velocity
                                    // parse/runtime errors. Keep those inline; only fall back to
                                    // the global (modal) handler for infrastructure failures
                                    // (network, 403 license, 500…).
                                    const { error: parsed, isVelocityError } =
                                        parseVelocityError(error);
                                    patchState(store, {
                                        status: ComponentStatus.LOADED,
                                        error: parsed,
                                        warnings: parsed.warnings
                                    });
                                    if (!isVelocityError) {
                                        httpErrorManager.handle(error);
                                    }
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
                wrapCode: typeof wrapCode === 'boolean' ? wrapCode : true
            });
        }
    })
);
