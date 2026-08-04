import { MonacoEditorLoaderService } from '@materia-ui/ngx-monaco-editor';
import { byTestId, createComponentFactory, mockProvider, Spectator } from '@openng/spectator/jest';
import { of } from 'rxjs';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotA11yDiffViewerComponent } from './a11y-diff-viewer.component';

import { PageDiffFile } from '../models/page-render-sources.models';

const VTL_FILE: PageDiffFile = {
    identifier: 'vtl-1',
    path: '//demo/application/containers/awazon/a.vtl',
    name: 'a.vtl',
    extension: 'vtl',
    origin: 'container',
    working: 'new\ncode',
    live: 'old\ncode',
    added: 1,
    removed: 1
};

const CSS_FILE: PageDiffFile = {
    identifier: 'css-1',
    path: '//demo/application/themes/x/style.css',
    name: 'style.css',
    extension: 'css',
    origin: 'theme',
    working: '.a{color:red}',
    live: '',
    added: 1,
    removed: 0
};

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

describe('DotA11yDiffViewerComponent', () => {
    let spectator: Spectator<DotA11yDiffViewerComponent>;
    let monacoMock: ReturnType<typeof installMonacoMock>;

    const createComponent = createComponentFactory({
        component: DotA11yDiffViewerComponent,
        providers: [
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    'accessibility.studio.diff.live': 'Live',
                    'accessibility.studio.diff.working': 'Working',
                    'accessibility.studio.diff.backtopreview': 'Back to preview'
                })
            },
            mockProvider(MonacoEditorLoaderService, { isMonacoLoaded$: of(true) })
        ]
    });

    beforeEach(() => {
        jest.clearAllMocks();
        monacoMock = installMonacoMock();
    });

    it('builds the diff editor for the file it is given', () => {
        // Regression: the viewer mounts inside an @if in the run screen, so the
        // render effect first runs before the host element exists. It must re-run
        // once the host appears rather than silently bailing out — otherwise the
        // pane renders empty and the preview appears to stay up.
        spectator = createComponent({ props: { file: VTL_FILE } });
        spectator.detectChanges();

        expect(monacoMock.createDiffEditor).toHaveBeenCalledTimes(1);
        // live is the original (left), working the modified (right).
        expect(monacoMock.createModel).toHaveBeenCalledWith('old\ncode', 'html');
        expect(monacoMock.createModel).toHaveBeenCalledWith('new\ncode', 'html');
        expect(monacoMock.setModel).toHaveBeenCalled();
    });

    it('shows the file name and path in the header', () => {
        spectator = createComponent({ props: { file: VTL_FILE } });
        spectator.detectChanges();

        const header = spectator.query(byTestId('diff-viewer-close-btn'))?.parentElement;
        expect(header?.textContent).toContain('a.vtl');
        expect(header?.textContent).toContain('//demo/application/containers/awazon/a.vtl');
    });

    it('swaps the models when a different file comes in, reusing the editor', () => {
        spectator = createComponent({ props: { file: VTL_FILE } });
        spectator.detectChanges();
        monacoMock.createModel.mockClear();

        spectator.setInput('file', CSS_FILE);
        spectator.detectChanges();

        // Same editor instance, new models — css language, empty live side.
        expect(monacoMock.createDiffEditor).toHaveBeenCalledTimes(1);
        expect(monacoMock.createModel).toHaveBeenCalledWith('', 'css');
        expect(monacoMock.createModel).toHaveBeenCalledWith('.a{color:red}', 'css');
    });

    it('emits closed when the back control is used', () => {
        spectator = createComponent({ props: { file: VTL_FILE } });
        spectator.detectChanges();

        let closed = false;
        spectator.component.closed.subscribe(() => (closed = true));

        spectator.click(
            spectator
                .query(byTestId('diff-viewer-close-btn'))
                ?.querySelector('button') as HTMLElement
        );
        expect(closed).toBe(true);
    });

    it('builds nothing until a file is set', () => {
        spectator = createComponent({ props: { file: null } });
        spectator.detectChanges();
        expect(monacoMock.createDiffEditor).not.toHaveBeenCalled();
    });
});
