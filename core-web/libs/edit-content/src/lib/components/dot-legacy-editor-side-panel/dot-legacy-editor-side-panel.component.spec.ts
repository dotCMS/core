import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator
} from '@openng/spectator/vitest';
import { of, Subject, throwError } from 'rxjs';
import { Mock, vi } from 'vitest';

import { NgZone, signal } from '@angular/core';

import { ConfirmationService, ConfirmEventType, Confirmation } from 'primeng/api';

import {
    DotContentletService,
    DotEventsService,
    DotHttpErrorManagerService,
    DotIframeService,
    DotMessageService,
    DotUiColorsService,
    DotWorkflowEventHandlerService
} from '@dotcms/data-access';
import { DotPushPublishDialogService } from '@dotcms/dotcms-js';
import { DotCMSContentlet, DotFunctionInfo } from '@dotcms/dotcms-models';
import { DotSidePanelRef } from '@dotcms/ui';
import { createFakeContentlet } from '@dotcms/utils-testing';

import { DotLegacyEditorSidePanelComponent } from './dot-legacy-editor-side-panel.component';

import { DotEditContentRequest } from '../../models/dot-edit-content-request.model';

/** The legacy editor's edit screen, as UVE and the full-page wrapper load it. */
const LAYOUT_PATH = '/c/portal/layout';

/** What `/api/v1/portlet/_actionurl/<type>` answers: the legacy create screen for that type. */
const CREATE_URL =
    '/c/portal/layout?p_p_id=content&_content_cmd=new&selectedStructure=banner-id&lang=2';

/**
 * Backs `DotIframeService.ran()`: the calls other admin code asks the legacy editor to run, such as
 * the workflow wizard's answer or a Bring Back. Re-created per test.
 */
let ran$ = new Subject<DotFunctionInfo>();

const CREATE_REQUEST: DotEditContentRequest = {
    mode: 'new',
    contentType: 'Banner',
    folderInode: 'f-1',
    languageId: 2,
    title: 'Banner',
    portletId: 'content-drive'
};

const EDIT_REQUEST: DotEditContentRequest = {
    mode: 'edit',
    inode: 'abc',
    identifier: 'id-1',
    contentType: 'Banner',
    languageId: 1,
    title: 'My legacy content',
    portletId: 'content-drive'
};

/** What the server answers for the saved inode. */
const SAVED: DotCMSContentlet = createFakeContentlet({
    inode: 'abc2',
    identifier: 'id-1',
    languageId: 1,
    title: 'My legacy content'
});

/**
 * A stand-in for the legacy editor's window. The test DOM never loads the JSP, so the panel would
 * otherwise find an empty frame. A real `Document` keeps `addEventListener`/`dispatchEvent` honest,
 * so the panel's listeners are exercised exactly as the JSP's `document.dispatchEvent` reaches them.
 */
interface StubFrame {
    doc: Document;
    win: Window & { ngEditContentletEvents?: { next: (detail: unknown) => void } };
}

/** Keeps the test DOM from fetching the JSP for every rendered iframe. */
const disableIframePageLoading = (): void => {
    const happyDOM = (window as unknown as { happyDOM?: { settings: Record<string, unknown> } })
        .happyDOM;

    if (happyDOM) {
        happyDOM.settings['disableIframePageLoading'] = true;
    }
};

describe('DotLegacyEditorSidePanelComponent', () => {
    beforeAll(disableIframePageLoading);

    let spectator: Spectator<DotLegacyEditorSidePanelComponent>;
    /** The panel's ref, reduced to what the content uses. */
    let panel: {
        close: Mock;
        contentStyleClass: ReturnType<typeof signal<string>>;
        closed: Subject<void>;
    };

    const createComponent = createComponentFactory({
        component: DotLegacyEditorSidePanelComponent,
        providers: [
            mockProvider(DotUiColorsService, { setColors: vi.fn() }),
            mockProvider(DotContentletService, {
                getContentletByInode: vi.fn().mockReturnValue(of(SAVED))
            }),
            mockProvider(DotHttpErrorManagerService, { handle: vi.fn() }),
            mockProvider(DotWorkflowEventHandlerService, { open: vi.fn() }),
            mockProvider(DotPushPublishDialogService, { open: vi.fn() }),
            mockProvider(DotIframeService, { ran: vi.fn(() => ran$.asObservable()) }),
            mockProvider(DotMessageService, { get: vi.fn((key: string) => key) }),
            mockProvider(DotEventsService, { notify: vi.fn() }),
            // A root instance, so the tests can tell the panel's own (component-level) one apart.
            ConfirmationService
        ],
        detectChanges: false
    });

    beforeEach(() => {
        localStorage.clear();
        ran$ = new Subject<DotFunctionInfo>();
        panel = { close: vi.fn(), contentStyleClass: signal('p-0!'), closed: new Subject<void>() };

        spectator = createComponent({
            providers: [{ provide: DotSidePanelRef, useValue: panel }]
        });

        vi.clearAllMocks();
        (spectator.inject(DotContentletService).getContentletByInode as Mock).mockReturnValue(
            of(SAVED)
        );
    });

    /** `appendTo="body"` teleports the drawer out of the fixture, so queries search the document. */
    const getIframe = (): HTMLIFrameElement | null =>
        spectator.query<HTMLIFrameElement>(byTestId('legacy-screen-iframe'), { root: true });

    /** Opens the panel with a request (and, for a create, the resolved create screen). */
    const open = (
        request: DotEditContentRequest | null = EDIT_REQUEST,
        createUrl: string | null = request?.mode === 'new' ? CREATE_URL : null
    ): void => {
        spectator.setInput('request', request);
        spectator.setInput('createUrl', createUrl);
        spectator.detectChanges();
    };

    /** Replaces the iframe's window with a stand-in and fires `load`, as the browser does. */
    const loadFrame = (search = ''): StubFrame => {
        const iframe = getIframe() as HTMLIFrameElement;
        const doc = document.implementation.createHTMLDocument('legacy editor');
        const win = Object.assign(new EventTarget(), {
            document: doc,
            location: { search }
        }) as unknown as StubFrame['win'];

        Object.defineProperty(iframe, 'contentWindow', { configurable: true, get: () => win });
        iframe.dispatchEvent(new Event('load'));

        return { doc, win };
    };

    /** Sends a legacy editor event the way the JSP does: an `ng-event` on its own document. */
    const sendLegacyEvent = (doc: Document, detail: Record<string, unknown>): void => {
        doc.dispatchEvent(new CustomEvent('ng-event', { detail }));
    };

    const searchParams = (): Record<string, string> =>
        Object.fromEntries(
            new URL(getIframe()?.getAttribute('src') ?? '', 'http://localhost').searchParams
        );

    describe('edit URL', () => {
        it('loads the legacy edit screen for the inode, naming the portlet the request came from', () => {
            open();

            const url = new URL(getIframe()?.getAttribute('src') ?? '', 'http://localhost');

            expect(url.pathname).toBe(LAYOUT_PATH);
            expect(searchParams()).toEqual({
                p_p_id: 'content',
                p_p_action: '1',
                p_p_state: 'maximized',
                p_p_mode: 'view',
                _content_struts_action: '/ext/contentlet/edit_contentlet',
                _content_cmd: 'edit',
                inode: 'abc',
                angularCurrentPortlet: 'content-drive'
            });
        });

        it("defaults the portlet to 'content' when the request names none", () => {
            open({ ...EDIT_REQUEST, portletId: undefined });

            expect(searchParams()['angularCurrentPortlet']).toBe('content');
        });

        it('carries the experiment variant and the site when the request has them (UVE)', () => {
            open({ ...EDIT_REQUEST, variantName: 'variant-a', siteId: 'site-1' });

            expect(searchParams()).toEqual(
                expect.objectContaining({ variantName: 'variant-a', host_id: 'site-1' })
            );
        });

        it('adds neither when the request has none', () => {
            open();

            expect(searchParams()).not.toHaveProperty('variantName');
            expect(searchParams()).not.toHaveProperty('host_id');
        });

        it('renders no iframe for an edit without an inode', () => {
            open({ ...EDIT_REQUEST, inode: undefined });

            expect(getIframe()).toBeNull();
        });

        it('renders nothing before it has a request', () => {
            open(null);

            expect(getIframe()).toBeNull();
        });
    });

    /**
     * The opener resolves the create screen before the panel opens, so a refused create opens
     * nothing. The panel only pre-selects the folder on the URL it is handed.
     */
    describe('create URL', () => {
        it('pre-selects the folder by appending its inode to the create screen', () => {
            open(CREATE_REQUEST);

            expect(getIframe()?.getAttribute('src')).toBe(`${CREATE_URL}&folder=f-1`);
        });

        it('starts the query string when the create screen has none', () => {
            open(CREATE_REQUEST, '/c/portal/layout');

            expect(getIframe()?.getAttribute('src')).toBe('/c/portal/layout?folder=f-1');
        });

        it('encodes the folder value', () => {
            open({ ...CREATE_REQUEST, folderInode: 'a b/c' });

            expect(getIframe()?.getAttribute('src')).toBe(`${CREATE_URL}&folder=a%20b%2Fc`);
        });

        it('adds no folder when the request names none', () => {
            open({ ...CREATE_REQUEST, folderInode: undefined });

            expect(getIframe()?.getAttribute('src')).toBe(CREATE_URL);
        });

        it('opens nothing for a create without a create screen', () => {
            open(CREATE_REQUEST, null);

            expect(getIframe()).toBeNull();
        });

        it('refuses a create screen that is not on this server', () => {
            open(CREATE_REQUEST, 'https://elsewhere.example/c/portal/layout');

            expect(getIframe()).toBeNull();
        });
    });

    /**
     * Workflow actions from inside the panel. The admin app already mounts the workflow wizard and
     * the push publish dialog on every route; the panel only has to hand them the event, and hand
     * the wizard's answer back to the legacy editor (FR-010).
     */
    describe('workflow actions', () => {
        it('opens the workflow wizard for an action that needs input', () => {
            open();
            const { doc } = loadFrame();
            const data = { workflow: { id: 'wf-1' }, callback: 'saveAssignCallBackAngular' };

            sendLegacyEvent(doc, { name: 'workflow-wizard', data });

            expect(spectator.inject(DotWorkflowEventHandlerService).open).toHaveBeenCalledWith(
                data
            );
        });

        it("opens the admin app's push publish dialog, not a copy of its own", () => {
            open();
            const { doc } = loadFrame();
            const data = { assetIdentifier: 'id-1', title: 'My legacy content' };

            sendLegacyEvent(doc, { name: 'push-publish', data });

            expect(spectator.inject(DotPushPublishDialogService).open).toHaveBeenCalledWith(data);
        });

        it("hands the wizard's answer back to the legacy editor, inside Angular's zone", () => {
            open();
            const { win } = loadFrame();
            let ranInZone = false;
            const saveAssignCallBackAngular = vi.fn(() => {
                ranInZone = NgZone.isInAngularZone();
            });
            Object.assign(win, { saveAssignCallBackAngular });

            ran$.next({ name: 'saveAssignCallBackAngular', args: ['action-1', { comments: 'x' }] });

            expect(saveAssignCallBackAngular).toHaveBeenCalledWith('action-1', { comments: 'x' });
            expect(ranInZone).toBe(true);
        });

        it('ignores a call the legacy editor has no function for', () => {
            open();
            const { win } = loadFrame();
            Object.assign(win, { notAFunction: 'value' });

            expect(() => ran$.next({ name: 'notAFunction', args: [] })).not.toThrow();
            expect(() => ran$.next({ name: 'missing', args: [] })).not.toThrow();
        });

        it('stops listening for calls once the panel is gone', () => {
            open();
            const { win } = loadFrame();
            const saveAssignCallBackAngular = vi.fn();
            Object.assign(win, { saveAssignCallBackAngular });

            spectator.fixture.destroy();
            ran$.next({ name: 'saveAssignCallBackAngular', args: [] });

            expect(saveAssignCallBackAngular).not.toHaveBeenCalled();
        });
    });

    /**
     * Unsaved changes are never lost silently (US3, FR-034). The prompt and its wording are the
     * new-editor side panel's (`DotEditContentLayoutComponent`), so both panels ask the same
     * question.
     */
    describe('unsaved changes', () => {
        /** The panel's own confirmation service, the one its `<p-confirmDialog>` renders. */
        const panelConfirmation = (): ConfirmationService =>
            spectator.inject(ConfirmationService, true);

        /** Answers the next prompt the way the author would. */
        const answerPrompt = (answer: 'keep' | 'discard' | 'dismiss') =>
            vi.spyOn(panelConfirmation(), 'confirm').mockImplementation((options: Confirmation) => {
                if (answer === 'keep') {
                    options.accept?.();
                } else {
                    options.reject?.(
                        answer === 'discard' ? ConfirmEventType.REJECT : ConfirmEventType.CANCEL
                    );
                }

                return panelConfirmation();
            });

        const changeAField = (doc: Document) =>
            sendLegacyEvent(doc, { name: 'edit-contentlet-data-updated', payload: true });

        it('renders its own confirm dialog, provided on the panel', () => {
            open();

            expect(spectator.query('p-confirmdialog')).not.toBeNull();
            expect(panelConfirmation()).not.toBe(spectator.inject(ConfirmationService));
        });

        it("asks with the new editor's wording before closing with unsaved changes", () => {
            open();
            const { doc } = loadFrame();
            changeAField(doc);
            const confirm = vi.spyOn(panelConfirmation(), 'confirm');

            spectator.component.requestClose();

            expect(confirm).toHaveBeenCalledWith(
                expect.objectContaining({
                    header: 'edit.content.unsaved.changes.title',
                    message: 'edit.content.unsaved.changes.message',
                    acceptLabel: 'edit.content.unsaved.changes.keep',
                    rejectLabel: 'edit.content.unsaved.changes.discard'
                })
            );
        });

        it.each([
            ['Keep editing', 'keep', 0],
            ['Discard', 'discard', 1],
            ['dismissing the prompt', 'dismiss', 0]
        ] as const)('closes only after the author confirms: %s', (_label, answer, closes) => {
            open();
            const { doc } = loadFrame();
            changeAField(doc);
            answerPrompt(answer);

            spectator.component.requestClose();

            expect(panel.close).toHaveBeenCalledTimes(closes);
        });

        it('stops asking once the editor reports the form is clean again', () => {
            open();
            const { doc } = loadFrame();
            changeAField(doc);
            sendLegacyEvent(doc, { name: 'edit-contentlet-data-updated', payload: false });
            const confirm = vi.spyOn(panelConfirmation(), 'confirm');

            spectator.component.requestClose();

            expect(confirm).not.toHaveBeenCalled();
            expect(panel.close).toHaveBeenCalledTimes(1);
        });

        it('does not ask right after a save', () => {
            open();
            const { doc } = loadFrame();
            changeAField(doc);
            sendLegacyEvent(doc, {
                name: 'save-page',
                payload: { contentletIdentifier: 'id-1', contentletInode: 'abc2' }
            });
            const confirm = vi.spyOn(panelConfirmation(), 'confirm');

            spectator.component.requestClose();

            expect(confirm).not.toHaveBeenCalled();
        });

        it('does not ask after the editor reloaded, since the reload already dropped the changes', () => {
            open();
            const { doc } = loadFrame();
            changeAField(doc);
            loadFrame();
            const confirm = vi.spyOn(panelConfirmation(), 'confirm');

            spectator.component.requestClose();

            expect(confirm).not.toHaveBeenCalled();
        });

        it('closes without asking when the editor itself closes', () => {
            open();
            const { doc } = loadFrame();
            changeAField(doc);
            const confirm = vi.spyOn(panelConfirmation(), 'confirm');

            sendLegacyEvent(doc, { name: 'close' });

            expect(confirm).not.toHaveBeenCalled();
            expect(panel.close).toHaveBeenCalledTimes(1);
        });
    });

    /**
     * The five contract events (FR-007 to FR-009). `saved` always carries the full contentlet,
     * fetched by the saved inode; a restore is a save; a removal is `deleted` then `closed`.
     */
    describe('saved', () => {
        it('fetches the saved contentlet by inode and reports it', () => {
            open();
            const { doc } = loadFrame();
            const saved = vi.fn();
            spectator.output('saved').subscribe(saved);

            sendLegacyEvent(doc, {
                name: 'save-page',
                payload: { contentletIdentifier: 'id-1', contentletInode: 'abc2' }
            });

            expect(
                spectator.inject(DotContentletService).getContentletByInode
            ).toHaveBeenCalledWith('abc2');
            expect(saved).toHaveBeenCalledWith(SAVED);
        });

        it('reports the error and no save when the fetch fails', () => {
            (spectator.inject(DotContentletService).getContentletByInode as Mock).mockReturnValue(
                throwError(() => new Error('404'))
            );
            open();
            const { doc } = loadFrame();
            const saved = vi.fn();
            spectator.output('saved').subscribe(saved);

            sendLegacyEvent(doc, {
                name: 'save-page',
                payload: { contentletIdentifier: 'id-1', contentletInode: 'abc2' }
            });

            expect(spectator.inject(DotHttpErrorManagerService).handle).toHaveBeenCalledTimes(1);
            expect(saved).not.toHaveBeenCalled();
        });

        it('reports a save for every save, so a second save refreshes the opener again', () => {
            open();
            const { doc } = loadFrame();
            const saved = vi.fn();
            spectator.output('saved').subscribe(saved);
            const payload = { contentletIdentifier: 'id-1', contentletInode: 'abc2' };

            sendLegacyEvent(doc, { name: 'save-page', payload });
            sendLegacyEvent(doc, { name: 'save-page', payload });

            expect(saved).toHaveBeenCalledTimes(2);
        });
    });

    describe('restore (Bring Back)', () => {
        it('brings the chosen version back in the editor', () => {
            open();
            const { win } = loadFrame();
            const getVersionBack = vi.fn();
            Object.assign(win, { getVersionBack });

            ran$.next({ name: 'getVersionBack', args: ['v1'] });

            expect(getVersionBack).toHaveBeenCalledWith('v1');
        });

        it('reports the restored version as a save once the editor has reloaded with it, once', () => {
            open();
            const { win } = loadFrame();
            Object.assign(win, { getVersionBack: vi.fn() });
            const saved = vi.fn();
            spectator.output('saved').subscribe(saved);
            const restored = createFakeContentlet({ inode: 'v1-restored', identifier: 'id-1' });
            (spectator.inject(DotContentletService).getContentletByInode as Mock).mockReturnValue(
                of(restored)
            );

            ran$.next({ name: 'getVersionBack', args: ['v1'] });

            expect(saved).not.toHaveBeenCalled();

            loadFrame('?p_p_id=content&inode=v1-restored&lang=1');
            loadFrame('?p_p_id=content&inode=v1-restored&lang=1');

            expect(
                spectator.inject(DotContentletService).getContentletByInode
            ).toHaveBeenCalledWith('v1-restored');
            expect(saved).toHaveBeenCalledTimes(1);
            expect(saved).toHaveBeenCalledWith(restored);
        });

        it('reports no save for a reload with no Bring Back', () => {
            open();
            loadFrame();
            const saved = vi.fn();
            spectator.output('saved').subscribe(saved);

            loadFrame('?p_p_id=content&inode=abc');

            expect(saved).not.toHaveBeenCalled();
        });
    });

    describe('deleted', () => {
        it('supplies the window hook the editor calls when it removed the content', () => {
            open();
            const { win } = loadFrame();

            expect(typeof win.ngEditContentletEvents?.next).toBe('function');
        });

        it('reports deleted, never saved, when the editor announces a removed contentlet', () => {
            open();
            const { win, doc } = loadFrame();
            const deleted = vi.fn();
            const saved = vi.fn();
            const closed = vi.fn();
            spectator.output('deleted').subscribe(deleted);
            spectator.output('saved').subscribe(saved);
            spectator.output('closed').subscribe(closed);

            // The JSP: the hook first, then the generic close.
            win.ngEditContentletEvents?.next({ name: 'deleted-contenlet', data: {} });
            sendLegacyEvent(doc, { name: 'close' });

            expect(deleted).toHaveBeenCalledTimes(1);
            expect(saved).not.toHaveBeenCalled();
            expect(panel.close).toHaveBeenCalledTimes(1);
        });

        it('reports deleted then closes when a page was deleted', () => {
            open();
            const { doc } = loadFrame();
            const deleted = vi.fn();
            spectator.output('deleted').subscribe(deleted);

            sendLegacyEvent(doc, { name: 'deleted-page', payload: {} });

            expect(deleted).toHaveBeenCalledTimes(1);
            expect(panel.close).toHaveBeenCalledTimes(1);
        });

        it('ignores other calls through the hook', () => {
            open();
            const { win } = loadFrame();
            const deleted = vi.fn();
            spectator.output('deleted').subscribe(deleted);

            win.ngEditContentletEvents?.next({ name: 'save', data: {} });

            expect(deleted).not.toHaveBeenCalled();
        });
    });

    describe('closed', () => {
        it('reports closed once when the panel closes, for any reason', () => {
            open();
            const closed = vi.fn();
            spectator.output('closed').subscribe(closed);

            panel.closed.next();
            panel.closed.complete();

            expect(closed).toHaveBeenCalledTimes(1);
        });
    });

    describe('compare versions', () => {
        it('opens the root compare dialog', () => {
            open();
            const { doc } = loadFrame();
            const data = { inode: 'v2', identifier: 'id-1', language: 'en-us' };

            sendLegacyEvent(doc, { name: 'compare-contentlet', data });

            expect(spectator.inject(DotEventsService).notify).toHaveBeenCalledWith(
                'compare-contentlet',
                data
            );
        });
    });

    /**
     * Switching language inside the legacy editor reloads it with `lang=<id>` and sends no event,
     * so the panel reads the language off the reload (FR-036).
     */
    describe('language', () => {
        it('reports a switch to another language, once', () => {
            open();
            loadFrame();
            const changed = vi.fn();
            spectator.output('languageChanged').subscribe(changed);

            loadFrame('?p_p_id=content&lang=3&inode=abc-es&identifier=id-1');

            expect(changed).toHaveBeenCalledTimes(1);
            expect(changed).toHaveBeenCalledWith(3);
        });

        it('reports nothing for a reload in the same language, or one that names none', () => {
            open();
            loadFrame();
            const changed = vi.fn();
            spectator.output('languageChanged').subscribe(changed);

            loadFrame('?p_p_id=content&lang=1');
            loadFrame('?p_p_id=content&inode=abc');

            expect(changed).not.toHaveBeenCalled();
        });
    });

    describe('first save of a new page', () => {
        it('asks the opener to open the page editor, in the language the close names', () => {
            open(CREATE_REQUEST);
            const { doc } = loadFrame();
            const pageEditor = vi.fn();
            spectator.output('pageEditorRequested').subscribe(pageEditor);

            sendLegacyEvent(doc, {
                name: 'close',
                data: { redirectUrl: '/blog/new-page', languageId: '2' }
            });

            expect(pageEditor).toHaveBeenCalledWith({ url: '/blog/new-page', languageId: 2 });
            expect(panel.close).not.toHaveBeenCalled();
        });
    });

    /**
     * A block editor inside the legacy editor asks to open other content (FR-032). The custom
     * element bridge dispatches `edit-contentlet` on the iframe document; the panel hands it to the
     * opener, which stacks a panel. The legacy editor's window is never navigated.
     */
    describe('content that opens other content', () => {
        it('asks the opener to open an existing contentlet a block editor named', () => {
            open();
            const { doc } = loadFrame();
            const openContent = vi.fn();
            spectator.output('openContent').subscribe(openContent);

            sendLegacyEvent(doc, {
                name: 'edit-contentlet',
                data: { inode: 'rel-1', contentType: 'Blog' }
            });

            expect(openContent).toHaveBeenCalledWith({
                request: { mode: 'edit', inode: 'rel-1', contentType: 'Blog' },
                source: 'block-editor'
            });
            expect(panel.close).not.toHaveBeenCalled();
        });

        it('asks the opener to create content a legacy screen named', () => {
            open();
            const { doc } = loadFrame();
            const openContent = vi.fn();
            spectator.output('openContent').subscribe(openContent);

            sendLegacyEvent(doc, {
                name: 'create-contentlet',
                data: { url: '/ignored', contentType: 'Blog', folderPath: '/news/' }
            });

            expect(openContent).toHaveBeenCalledWith({
                request: { mode: 'new', contentType: 'Blog', folderPath: '/news/' },
                source: 'block-editor'
            });
        });
    });

    describe('legacy editor events', () => {
        it("closes the panel on the editor's own close", () => {
            open();
            const { doc } = loadFrame();

            sendLegacyEvent(doc, { name: 'close' });

            expect(panel.close).toHaveBeenCalledTimes(1);
        });

        it('ignores events it has no use for', () => {
            open();
            const { doc } = loadFrame();
            const saved = vi.fn();
            spectator.output('saved').subscribe(saved);

            sendLegacyEvent(doc, { name: 'edit-contentlet-loaded', data: { contentType: 'Blog' } });
            sendLegacyEvent(doc, { name: 'edit-page', data: { url: '/home' } });
            sendLegacyEvent(doc, { name: 'update-workflow-action' });

            expect(panel.close).not.toHaveBeenCalled();
            expect(saved).not.toHaveBeenCalled();
        });

        it('listens to the new document after every reload, and only once', () => {
            open();
            const first = loadFrame();
            const second = loadFrame();

            sendLegacyEvent(first.doc, { name: 'close' });
            sendLegacyEvent(second.doc, { name: 'close' });

            expect(panel.close).toHaveBeenCalledTimes(1);
        });
    });

    describe('loading', () => {
        const spinner = () => spectator.query(byTestId('legacy-screen-loading'), { root: true });

        it('shows a spinner until the legacy editor has loaded', () => {
            open();

            expect(spinner()).toBeTruthy();

            loadFrame();
            spectator.detectChanges();

            expect(spinner()).toBeNull();
        });

        it('shows it again when the panel opens other content', () => {
            open();
            loadFrame();
            spectator.detectChanges();

            open({ ...EDIT_REQUEST, inode: 'other-inode' });

            expect(spinner()).toBeTruthy();
        });
    });

    describe('closing', () => {
        it('closes the panel from requestClose when nothing changed', () => {
            open();

            spectator.component.requestClose();

            expect(panel.close).toHaveBeenCalledTimes(1);
        });
    });
});
