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
    DotVelocityPlaygroundResponse,
    DotVelocityResponseContentType
} from '../../models/dot-velocity-playground.models';
import { DotVelocityPlaygroundService } from '../../services/dot-velocity-playground.service';

export const HISTORY_STORAGE_KEY = 'velocityPlayground';
export const SPLITTER_STORAGE_KEY = 'velocityPlayground.splitterRatio';
export const WRAP_STORAGE_KEY = 'velocityPlayground.wrap';

export const HISTORY_MAX_ENTRIES = 10;
export const DEFAULT_SPLITTER_RATIO: readonly [number, number] = [50, 50];
export const JSON_PRETTY_PRINT_MAX_BYTES = 512_000;

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

const readJson = <T>(key: string, fallback: T): T => {
    if (typeof window === 'undefined') return fallback;
    try {
        const raw = window.localStorage.getItem(key);
        if (raw == null) return fallback;
        return JSON.parse(raw) as T;
    } catch {
        return fallback;
    }
};

const writeJson = (key: string, value: unknown): void => {
    if (typeof window === 'undefined') return;
    try {
        window.localStorage.setItem(key, JSON.stringify(value));
    } catch {
        // Storage may be unavailable (quota, private mode) — ignore
    }
};

const removeKey = (key: string): void => {
    if (typeof window === 'undefined') return;
    try {
        window.localStorage.removeItem(key);
    } catch {
        // ignore
    }
};

const isValidHistory = (value: unknown): value is string[] =>
    Array.isArray(value) && value.every((entry) => typeof entry === 'string');

const isValidRatio = (value: unknown): value is [number, number] =>
    Array.isArray(value) &&
    value.length === 2 &&
    value.every((n) => typeof n === 'number' && Number.isFinite(n));

const dedupeAndCap = (history: string[], entry: string): string[] => {
    const trimmed = entry.trim();
    if (!trimmed) return history;
    const filtered = history.filter((item) => item !== trimmed);
    return [trimmed, ...filtered].slice(0, HISTORY_MAX_ENTRIES);
};

const formatBody = (body: string, contentType: DotVelocityResponseContentType): string => {
    if (contentType !== 'json' || !body.trim()) return body;
    // Skip the parse/stringify round-trip on very large payloads — Monaco
    // already struggles past ~500KB and the temporary copy doubles memory.
    if (body.length > JSON_PRETTY_PRINT_MAX_BYTES) return body;
    try {
        return JSON.stringify(JSON.parse(body), null, 2);
    } catch {
        return body;
    }
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
                                    patchState(store, {
                                        status: ComponentStatus.LOADED,
                                        errorMessage:
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
