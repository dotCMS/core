import { MonacoEditorLoaderService } from '@materia-ui/ngx-monaco-editor';
import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator
} from '@openng/spectator/jest';
import { of, throwError } from 'rxjs';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotA11yDiffComponent } from './a11y-diff.component';

import { PageDiffFile, PageSourceFile } from '../models/page-render-sources.models';
import { DotPageSourcesService } from '../services/dot-page-sources.service';
import { AccessibilityStudioStore } from '../store/accessibility-studio.store';

const MOCK_PAGE = {
    identifier: 'id-1',
    title: 'About Us',
    path: '/about-us',
    type: 'htmlpageasset',
    languageId: 1,
    hostId: 'host-1',
    hostName: 'demo.dotcms.com',
    modDate: '',
    modUserName: '',
    live: true
};

const DIFF_FILES: PageDiffFile[] = [
    {
        identifier: 'vtl-1',
        path: '//demo/application/containers/awazon/a.vtl',
        name: 'a.vtl',
        extension: 'vtl',
        origin: 'container',
        working: 'new\ncode',
        live: 'old\ncode',
        added: 1,
        removed: 1
    },
    {
        identifier: 'css-1',
        path: '//demo/application/themes/x/style.css',
        name: 'style.css',
        extension: 'css',
        origin: 'theme',
        working: '.a{color:red}',
        live: '',
        added: 1,
        removed: 0
    }
];

/** A minimal monaco diff-editor mock installed on the window global. */
function installMonacoMock() {
    const setModel = jest.fn();
    const dispose = jest.fn();
    const editor = {
        getModel: jest.fn().mockReturnValue(null),
        setModel,
        dispose
    };
    const createDiffEditor = jest.fn().mockReturnValue(editor);
    const createModel = jest.fn((value: string) => ({ value, dispose: jest.fn() }));
    (window as unknown as { monaco: unknown }).monaco = {
        editor: { createDiffEditor, createModel }
    };

    return { createDiffEditor, createModel, setModel };
}

/** Flush the queueMicrotask() the component uses to defer the Monaco render. */
async function flushMicrotasks(spectator: Spectator<DotA11yDiffComponent>) {
    await Promise.resolve();
    spectator.detectChanges();
}

describe('DotA11yDiffComponent', () => {
    let spectator: Spectator<DotA11yDiffComponent>;

    let selectedPage: typeof MOCK_PAGE | null = MOCK_PAGE;
    let monacoMock: ReturnType<typeof installMonacoMock>;

    const storeMock = {
        selected: () => selectedPage
    };

    const createComponent = createComponentFactory({
        component: DotA11yDiffComponent,
        componentProviders: [{ provide: AccessibilityStudioStore, useValue: storeMock }],
        providers: [
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    'accessibility.studio.diff.fileschanged': 'Files changed',
                    'accessibility.studio.diff.empty.title': 'No file changes',
                    'accessibility.studio.diff.loading': 'Loading…',
                    'accessibility.studio.diff.select': 'Select a file'
                })
            }
        ]
    });

    beforeEach(() => {
        jest.clearAllMocks();
        selectedPage = MOCK_PAGE;
        monacoMock = installMonacoMock();
    });

    /** Render the panel with the Code tab active (or not) and the given diff files. */
    function render(active = true, diffFiles: PageDiffFile[] = DIFF_FILES) {
        spectator = createComponent({
            props: { active },
            providers: [
                mockProvider(DotPageSourcesService, {
                    getPageSources: jest
                        .fn()
                        .mockReturnValue(of(diffFiles.map((f) => f as PageSourceFile))),
                    getDiffFiles: jest.fn().mockReturnValue(of(diffFiles))
                }),
                mockProvider(MonacoEditorLoaderService, {
                    isMonacoLoaded$: of(true)
                })
            ]
        });
        spectator.detectChanges();
    }

    it('loads nothing while inactive', () => {
        render(false);
        expect(spectator.inject(DotPageSourcesService).getPageSources).not.toHaveBeenCalled();
    });

    it('loads the diff for the selected page once active', () => {
        render(true);
        expect(spectator.inject(DotPageSourcesService).getPageSources).toHaveBeenCalledWith(
            '/about-us',
            'host-1',
            1
        );
    });

    it('lists only the changed files with add/remove counts', () => {
        render();
        const rows = spectator.queryAll(byTestId('diff-file-row'));
        expect(rows.length).toBe(2);
        expect(spectator.query(byTestId('diff-file-count'))).toHaveText('2');
    });

    it('shows each file name and its +/- line counts, but not the folder path', () => {
        render();
        const rows = spectator.queryAll(byTestId('diff-file-row'));
        expect(rows[0].textContent).toContain('a.vtl');
        expect(rows[0].textContent).toContain('+1');
        expect(rows[0].textContent).not.toContain('//demo/application/containers/');
        expect(rows[1].textContent).toContain('style.css');
        expect(rows[1].textContent).not.toContain('//demo/application/themes/');
    });

    it('renders the Monaco diff editor for the first file', async () => {
        render();
        await flushMicrotasks(spectator);
        // createDiffEditor is created once; models built from live (original) + working (modified).
        expect(monacoMock.createDiffEditor).toHaveBeenCalledTimes(1);
        expect(monacoMock.createModel).toHaveBeenCalledWith('old\ncode', 'html');
        expect(monacoMock.createModel).toHaveBeenCalledWith('new\ncode', 'html');
        expect(monacoMock.setModel).toHaveBeenCalled();
    });

    it('re-renders the diff models when a different file is selected', async () => {
        render();
        await flushMicrotasks(spectator);
        monacoMock.createModel.mockClear();

        const cssRow = spectator
            .queryAll(byTestId('diff-file-row'))
            .find((el) => el.textContent?.includes('style.css'));
        spectator.click(cssRow as HTMLElement);
        await flushMicrotasks(spectator);

        // CSS file → css language; live is empty string.
        expect(monacoMock.createModel).toHaveBeenCalledWith('', 'css');
        expect(monacoMock.createModel).toHaveBeenCalledWith('.a{color:red}', 'css');
    });

    it('shows the empty state when nothing changed', () => {
        render(true, []);
        expect(spectator.query(byTestId('diff-empty'))).toBeTruthy();
        expect(spectator.query(byTestId('diff-file-list'))).toBeFalsy();
    });

    it('shows the error state when the diff load fails', () => {
        spectator = createComponent({
            props: { active: true },
            providers: [
                mockProvider(DotPageSourcesService, {
                    getPageSources: jest.fn().mockReturnValue(of([])),
                    getDiffFiles: jest.fn().mockReturnValue(throwError(() => new Error('boom')))
                }),
                mockProvider(MonacoEditorLoaderService, { isMonacoLoaded$: of(true) })
            ]
        });
        spectator.detectChanges();
        expect(spectator.query(byTestId('diff-error'))).toBeTruthy();
    });
});
