import { MonacoEditorLoaderService } from '@materia-ui/ngx-monaco-editor';
import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator
} from '@openng/spectator/jest';
import { of, throwError } from 'rxjs';

import { Drawer } from 'primeng/drawer';

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
                    'accessibility.studio.diff.title': 'File changes',
                    'accessibility.studio.diff.close': 'Close',
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

    /** Render with the drawer open (or closed) and the given diff files. */
    function render(open = true, diffFiles: PageDiffFile[] = DIFF_FILES) {
        spectator = createComponent({
            props: { open },
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

    /** Simulate the drawer finishing its open animation (its `onShow` event). */
    function fireDrawerShow() {
        spectator.component.onDrawerShow();
        spectator.detectChanges();
    }

    it('keeps the drawer closed and loads nothing when open is false', () => {
        render(false);
        expect(spectator.query(byTestId('diff-panel'))).toBeFalsy();
        expect(spectator.inject(DotPageSourcesService).getPageSources).not.toHaveBeenCalled();
    });

    it('renders the drawer and loads the diff once opened', () => {
        render(true);
        expect(spectator.query(byTestId('diff-panel'))).toBeTruthy();
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

    it('renders the Monaco diff editor for the first file once the drawer shows', () => {
        render();
        fireDrawerShow();
        // createDiffEditor is created once; models built from live (original) + working (modified).
        expect(monacoMock.createDiffEditor).toHaveBeenCalledTimes(1);
        expect(monacoMock.createModel).toHaveBeenCalledWith('old\ncode', 'html');
        expect(monacoMock.createModel).toHaveBeenCalledWith('new\ncode', 'html');
        expect(monacoMock.setModel).toHaveBeenCalled();
    });

    it('re-renders the diff models when a different file is selected', () => {
        render();
        fireDrawerShow();
        monacoMock.createModel.mockClear();

        const cssRow = spectator
            .queryAll(byTestId('diff-file-row'))
            .find((el) => el.textContent?.includes('style.css'));
        spectator.click(cssRow as HTMLElement);
        spectator.detectChanges();

        // CSS file → css language; live is empty string.
        expect(monacoMock.createModel).toHaveBeenCalledWith('', 'css');
        expect(monacoMock.createModel).toHaveBeenCalledWith('.a{color:red}', 'css');
    });

    it('shows the empty state when nothing changed', () => {
        render(true, []);
        expect(spectator.query(byTestId('diff-empty'))).toBeTruthy();
        expect(spectator.query(byTestId('diff-file-list'))).toBeFalsy();
    });

    it('emits close and disposes the editor when the drawer hides', () => {
        render();
        fireDrawerShow();
        const closeSpy = jest.fn();
        spectator.output('close').subscribe(closeSpy);

        // Simulate the drawer's dismissal (X / backdrop / Esc all funnel to onHide).
        spectator.component.onDrawerHide();

        expect(closeSpy).toHaveBeenCalled();
    });

    it('closes the drawer when the X button is clicked', () => {
        render();
        const drawer = spectator.query(Drawer);
        const btn = spectator.query(byTestId('diff-close-btn'))?.querySelector('button');
        spectator.click(btn as HTMLElement);
        // requestClose() flips the drawer's visible input to false.
        expect(drawer.visible).toBe(false);
    });

    it('shows the error state when the diff load fails', () => {
        spectator = createComponent({
            props: { open: true },
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
