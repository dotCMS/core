import { byTestId, createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';

import { DotHttpErrorManagerService, DotMessageService } from '@dotcms/data-access';
import { ComponentStatus } from '@dotcms/dotcms-models';

import { DotVelocityPlaygroundPageComponent } from './dot-velocity-playground-page.component';
import { DotVelocityPlaygroundStore } from './store/dot-velocity-playground.store';

import { DotVelocityPlaygroundService } from '../services/dot-velocity-playground.service';

type StoreOverrides = Partial<Record<string, jest.Mock>>;

const buildStoreMock = (overrides: StoreOverrides = {}) => ({
    code: jest.fn().mockReturnValue(''),
    wrapCode: jest.fn().mockReturnValue(true),
    splitterRatio: jest.fn().mockReturnValue([50, 50]),
    history: jest.fn().mockReturnValue([]),
    status: jest.fn().mockReturnValue(ComponentStatus.INIT),
    output: jest.fn().mockReturnValue(''),
    outputContentType: jest.fn().mockReturnValue('plaintext'),
    elapsedMs: jest.fn().mockReturnValue(null),
    errorMessage: jest.fn().mockReturnValue(null),
    isLoading: jest.fn().mockReturnValue(false),
    hasOutput: jest.fn().mockReturnValue(false),
    hasError: jest.fn().mockReturnValue(false),
    canRun: jest.fn().mockReturnValue(false),
    hasHistory: jest.fn().mockReturnValue(false),
    setCode: jest.fn(),
    setWrapCode: jest.fn(),
    setSplitterRatio: jest.fn(),
    selectHistoryEntry: jest.fn(),
    clearHistory: jest.fn(),
    runScript: jest.fn(),
    ...overrides
});

describe('DotVelocityPlaygroundPageComponent', () => {
    let spectator: Spectator<DotVelocityPlaygroundPageComponent>;
    let pendingStoreOverrides: StoreOverrides = {};

    const createComponent = createComponentFactory({
        component: DotVelocityPlaygroundPageComponent,
        overrideComponents: [
            [
                DotVelocityPlaygroundPageComponent,
                {
                    remove: { providers: [DotVelocityPlaygroundStore] }
                }
            ]
        ],
        providers: [
            mockProvider(DotMessageService, { get: jest.fn().mockReturnValue('') }),
            mockProvider(DotHttpErrorManagerService),
            mockProvider(DotVelocityPlaygroundService)
        ],
        componentProviders: [
            {
                provide: DotVelocityPlaygroundStore,
                useFactory: () => buildStoreMock(pendingStoreOverrides)
            }
        ]
    });

    const setup = (storeOverrides: StoreOverrides = {}) => {
        pendingStoreOverrides = storeOverrides;
        spectator = createComponent();
        return spectator.inject(DotVelocityPlaygroundStore, true);
    };

    afterEach(() => {
        pendingStoreOverrides = {};
    });

    it('creates the component', () => {
        setup();
        expect(spectator.component).toBeTruthy();
    });

    describe('run button', () => {
        it('is a no-op when canRun is false', () => {
            const store = setup({ canRun: jest.fn().mockReturnValue(false) });
            spectator.component.onRun();
            expect(store.runScript).not.toHaveBeenCalled();
        });

        it('triggers runScript when canRun is true', () => {
            const store = setup({
                code: jest.fn().mockReturnValue('$x'),
                canRun: jest.fn().mockReturnValue(true)
            });
            const btn = spectator
                .query(byTestId('velocity-playground-run-btn'))
                ?.querySelector('button');
            expect(btn).toBeTruthy();
            if (btn) spectator.click(btn);
            expect(store.runScript).toHaveBeenCalled();
        });
    });

    describe('history select', () => {
        it('forwards selection to store.selectHistoryEntry', () => {
            const store = setup({
                history: jest.fn().mockReturnValue(['$one', '$two']),
                hasHistory: jest.fn().mockReturnValue(true)
            });
            spectator.component.onHistoryChange('$two');
            expect(store.selectHistoryEntry).toHaveBeenCalledWith('$two');
        });

        it('ignores null values from the select clear', () => {
            const store = setup({ hasHistory: jest.fn().mockReturnValue(true) });
            spectator.component.onHistoryChange(null);
            expect(store.selectHistoryEntry).not.toHaveBeenCalled();
        });
    });

    describe('splitter resize', () => {
        it('persists the new ratio when both values are numeric', () => {
            const store = setup();
            spectator.component.onSplitterResize({ sizes: [70, 30] });
            expect(store.setSplitterRatio).toHaveBeenCalledWith([70, 30]);
        });
    });

    describe('editor options computed signal', () => {
        it('exposes velocity language and wrap=on when wrapCode is true', () => {
            setup({ wrapCode: jest.fn().mockReturnValue(true) });
            expect(spectator.component.$editorOptions().language).toBe('velocity');
            expect(spectator.component.$editorOptions().wordWrap).toBe('on');
        });

        it('flips wordWrap to off when wrapCode is false', () => {
            setup({ wrapCode: jest.fn().mockReturnValue(false) });
            expect(spectator.component.$editorOptions().wordWrap).toBe('off');
        });
    });

    describe('output options computed signal', () => {
        it('mirrors outputContentType into the Monaco language', () => {
            setup({ outputContentType: jest.fn().mockReturnValue('json') });
            expect(spectator.component.$outputOptions().language).toBe('json');
            expect(spectator.component.$outputOptions().readOnly).toBe(true);
        });
    });

    describe('error banner', () => {
        it('renders when hasError is true', () => {
            setup({
                hasError: jest.fn().mockReturnValue(true),
                errorMessage: jest.fn().mockReturnValue('Velocity failed'),
                status: jest.fn().mockReturnValue(ComponentStatus.LOADED)
            });
            expect(spectator.query(byTestId('velocity-playground-error-banner'))).toBeTruthy();
        });

        it('is hidden when hasError is false', () => {
            setup({ hasError: jest.fn().mockReturnValue(false) });
            expect(spectator.query(byTestId('velocity-playground-error-banner'))).toBeFalsy();
        });
    });

    describe('content-type chip', () => {
        it('uppercases the content type when output is present', () => {
            setup({
                hasOutput: jest.fn().mockReturnValue(true),
                outputContentType: jest.fn().mockReturnValue('json'),
                status: jest.fn().mockReturnValue(ComponentStatus.LOADED)
            });
            const chip = spectator.query(byTestId('velocity-playground-content-type-chip'));
            expect(chip?.textContent).toContain('JSON');
        });
    });
});
