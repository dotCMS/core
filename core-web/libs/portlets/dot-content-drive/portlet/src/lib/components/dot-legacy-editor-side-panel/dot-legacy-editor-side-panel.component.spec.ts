import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator
} from '@openng/spectator/vitest';
import { MockPipe } from 'ng-mocks';
import { of, Subject } from 'rxjs';
import { Mock, vi } from 'vitest';

import { NgZone } from '@angular/core';

import { ConfirmationService, ConfirmEventType, Confirmation } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { Drawer, DrawerModule } from 'primeng/drawer';

import {
    DotActionUrlService,
    DotEventsService,
    DotHttpErrorManagerService,
    DotIframeService,
    DotMessageService,
    DotUiColorsService,
    DotWorkflowEventHandlerService
} from '@dotcms/data-access';
import { DotPushPublishDialogService } from '@dotcms/dotcms-js';
import { DotFunctionInfo } from '@dotcms/dotcms-models';
import { DotSidePanelNavController } from '@dotcms/edit-content';
import { DotMessagePipe } from '@dotcms/ui';

import { DotLegacyEditorSidePanelComponent } from './dot-legacy-editor-side-panel.component';

import { DotLegacyEditorRequest } from '../../shared/legacy-editor.models';

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

const CREATE_REQUEST: DotLegacyEditorRequest = {
    mode: 'new',
    contentTypeVariable: 'Banner',
    folderInode: 'f-1',
    languageId: 2,
    title: 'Banner',
    portletId: 'content-drive',
    createUrl: CREATE_URL
};

const EDIT_REQUEST: DotLegacyEditorRequest = {
    mode: 'edit',
    inode: 'abc',
    identifier: 'id-1',
    languageId: 1,
    title: 'My legacy content',
    portletId: 'content-drive'
};

/**
 * A stand-in for the legacy editor's window. The test DOM never loads the JSP, so the panel would
 * otherwise find an empty frame. A real `Document` keeps `addEventListener`/`dispatchEvent` honest,
 * so the panel's listeners are exercised exactly as the JSP's `document.dispatchEvent` reaches them.
 */
interface StubFrame {
    doc: Document;
    win: Window;
}

/**
 * The test DOM (happy-dom) would otherwise try to fetch the JSP for every rendered iframe and log
 * "task manager destroyed" errors once each test has torn the frame down. The panel is tested
 * against a stand-in window (see {@link StubFrame}), so no page needs to load.
 */
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
    let navController: { acquire: Mock; release: Mock; isTop: Mock };

    const createComponent = createComponentFactory({
        component: DotLegacyEditorSidePanelComponent,
        overrideComponents: [
            [
                DotLegacyEditorSidePanelComponent,
                {
                    set: {
                        imports: [
                            DrawerModule,
                            ButtonModule,
                            MockPipe(DotMessagePipe, (key: string) => key)
                        ]
                    }
                }
            ]
        ],
        providers: [
            mockProvider(DotUiColorsService, { setColors: vi.fn() }),
            mockProvider(DotActionUrlService, {
                getCreateContentletUrl: vi.fn().mockReturnValue(of(CREATE_URL))
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
        navController = {
            acquire: vi.fn(),
            release: vi.fn(),
            isTop: vi.fn().mockReturnValue(true)
        };

        spectator = createComponent({
            providers: [{ provide: DotSidePanelNavController, useValue: navController }]
        });

        // The factory's mocks are shared across tests: clear their calls and restore the answer a
        // test may have overridden.
        vi.clearAllMocks();
        (spectator.inject(DotActionUrlService).getCreateContentletUrl as Mock).mockReturnValue(
            of(CREATE_URL)
        );
    });

    /** `appendTo="body"` teleports the drawer out of the fixture, so queries search the document. */
    const getIframe = (): HTMLIFrameElement | null =>
        spectator.query<HTMLIFrameElement>(byTestId('legacy-editor-iframe'), { root: true });

    /** Opens the panel with a request and renders it. */
    const open = (request: DotLegacyEditorRequest | null = EDIT_REQUEST): void => {
        spectator.setInput('request', request);
        spectator.detectChanges();
    };

    /** Replaces the iframe's window with a stand-in and fires `load`, as the browser does. */
    const loadFrame = (search = ''): StubFrame => {
        const iframe = getIframe() as HTMLIFrameElement;
        const doc = document.implementation.createHTMLDocument('legacy editor');
        // An EventTarget, so the panel can listen to the window as it does to the real one.
        const win = Object.assign(new EventTarget(), {
            document: doc,
            location: { search }
        }) as unknown as Window;

        Object.defineProperty(iframe, 'contentWindow', { configurable: true, get: () => win });
        iframe.dispatchEvent(new Event('load'));

        return { doc, win };
    };

    /** Sends a legacy editor event the way the JSP does: an `ng-event` on its own document. */
    const sendLegacyEvent = (doc: Document, detail: Record<string, unknown>): void => {
        doc.dispatchEvent(new CustomEvent('ng-event', { detail }));
    };

    const clickButton = (testId: string): void => {
        const button = spectator.query(byTestId(testId), { root: true })?.querySelector('button');
        spectator.click(button as HTMLElement);
    };

    describe('edit URL', () => {
        it('loads the legacy edit screen for the inode, naming Content Drive as the portlet', () => {
            open();

            const url = new URL(getIframe()?.getAttribute('src') ?? '', 'http://localhost');

            expect(url.pathname).toBe(LAYOUT_PATH);
            expect(Object.fromEntries(url.searchParams)).toEqual({
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

        it('renders no iframe for an edit without an inode', () => {
            open({ ...EDIT_REQUEST, inode: undefined });

            expect(getIframe()).toBeNull();
        });

        it('renders nothing while closed', () => {
            open(null);

            expect(getIframe()).toBeNull();
        });
    });

    /**
     * The navigation service resolves the create screen before the panel opens, so a failure opens
     * nothing (#37759, T112). The panel only pre-selects the folder on the URL it is handed.
     */
    describe('create URL', () => {
        it('loads the create screen the request carries, without asking the server again', () => {
            open(CREATE_REQUEST);

            expect(
                spectator.inject(DotActionUrlService).getCreateContentletUrl
            ).not.toHaveBeenCalled();
            expect(getIframe()).not.toBeNull();
        });

        it('pre-selects the folder by appending its inode to the create screen', () => {
            open(CREATE_REQUEST);

            expect(getIframe()?.getAttribute('src')).toBe(`${CREATE_URL}&folder=f-1`);
        });

        it('starts the query string when the create screen has none', () => {
            open({ ...CREATE_REQUEST, createUrl: '/c/portal/layout' });

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
            open({ ...CREATE_REQUEST, createUrl: undefined });

            expect(getIframe()).toBeNull();
        });

        it('refuses a create screen that is not on this server', () => {
            open({ ...CREATE_REQUEST, createUrl: 'https://elsewhere.example/c/portal/layout' });

            expect(getIframe()).toBeNull();
        });
    });

    /**
     * Workflow actions from inside the panel (#37759, US3). The admin app already mounts the
     * workflow wizard and the push publish dialog on every route; the panel only has to hand them
     * the event, and hand the wizard's answer back to the legacy editor (FR-009, FR-010).
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

            // The root instance is the one the mounted dialog listens to. A component-level
            // provider would swallow the call and no dialog would open.
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
     * Unsaved changes are never lost silently (#37759, US4). The prompt and its wording are the
     * new-editor side panel's (`DotEditContentLayoutComponent`), so both panels ask the same
     * question (FR-017 to FR-019).
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
            const closed = vi.fn();
            spectator.output('closed').subscribe(closed);

            spectator.component.requestClose();

            expect(closed).toHaveBeenCalledTimes(closes);
        });

        /** Every way out of the panel goes through the prompt (FR-017). */
        const CLOSE_TRIGGERS: [string, (doc: Document) => void][] = [
            ['the close button', () => clickButton('legacy-side-panel-close')],
            [
                'Escape in the admin page',
                () =>
                    document.dispatchEvent(
                        new KeyboardEvent('keydown', {
                            key: 'Escape',
                            bubbles: true,
                            cancelable: true
                        })
                    )
            ],
            [
                'Escape inside the legacy editor',
                (doc) =>
                    doc.dispatchEvent(
                        new KeyboardEvent('keydown', { key: 'Escape', bubbles: true })
                    )
            ],
            [
                'a click on its own mask',
                () => {
                    const mask = document.createElement('div');
                    document.body.appendChild(mask);
                    spectator.query(Drawer)!.mask = mask;
                    mask.dispatchEvent(new MouseEvent('click', { bubbles: true }));
                    mask.remove();
                }
            ]
        ];

        it.each(CLOSE_TRIGGERS)('asks before closing from %s', (_label, trigger) => {
            open();
            const { doc } = loadFrame();
            changeAField(doc);
            const confirm = answerPrompt('keep');
            const closed = vi.fn();
            spectator.output('closed').subscribe(closed);

            trigger(doc);

            expect(confirm).toHaveBeenCalledTimes(1);
            expect(closed).not.toHaveBeenCalled();
        });

        it('stops asking once the editor reports the form is clean again', () => {
            open();
            const { doc } = loadFrame();
            changeAField(doc);
            sendLegacyEvent(doc, { name: 'edit-contentlet-data-updated', payload: false });
            const confirm = vi.spyOn(panelConfirmation(), 'confirm');
            const closed = vi.fn();
            spectator.output('closed').subscribe(closed);

            spectator.component.requestClose();

            expect(confirm).not.toHaveBeenCalled();
            expect(closed).toHaveBeenCalledTimes(1);
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

        it('closes without asking when the editor itself closes, as after a delete', () => {
            open();
            const { doc } = loadFrame();
            changeAField(doc);
            const confirm = vi.spyOn(panelConfirmation(), 'confirm');
            const closed = vi.fn();
            spectator.output('closed').subscribe(closed);

            sendLegacyEvent(doc, { name: 'close' });

            expect(confirm).not.toHaveBeenCalled();
            expect(closed).toHaveBeenCalledTimes(1);
        });
    });

    /**
     * Compare versions from the History tab, and Bring Back (#37759, US6). The admin app already
     * mounts the compare dialog on every route and listens for `compare-contentlet`; its Bring Back
     * asks the legacy editor to `getVersionBack`, which reloads the editor with that version and
     * sends no save, so the panel reports the restore once the editor has reloaded.
     */
    describe('compare and Bring Back', () => {
        it('opens the compare dialog over Content Drive', () => {
            open();
            const { doc } = loadFrame();
            const data = { inode: 'v2', identifier: 'id-1', language: 'en-us' };

            sendLegacyEvent(doc, { name: 'compare-contentlet', data });

            expect(spectator.inject(DotEventsService).notify).toHaveBeenCalledWith(
                'compare-contentlet',
                data
            );
        });

        it('brings the chosen version back in the editor', () => {
            open();
            const { win } = loadFrame();
            const getVersionBack = vi.fn();
            Object.assign(win, { getVersionBack });

            ran$.next({ name: 'getVersionBack', args: ['v1'] });

            expect(getVersionBack).toHaveBeenCalledWith('v1');
        });

        it('reports the restore once the editor has reloaded with it, and only once', () => {
            open();
            const { win } = loadFrame();
            Object.assign(win, { getVersionBack: vi.fn() });
            const restored = vi.fn();
            spectator.output('versionRestored').subscribe(restored);

            ran$.next({ name: 'getVersionBack', args: ['v1'] });

            expect(restored).not.toHaveBeenCalled();

            loadFrame();
            loadFrame();

            expect(restored).toHaveBeenCalledTimes(1);
        });

        it('reports no restore for a reload with no Bring Back', () => {
            open();
            loadFrame();
            const restored = vi.fn();
            spectator.output('versionRestored').subscribe(restored);

            loadFrame();

            expect(restored).not.toHaveBeenCalled();
        });
    });

    /**
     * The URL follows the open panel (#37759, US7). Switching language inside the legacy editor
     * reloads it with `lang=<id>` and sends no event, so the panel reads the language off the
     * reload (FR-020), and reports saves in the language it is showing (FR-025).
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

        it('reports saves in the language it switched to', () => {
            open();
            loadFrame();
            const { doc } = loadFrame('?lang=3');
            const saved = vi.fn();
            spectator.output('saved').subscribe(saved);

            sendLegacyEvent(doc, {
                name: 'save-page',
                payload: { contentletIdentifier: 'id-1', contentletInode: 'abc-es' }
            });

            expect(saved).toHaveBeenCalledWith({
                identifier: 'id-1',
                inode: 'abc-es',
                languageId: 3
            });
        });

        it('reads the language off the saved version when it does not know which one it shows', () => {
            open({ ...EDIT_REQUEST, languageId: 0 });
            const { doc } = loadFrame();
            const saved = vi.fn();
            spectator.output('saved').subscribe(saved);

            sendLegacyEvent(doc, {
                name: 'save-page',
                payload: {
                    contentletIdentifier: 'id-1',
                    contentletInode: 'abc-it',
                    allLangContentlets: [
                        { inode: 'abc-en', identifier: 'id-1', languageId: 1 },
                        { inode: 'abc-it', identifier: 'id-1', languageId: 4 }
                    ]
                }
            });

            expect(saved).toHaveBeenCalledWith(expect.objectContaining({ languageId: 4 }));
        });
    });

    describe('first save of a new page', () => {
        it('asks the opener to open the page editor, in the language the close names', () => {
            open(CREATE_REQUEST);
            const { doc } = loadFrame();
            const pageEditor = vi.fn();
            const closed = vi.fn();
            spectator.output('pageEditorRequested').subscribe(pageEditor);
            spectator.output('closed').subscribe(closed);

            sendLegacyEvent(doc, {
                name: 'close',
                data: { redirectUrl: '/blog/new-page', languageId: '2' }
            });

            expect(pageEditor).toHaveBeenCalledWith({ url: '/blog/new-page', languageId: 2 });
            expect(closed).not.toHaveBeenCalled();
        });
    });

    describe('loading', () => {
        const spinner = () => spectator.query(byTestId('legacy-editor-loading'), { root: true });

        it('shows a spinner until the legacy editor has loaded', () => {
            open();

            expect(spinner()).toBeTruthy();

            loadFrame();
            spectator.detectChanges();

            expect(spinner()).toBeNull();
        });

        // The editor reloads itself on a save, a language switch or a restored version.
        it('shows it again while the legacy editor reloads itself', () => {
            open();
            const { win } = loadFrame();
            spectator.detectChanges();

            win.dispatchEvent(new Event('pagehide'));
            spectator.detectChanges();

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

    describe('drawer', () => {
        it('shows the request title in the header', () => {
            open();

            expect(
                spectator.query(byTestId('legacy-side-panel-title'), { root: true })?.textContent
            ).toContain('My legacy content');
        });

        it('opens at 80% and expands to the full width, remembering the choice', () => {
            open();
            const root = () =>
                spectator.query<HTMLElement>(byTestId('legacy-editor-side-panel'), {
                    root: true
                });

            expect(root()?.style.width).toBe('80%');

            clickButton('legacy-side-panel-expand');
            spectator.detectChanges();

            expect(root()?.style.width).toBe('100%');
            // Same key as the new-editor panel, so the author's choice carries over between the two.
            expect(localStorage.getItem('dot-edit-content-side-panel-expanded')).toBe('true');
        });

        it('registers with the side-panel stack and leaves it when destroyed', async () => {
            open();
            await spectator.fixture.whenStable();

            expect(navController.acquire).toHaveBeenCalledWith(spectator.component);

            spectator.fixture.destroy();

            expect(navController.release).toHaveBeenCalledWith(spectator.component);
        });
    });

    describe('closing', () => {
        it('emits closed from the close button when nothing changed', () => {
            open();
            const closed = vi.fn();
            spectator.output('closed').subscribe(closed);

            clickButton('legacy-side-panel-close');

            expect(closed).toHaveBeenCalledTimes(1);
        });

        it('emits closed from requestClose when nothing changed', () => {
            open();
            const closed = vi.fn();
            spectator.output('closed').subscribe(closed);

            spectator.component.requestClose();

            expect(closed).toHaveBeenCalledTimes(1);
        });

        it('closes on Escape pressed in the admin page', () => {
            open();
            const closed = vi.fn();
            spectator.output('closed').subscribe(closed);

            document.dispatchEvent(
                new KeyboardEvent('keydown', { key: 'Escape', bubbles: true, cancelable: true })
            );

            expect(closed).toHaveBeenCalledTimes(1);
        });

        it('closes on Escape pressed inside the legacy editor, which the admin page never sees', () => {
            open();
            const { doc } = loadFrame();
            const closed = vi.fn();
            spectator.output('closed').subscribe(closed);

            doc.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }));

            expect(closed).toHaveBeenCalledTimes(1);
        });

        // The admin page can't see the editor's own dialogs (Dojo, TinyMCE), so an Escape one of
        // them already handled is told apart by having been consumed.
        it('does not close on an Escape the legacy editor already handled', () => {
            open();
            const { doc } = loadFrame();
            const closed = vi.fn();
            spectator.output('closed').subscribe(closed);
            const handledByEditor = new KeyboardEvent('keydown', {
                key: 'Escape',
                bubbles: true,
                cancelable: true
            });
            handledByEditor.preventDefault();

            doc.dispatchEvent(handledByEditor);

            expect(closed).not.toHaveBeenCalled();
        });

        it('does not close on Escape when another side panel is on top', () => {
            open();
            const { doc } = loadFrame();
            navController.isTop.mockReturnValue(false);
            const closed = vi.fn();
            spectator.output('closed').subscribe(closed);

            doc.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }));
            document.dispatchEvent(
                new KeyboardEvent('keydown', { key: 'Escape', bubbles: true, cancelable: true })
            );

            expect(closed).not.toHaveBeenCalled();
        });

        /** PrimeNG builds the mask during an animation the test DOM never runs, hence the stand-in. */
        const clickMask = ({ ownedByPanel }: { ownedByPanel: boolean }): void => {
            const mask = document.createElement('div');
            mask.classList.add('p-drawer-mask');
            document.body.appendChild(mask);

            if (ownedByPanel) {
                spectator.query(Drawer)!.mask = mask;
            }

            mask.dispatchEvent(new MouseEvent('click', { bubbles: true }));
            mask.remove();
        };

        it('closes on a click on its own mask when it is on top', () => {
            open();
            const closed = vi.fn();
            spectator.output('closed').subscribe(closed);

            clickMask({ ownedByPanel: true });

            expect(closed).toHaveBeenCalledTimes(1);
        });

        it('ignores a click on its own mask when another side panel is on top', () => {
            open();
            navController.isTop.mockReturnValue(false);
            const closed = vi.fn();
            spectator.output('closed').subscribe(closed);

            clickMask({ ownedByPanel: true });

            expect(closed).not.toHaveBeenCalled();
        });

        it("ignores a click on another drawer's mask", () => {
            open();
            const closed = vi.fn();
            spectator.output('closed').subscribe(closed);

            clickMask({ ownedByPanel: false });

            expect(closed).not.toHaveBeenCalled();
        });
    });

    describe('legacy editor events', () => {
        it("emits closed on the editor's own close", () => {
            open();
            const { doc } = loadFrame();
            const closed = vi.fn();
            spectator.output('closed').subscribe(closed);

            sendLegacyEvent(doc, { name: 'close' });

            expect(closed).toHaveBeenCalledTimes(1);
        });

        it('emits closed when a page was deleted', () => {
            open();
            const { doc } = loadFrame();
            const closed = vi.fn();
            spectator.output('closed').subscribe(closed);

            sendLegacyEvent(doc, { name: 'deleted-page', payload: {} });

            expect(closed).toHaveBeenCalledTimes(1);
        });

        it('reports a save with the identifier, the saved inode and the language it shows', () => {
            open();
            const { doc } = loadFrame();
            const saved = vi.fn();
            spectator.output('saved').subscribe(saved);

            sendLegacyEvent(doc, {
                name: 'save-page',
                payload: {
                    contentletIdentifier: 'id-1',
                    contentletInode: 'abc2',
                    allLangContentlets: [{ inode: 'abc2', identifier: 'id-1', languageId: 1 }]
                }
            });

            expect(saved).toHaveBeenCalledWith({
                identifier: 'id-1',
                inode: 'abc2',
                languageId: 1
            });
        });

        it('applies the admin UI colors to the editor once it loads', () => {
            open();
            const { doc } = loadFrame();

            expect(spectator.inject(DotUiColorsService).setColors).toHaveBeenCalledWith(
                doc.querySelector('html')
            );
        });

        it('ignores events it has no use for', () => {
            open();
            const { doc } = loadFrame();
            const closed = vi.fn();
            const saved = vi.fn();
            spectator.output('closed').subscribe(closed);
            spectator.output('saved').subscribe(saved);

            sendLegacyEvent(doc, { name: 'edit-contentlet-loaded', data: { contentType: 'Blog' } });
            sendLegacyEvent(doc, { name: 'edit-page', data: { url: '/home' } });

            expect(closed).not.toHaveBeenCalled();
            expect(saved).not.toHaveBeenCalled();
        });

        it('listens to the new document after every reload, and only once', () => {
            open();
            const first = loadFrame();
            const second = loadFrame();
            const closed = vi.fn();
            spectator.output('closed').subscribe(closed);

            // The old document is gone after a reload; only the current one is heard.
            sendLegacyEvent(first.doc, { name: 'close' });
            sendLegacyEvent(second.doc, { name: 'close' });

            expect(closed).toHaveBeenCalledTimes(1);
        });
    });
});
