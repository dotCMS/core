import { MonacoEditorLoaderService } from '@materia-ui/ngx-monaco-editor';
import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator
} from '@openng/spectator/jest';
import { of, throwError } from 'rxjs';

import { signal } from '@angular/core';
import { ActivatedRoute, Router, UrlSegment } from '@angular/router';

import { DotMessageService } from '@dotcms/data-access';
import { GlobalStore } from '@dotcms/store';
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
        folder: '//demo/application/containers/awazon/',
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
        folder: '//demo/application/themes/x/',
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

    const navigate = jest.fn();
    const openPageByUri = jest.fn();
    const currentSiteIdSignal = signal<string | null>('site-1');

    let pathSegments = ['about-us', 'diff'];
    let selectedPage: typeof MOCK_PAGE | null = MOCK_PAGE;
    let rehydrateStatus: 'idle' | 'loading' | 'not-found' = 'idle';
    let monacoMock: ReturnType<typeof installMonacoMock>;

    const storeMock = {
        selected: () => selectedPage,
        rehydrateStatus: () => rehydrateStatus,
        openPageByUri
    };

    const createComponent = createComponentFactory({
        component: DotA11yDiffComponent,
        componentProviders: [{ provide: AccessibilityStudioStore, useValue: storeMock }],
        providers: [
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    'accessibility.studio.diff.title': 'File changes',
                    'accessibility.studio.diff.fileschanged': 'Files changed',
                    'accessibility.studio.diff.empty.title': 'No file changes',
                    'accessibility.studio.diff.loading': 'Loading…',
                    'accessibility.studio.diff.select': 'Select a file'
                })
            },
            { provide: Router, useValue: { navigate } },
            {
                provide: GlobalStore,
                useValue: {
                    get currentSiteId() {
                        return currentSiteIdSignal;
                    }
                }
            },
            {
                provide: ActivatedRoute,
                useFactory: () => ({
                    url: of(pathSegments.map((p) => new UrlSegment(p, {})))
                })
            }
        ]
    });

    beforeEach(() => {
        jest.clearAllMocks();
        pathSegments = ['about-us', 'diff'];
        selectedPage = MOCK_PAGE;
        rehydrateStatus = 'idle';
        currentSiteIdSignal.set('site-1');
        monacoMock = installMonacoMock();
    });

    function render(diffFiles: PageDiffFile[] = DIFF_FILES) {
        spectator = createComponent({
            providers: [
                mockProvider(DotPageSourcesService, {
                    getPageSources: jest
                        .fn()
                        .mockReturnValue(
                            of(diffFiles.map((f) => f as PageSourceFile))
                        ),
                    getDiffFiles: jest.fn().mockReturnValue(of(diffFiles))
                }),
                mockProvider(MonacoEditorLoaderService, {
                    isMonacoLoaded$: of(true)
                })
            ]
        });
        spectator.detectChanges();
    }

    it('rehydrates the selected page from the route (dropping the /diff marker)', () => {
        render();
        expect(openPageByUri).toHaveBeenCalledWith('/about-us');
    });

    it('lists only the changed files with add/remove counts', () => {
        render();
        const rows = spectator.queryAll(byTestId('diff-file-row'));
        expect(rows.length).toBe(2);
        expect(spectator.query(byTestId('diff-file-count'))).toHaveText('2');
    });

    it('shows each file name with its containing folder path', () => {
        render();
        const rows = spectator.queryAll(byTestId('diff-file-row'));
        // First row: name + full folder path (not the origin label).
        expect(rows[0].textContent).toContain('a.vtl');
        expect(rows[0].textContent).toContain('//demo/application/containers/awazon/');
        expect(rows[1].textContent).toContain('//demo/application/themes/x/');
    });

    it('selects the first file by default and renders a Monaco diff editor', () => {
        render();
        // createDiffEditor is created once; models built from live (original) + working (modified).
        expect(monacoMock.createDiffEditor).toHaveBeenCalledTimes(1);
        expect(monacoMock.createModel).toHaveBeenCalledWith('old\ncode', 'html');
        expect(monacoMock.createModel).toHaveBeenCalledWith('new\ncode', 'html');
        expect(monacoMock.setModel).toHaveBeenCalled();
    });

    it('re-renders the diff models when a different file is selected', () => {
        render();
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
        render([]);
        expect(spectator.query(byTestId('diff-empty'))).toBeTruthy();
        expect(spectator.query(byTestId('diff-file-list'))).toBeFalsy();
    });

    it('navigates back to the run route on back', () => {
        render();
        const btn = spectator.query(byTestId('diff-back-btn'))?.querySelector('button');
        spectator.click(btn as HTMLElement);
        expect(navigate).toHaveBeenCalledWith(['/agents/a11y', 'about-us']);
    });

    it('bounces to the picker when the page cannot be rehydrated', () => {
        rehydrateStatus = 'not-found';
        render();
        expect(navigate).toHaveBeenCalledWith(['/agents/a11y']);
    });

    it('shows the error state when the diff load fails', () => {
        spectator = createComponent({
            providers: [
                mockProvider(DotPageSourcesService, {
                    getPageSources: jest.fn().mockReturnValue(of([])),
                    getDiffFiles: jest
                        .fn()
                        .mockReturnValue(throwError(() => new Error('boom')))
                }),
                mockProvider(MonacoEditorLoaderService, { isMonacoLoaded$: of(true) })
            ]
        });
        spectator.detectChanges();
        expect(spectator.query(byTestId('diff-error'))).toBeTruthy();
    });
});
