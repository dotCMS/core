import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator
} from '@openng/spectator/jest';
import { of, throwError } from 'rxjs';

import { signal } from '@angular/core';

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

describe('DotA11yDiffComponent', () => {
    let spectator: Spectator<DotA11yDiffComponent>;

    let selectedPage: typeof MOCK_PAGE | null = MOCK_PAGE;
    /** Signal-backed so bumping it re-runs the component's reload effect. */
    const previewRevision = signal(0);

    const storeMock = {
        selected: () => selectedPage,
        previewRevision: () => previewRevision()
    };

    const createComponent = createComponentFactory({
        component: DotA11yDiffComponent,
        componentProviders: [{ provide: AccessibilityStudioStore, useValue: storeMock }],
        providers: [
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    'accessibility.studio.diff.fileschanged': 'Files changed',
                    'accessibility.studio.diff.empty.title': 'No files changed',
                    'accessibility.studio.diff.loading': 'Loading…',
                    'accessibility.studio.diff.working': 'Working',
                    'accessibility.studio.diff.live': 'Live'
                })
            }
        ]
    });

    beforeEach(() => {
        jest.clearAllMocks();
        selectedPage = MOCK_PAGE;
        previewRevision.set(0);
    });

    /** Render the accordion with the given diff files. */
    function render(diffFiles: PageDiffFile[] = DIFF_FILES) {
        spectator = createComponent({
            providers: [
                mockProvider(DotPageSourcesService, {
                    getPageSources: jest
                        .fn()
                        .mockReturnValue(of(diffFiles.map((f) => f as PageSourceFile))),
                    getDiffFiles: jest.fn().mockReturnValue(of(diffFiles))
                })
            ]
        });
        spectator.detectChanges();
    }

    it('loads the diff for the selected page on init — no scan required', () => {
        render();
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
        // The count badge itself lives on the run screen's panel header now.
        expect(rows[0].textContent).toContain('+1');
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

    it('emits the picked file so the run screen can diff it in the right pane', () => {
        render();
        const emitted: (PageDiffFile | null)[] = [];
        spectator.component.fileSelected.subscribe((f) => emitted.push(f));

        spectator.click(spectator.queryAll(byTestId('diff-file-row'))[0]);
        expect(emitted).toEqual([DIFF_FILES[0]]);
    });

    it('offers a way back to the preview once a file is open, and emits null', () => {
        render();
        const emitted: (PageDiffFile | null)[] = [];
        spectator.component.fileSelected.subscribe((f) => emitted.push(f));

        // No back control until a file is actually being diffed.
        expect(spectator.query(byTestId('diff-back-to-preview-btn'))).toBeFalsy();

        // The run screen owns the selection and feeds it back in.
        spectator.setInput('activeFileId', DIFF_FILES[0].identifier);
        spectator.detectChanges();

        spectator.click(spectator.query(byTestId('diff-back-to-preview-btn')) as HTMLElement);
        expect(emitted).toEqual([null]);
    });

    it('closes the right pane when a reload drops the file it was showing', () => {
        render();
        spectator.setInput('activeFileId', DIFF_FILES[0].identifier);
        spectator.detectChanges();

        const emitted: (PageDiffFile | null)[] = [];
        spectator.component.fileSelected.subscribe((f) => emitted.push(f));

        // A publish makes working == live, so the file leaves the list.
        spectator
            .inject(DotPageSourcesService)
            .getDiffFiles.mockReturnValue(of([DIFF_FILES[1]]));
        previewRevision.set(1);
        spectator.detectChanges();

        expect(emitted).toEqual([null]);
    });

    it('reports the changed-file count so the panel header can badge it', () => {
        const counts: number[] = [];
        spectator = createComponent({
            providers: [
                mockProvider(DotPageSourcesService, {
                    getPageSources: jest
                        .fn()
                        .mockReturnValue(of(DIFF_FILES as PageSourceFile[])),
                    getDiffFiles: jest.fn().mockReturnValue(of(DIFF_FILES))
                })
            ]
        });
        spectator.component.changedCount.subscribe((n) => counts.push(n));
        spectator.detectChanges();

        // Re-resolve on a revision bump so the count is re-reported.
        previewRevision.set(1);
        spectator.detectChanges();
        expect(counts).toContain(2);
    });

    it('shows the empty state when nothing changed', () => {
        render([]);
        expect(spectator.query(byTestId('diff-empty'))).toBeTruthy();
        expect(spectator.query(byTestId('diff-file-list'))).toBeFalsy();
    });

    it('reports zero changed files when the page has no working-vs-live delta', () => {
        const counts: number[] = [];
        spectator = createComponent({
            providers: [
                mockProvider(DotPageSourcesService, {
                    getPageSources: jest.fn().mockReturnValue(of([])),
                    getDiffFiles: jest.fn().mockReturnValue(of([]))
                })
            ]
        });
        spectator.component.changedCount.subscribe((n) => counts.push(n));
        previewRevision.set(1);
        spectator.detectChanges();

        expect(counts).toContain(0);
    });

    it('shows the error state when the diff load fails', () => {
        spectator = createComponent({
            providers: [
                mockProvider(DotPageSourcesService, {
                    getPageSources: jest.fn().mockReturnValue(of([])),
                    getDiffFiles: jest.fn().mockReturnValue(throwError(() => new Error('boom')))
                })
            ]
        });
        spectator.detectChanges();
        expect(spectator.query(byTestId('diff-error'))).toBeTruthy();
    });
});
