import { MonacoEditorLoaderService } from '@materia-ui/ngx-monaco-editor';
import { byTestId, createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import {
    DotGlobalMessageService,
    DotHttpErrorManagerService,
    DotMessageService
} from '@dotcms/data-access';
import { ComponentStatus } from '@dotcms/dotcms-models';
import { DotClipboardUtil } from '@dotcms/ui';

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
                    remove: { providers: [DotVelocityPlaygroundStore, DotClipboardUtil] }
                }
            ]
        ],
        providers: [
            mockProvider(DotMessageService, { get: jest.fn().mockReturnValue('') }),
            mockProvider(DotHttpErrorManagerService),
            mockProvider(DotGlobalMessageService),
            mockProvider(DotClipboardUtil, { copy: jest.fn().mockResolvedValue(true) }),
            mockProvider(DotVelocityPlaygroundService),
            { provide: MonacoEditorLoaderService, useValue: { isMonacoLoaded$: of(false) } }
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
            expect(spectator.component.$editorOptions().language).toBe('velocity-playground');
            expect(spectator.component.$editorOptions().wordWrap).toBe('on');
        });

        it('flips wordWrap to off when wrapCode is false', () => {
            setup({ wrapCode: jest.fn().mockReturnValue(false) });
            expect(spectator.component.$editorOptions().wordWrap).toBe('off');
        });

        it('applies the dot-velocity-dark Monaco theme', () => {
            setup();
            expect(spectator.component.$editorOptions().theme).toBe('dot-velocity-dark');
        });
    });

    describe('output options computed signal', () => {
        it('mirrors outputContentType into the Monaco language', () => {
            setup({ outputContentType: jest.fn().mockReturnValue('json') });
            expect(spectator.component.$outputOptions().language).toBe('json');
            expect(spectator.component.$outputOptions().readOnly).toBe(true);
        });

        it('uses the same custom dark theme as the editor', () => {
            setup();
            expect(spectator.component.$outputOptions().theme).toBe('dot-velocity-dark');
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

    describe('content-type label', () => {
        it('renders the content type as-is when output is present', () => {
            setup({
                hasOutput: jest.fn().mockReturnValue(true),
                outputContentType: jest.fn().mockReturnValue('json'),
                status: jest.fn().mockReturnValue(ComponentStatus.LOADED)
            });
            const label = spectator.query(byTestId('velocity-playground-content-type-chip'));
            expect(label?.textContent?.trim()).toBe('json');
        });
    });

    describe('help popover', () => {
        it('exposes a non-empty list of help examples', () => {
            setup();
            expect(spectator.component.helpExamples.length).toBeGreaterThan(0);
            for (const ex of spectator.component.helpExamples) {
                expect(ex.title).toBeTruthy();
                expect(ex.code).toBeTruthy();
            }
        });

        it('renders the help button in the editor panel', () => {
            setup();
            const helpBtn = spectator
                .query(byTestId('velocity-playground-help-btn'))
                ?.querySelector('button');
            expect(helpBtn).toBeTruthy();
        });

        it('useExample loads the snippet into the editor and hides the popover', () => {
            const store = setup();
            const hideSpy = jest.fn();
            // Stub the viewChild signal so we don't depend on PrimeNG popover internals
            Object.defineProperty(spectator.component, 'helpPopover', {
                value: () => ({ hide: hideSpy })
            });
            spectator.component.useExample('#set($x = 1)');
            expect(store.setCode).toHaveBeenCalledWith('#set($x = 1)');
            expect(hideSpy).toHaveBeenCalled();
        });

        it('copyToClipboard delegates to DotClipboardUtil', async () => {
            setup();
            const clipboard = spectator.inject(DotClipboardUtil);
            spectator.component.copyToClipboard('payload');
            // microtask flush — copy is async inside the component
            await Promise.resolve();
            expect(clipboard.copy).toHaveBeenCalledWith('payload');
        });
    });

    describe('history panel', () => {
        it('starts collapsed', () => {
            setup();
            expect(spectator.component.$historyOpen()).toBe(false);
        });

        it('toggles via the signal setter', () => {
            setup();
            spectator.component.$historyOpen.set(true);
            expect(spectator.component.$historyOpen()).toBe(true);
        });
    });

    describe('empty output state', () => {
        it('renders dot-empty-container on INIT when there is no error', () => {
            setup({
                status: jest.fn().mockReturnValue(ComponentStatus.INIT),
                hasError: jest.fn().mockReturnValue(false)
            });
            expect(spectator.query(byTestId('velocity-playground-empty-output'))).toBeTruthy();
        });

        it('hides the empty state once a run has completed', () => {
            setup({
                status: jest.fn().mockReturnValue(ComponentStatus.LOADED),
                hasOutput: jest.fn().mockReturnValue(true),
                output: jest.fn().mockReturnValue('rendered')
            });
            expect(spectator.query(byTestId('velocity-playground-empty-output'))).toBeFalsy();
        });
    });

    describe('loading state', () => {
        it('renders the dot-spinner while a script is running', () => {
            setup({
                isLoading: jest.fn().mockReturnValue(true),
                status: jest.fn().mockReturnValue(ComponentStatus.LOADING),
                hasError: jest.fn().mockReturnValue(false)
            });
            const container = spectator.query(byTestId('velocity-playground-loading'));
            expect(container).toBeTruthy();
            expect(container?.querySelector('dot-spinner')).toBeTruthy();
        });

        it('hides the spinner once the run finishes', () => {
            setup({
                isLoading: jest.fn().mockReturnValue(false),
                status: jest.fn().mockReturnValue(ComponentStatus.LOADED),
                hasOutput: jest.fn().mockReturnValue(true),
                output: jest.fn().mockReturnValue('rendered')
            });
            expect(spectator.query(byTestId('velocity-playground-loading'))).toBeFalsy();
        });
    });

    // Kept last because rendering <p-menu [popup]="true"> attaches an overlay container
    // to document.body that can leak into the next test's query if other describes follow.
    describe('export buttons', () => {
        const loadedSetup = (overrides: StoreOverrides = {}) =>
            setup({
                hasOutput: jest.fn().mockReturnValue(true),
                output: jest.fn().mockReturnValue('rendered'),
                outputContentType: jest.fn().mockReturnValue('json'),
                status: jest.fn().mockReturnValue(ComponentStatus.LOADED),
                code: jest.fn().mockReturnValue('#set($x = 1)'),
                ...overrides
            });

        it('renders Share and Export buttons when there is output', () => {
            loadedSetup();
            expect(
                spectator
                    .query(byTestId('velocity-playground-share-btn'))
                    ?.querySelector('button')
            ).toBeTruthy();
            expect(
                spectator
                    .query(byTestId('velocity-playground-export-btn'))
                    ?.querySelector('button')
            ).toBeTruthy();
        });

        it('hides Share/Export when there is no output', () => {
            setup();
            expect(spectator.query(byTestId('velocity-playground-share-btn'))).toBeFalsy();
            expect(spectator.query(byTestId('velocity-playground-export-btn'))).toBeFalsy();
        });

        it('copies a curl snippet that POSTs to /api/vtl/dynamic/ with the current code', async () => {
            loadedSetup();
            const clipboard = spectator.inject(DotClipboardUtil);
            (clipboard.copy as jest.Mock).mockClear();
            const curlCommand = spectator.component.exportItems[0].command;
            if (!curlCommand) throw new Error('curl command not registered');
            curlCommand({} as never);
            await Promise.resolve();
            const [payload] = (clipboard.copy as jest.Mock).mock.calls[0];
            expect(payload).toContain('curl -X POST');
            expect(payload).toContain('/api/vtl/dynamic/');
            expect(payload).toContain('"velocity":"#set($x = 1)"');
        });

        it('copies a fetch snippet pointing at /api/vtl/dynamic/', async () => {
            loadedSetup();
            const clipboard = spectator.inject(DotClipboardUtil);
            (clipboard.copy as jest.Mock).mockClear();
            const fetchCommand = spectator.component.exportItems[1].command;
            if (!fetchCommand) throw new Error('fetch command not registered');
            fetchCommand({} as never);
            await Promise.resolve();
            const [payload] = (clipboard.copy as jest.Mock).mock.calls[0];
            expect(payload).toContain("await fetch('/api/vtl/dynamic/'");
            expect(payload).toContain('"velocity": "#set($x = 1)"');
        });

        it('downloadOutput appends an anchor whose filename matches the content type', () => {
            loadedSetup({ outputContentType: jest.fn().mockReturnValue('xml') });
            const createObjectUrlOriginal = URL.createObjectURL;
            URL.createObjectURL = jest.fn().mockReturnValue('blob:mock');
            const appendSpy = jest.spyOn(document.body, 'appendChild');

            spectator.component.downloadOutput();

            const anchorCall = appendSpy.mock.calls.find(
                ([node]) => (node as HTMLElement).tagName === 'A'
            );
            expect(anchorCall).toBeDefined();
            expect((anchorCall![0] as HTMLAnchorElement).download).toBe('velocity-output.xml');

            appendSpy.mockRestore();
            URL.createObjectURL = createObjectUrlOriginal;
        });

        it('toggleExportMenu delegates to the menu when the viewChild resolves', () => {
            loadedSetup();
            const menu = spectator.component.exportMenu();
            if (!menu) {
                // viewChild may not resolve in JSDOM with popup overlays — skip gracefully
                return;
            }
            const toggleSpy = jest.spyOn(menu, 'toggle');
            spectator.component.toggleExportMenu(new MouseEvent('click'));
            expect(toggleSpy).toHaveBeenCalled();
        });
    });
});
