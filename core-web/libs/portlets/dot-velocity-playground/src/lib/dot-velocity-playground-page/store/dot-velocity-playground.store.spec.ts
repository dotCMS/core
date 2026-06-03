import { createServiceFactory, mockProvider, SpectatorService } from '@ngneat/spectator/jest';
import { of, throwError } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';

import { DotHttpErrorManagerService } from '@dotcms/data-access';
import { ComponentStatus } from '@dotcms/dotcms-models';

import { DotVelocityPlaygroundStore } from './dot-velocity-playground.store';

import {
    DEFAULT_SPLITTER_RATIO,
    HISTORY_MAX_ENTRIES,
    HISTORY_STORAGE_KEY,
    SPLITTER_STORAGE_KEY,
    WRAP_STORAGE_KEY
} from '../../dot-velocity-playground.utils';
import { DotVelocityPlaygroundResponse } from '../../models/dot-velocity-playground.models';
import { DotVelocityPlaygroundService } from '../../services/dot-velocity-playground.service';

const MOCK_RESPONSE: DotVelocityPlaygroundResponse = {
    body: 'hello',
    contentType: 'plaintext',
    elapsedMs: 42
};

describe('DotVelocityPlaygroundStore', () => {
    let spectator: SpectatorService<InstanceType<typeof DotVelocityPlaygroundStore>>;
    let runScriptSpy: jest.Mock;
    let errorHandler: { handle: jest.Mock };

    const createService = createServiceFactory({
        service: DotVelocityPlaygroundStore,
        providers: [
            mockProvider(DotVelocityPlaygroundService, {
                runScript: jest.fn().mockReturnValue(of(MOCK_RESPONSE))
            }),
            mockProvider(DotHttpErrorManagerService, { handle: jest.fn() })
        ]
    });

    beforeEach(() => {
        window.localStorage.clear();
        spectator = createService();
        spectator.flushEffects();
        runScriptSpy = spectator.inject(DotVelocityPlaygroundService).runScript as jest.Mock;
        runScriptSpy.mockClear();
        runScriptSpy.mockReturnValue(of(MOCK_RESPONSE));
        errorHandler = spectator.inject(DotHttpErrorManagerService) as unknown as {
            handle: jest.Mock;
        };
    });

    afterEach(() => {
        window.localStorage.clear();
    });

    describe('defaults', () => {
        it('starts in INIT status with empty output', () => {
            expect(spectator.service.status()).toBe(ComponentStatus.INIT);
            expect(spectator.service.output()).toBe('');
            expect(spectator.service.errorMessage()).toBeNull();
            expect(spectator.service.history()).toEqual([]);
            expect(spectator.service.splitterRatio()).toEqual([...DEFAULT_SPLITTER_RATIO]);
            expect(spectator.service.wrapCode()).toBe(true);
        });
    });

    describe('setters', () => {
        it('setCode updates the code state', () => {
            spectator.service.setCode('$foo');
            expect(spectator.service.code()).toBe('$foo');
        });

        it('setWrapCode persists to localStorage', () => {
            spectator.service.setWrapCode(false);
            expect(spectator.service.wrapCode()).toBe(false);
            expect(window.localStorage.getItem(WRAP_STORAGE_KEY)).toBe('false');
        });

        it('setSplitterRatio persists to localStorage', () => {
            spectator.service.setSplitterRatio([70, 30]);
            expect(spectator.service.splitterRatio()).toEqual([70, 30]);
            expect(JSON.parse(window.localStorage.getItem(SPLITTER_STORAGE_KEY) ?? '')).toEqual([
                70, 30
            ]);
        });

        it('setSplitterRatio ignores invalid ratios', () => {
            spectator.service.setSplitterRatio([NaN, 30] as [number, number]);
            expect(spectator.service.splitterRatio()).toEqual([...DEFAULT_SPLITTER_RATIO]);
        });
    });

    describe('history', () => {
        it('selectHistoryEntry sets the code when the entry exists', () => {
            runScriptSpy.mockReturnValue(of(MOCK_RESPONSE));
            spectator.service.setCode('$first');
            spectator.service.runScript();

            spectator.service.setCode('something else');
            spectator.service.selectHistoryEntry('$first');

            expect(spectator.service.code()).toBe('$first');
        });

        it('clearHistory empties state and removes the localStorage key', () => {
            spectator.service.setCode('$first');
            spectator.service.runScript();
            expect(spectator.service.history().length).toBeGreaterThan(0);

            spectator.service.clearHistory();

            expect(spectator.service.history()).toEqual([]);
            expect(window.localStorage.getItem(HISTORY_STORAGE_KEY)).toBeNull();
        });

        it('records successful runs into history (dedupe + cap are covered in utils.spec)', () => {
            for (let i = 0; i < HISTORY_MAX_ENTRIES + 2; i++) {
                spectator.service.setCode(`$snippet_${i}`);
                spectator.service.runScript();
            }

            expect(spectator.service.history().length).toBe(HISTORY_MAX_ENTRIES);
            expect(spectator.service.history()[0]).toBe(`$snippet_${HISTORY_MAX_ENTRIES + 1}`);
        });
    });

    describe('runScript', () => {
        it('wraps the script with the dotTimer prefix/suffix at submit time', () => {
            spectator.service.setCode('$hello');
            spectator.service.runScript();

            const submitted = runScriptSpy.mock.calls[0][0].velocity as string;
            expect(submitted.startsWith('#set($dotTimer = $date.date.time)\n')).toBe(true);
            expect(submitted.endsWith('$math.sub($date.date.time, $dotTimer)ms')).toBe(true);
            expect(submitted).toContain('$hello');
        });

        it('on success patches output, contentType, elapsedMs, and LOADED status, formatting JSON', () => {
            runScriptSpy.mockReturnValue(
                of({
                    body: '{"ok":true}',
                    contentType: 'json',
                    elapsedMs: 17
                } satisfies DotVelocityPlaygroundResponse)
            );

            spectator.service.setCode('$x');
            spectator.service.runScript();

            expect(spectator.service.status()).toBe(ComponentStatus.LOADED);
            // formatBody pretty-prints JSON; full coverage of the helper lives in utils.spec
            expect(spectator.service.output()).toBe('{\n  "ok": true\n}');
            expect(spectator.service.outputContentType()).toBe('json');
            expect(spectator.service.elapsedMs()).toBe(17);
        });

        it('pushes the un-wrapped code into history on success', () => {
            spectator.service.setCode('$hello');
            spectator.service.runScript();

            expect(spectator.service.history()[0]).toBe('$hello');
            expect(JSON.parse(window.localStorage.getItem(HISTORY_STORAGE_KEY) ?? '[]')).toEqual([
                '$hello'
            ]);
        });

        it('on error sets errorMessage, returns to LOADED, and delegates to the http error manager', () => {
            const httpError = new HttpErrorResponse({
                error: { message: 'broken' },
                status: 500,
                statusText: 'boom'
            });
            runScriptSpy.mockReturnValue(throwError(() => httpError));

            spectator.service.setCode('$broken');
            spectator.service.runScript();

            expect(spectator.service.status()).toBe(ComponentStatus.LOADED);
            expect(spectator.service.errorMessage()).toBe('broken');
            expect(errorHandler.handle).toHaveBeenCalledWith(httpError);
        });

        it('does not add to history when the call errors', () => {
            runScriptSpy.mockReturnValue(
                throwError(() => new HttpErrorResponse({ status: 500, statusText: 'boom' }))
            );

            spectator.service.setCode('$broken');
            spectator.service.runScript();

            expect(spectator.service.history()).toEqual([]);
        });
    });
});

describe('DotVelocityPlaygroundStore onInit', () => {
    const createService = createServiceFactory({
        service: DotVelocityPlaygroundStore,
        providers: [
            mockProvider(DotVelocityPlaygroundService, {
                runScript: jest.fn().mockReturnValue(of(MOCK_RESPONSE))
            }),
            mockProvider(DotHttpErrorManagerService, { handle: jest.fn() })
        ]
    });

    afterEach(() => {
        window.localStorage.clear();
    });

    it('seeds state from localStorage', () => {
        window.localStorage.setItem(HISTORY_STORAGE_KEY, JSON.stringify(['$a', '$b']));
        window.localStorage.setItem(SPLITTER_STORAGE_KEY, JSON.stringify([60, 40]));
        window.localStorage.setItem(WRAP_STORAGE_KEY, JSON.stringify(false));

        const spectator = createService();
        spectator.flushEffects();

        expect(spectator.service.history()).toEqual(['$a', '$b']);
        expect(spectator.service.splitterRatio()).toEqual([60, 40]);
        expect(spectator.service.wrapCode()).toBe(false);
        expect(spectator.service.code()).toBe('$a');
    });

    it('ignores malformed history payloads', () => {
        window.localStorage.setItem(HISTORY_STORAGE_KEY, '{"oops":true}');

        const spectator = createService();
        spectator.flushEffects();

        expect(spectator.service.history()).toEqual([]);
    });
});
