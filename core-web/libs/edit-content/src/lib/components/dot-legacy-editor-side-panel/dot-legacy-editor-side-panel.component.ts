import { HttpErrorResponse } from '@angular/common/http';
import {
    Component,
    DestroyRef,
    NgZone,
    computed,
    inject,
    input,
    linkedSignal,
    output,
    signal
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { ConfirmationService, ConfirmEventType } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';

import { take } from 'rxjs/operators';

import {
    DotContentletService,
    DotEventsService,
    DotHttpErrorManagerService,
    DotIframeService,
    DotMessageService,
    DotWorkflowEventHandlerService
} from '@dotcms/data-access';
import { DotPushPublishDialogService } from '@dotcms/dotcms-js';
import {
    DotCMSContentlet,
    DotCMSWorkflowActionEvent,
    DotFunctionInfo,
    DotPushPublishDialogData
} from '@dotcms/dotcms-models';
import { DotSidePanelContent, DotSidePanelRef } from '@dotcms/ui';

import { DotPageEditorRequest } from '../../models/dot-edit-content-panel-events.model';
import { DotEditContentRequest } from '../../models/dot-edit-content-request.model';
import { DotLegacyScreenEvent, DotLegacyScreenRequest } from '../../models/dot-legacy-screen.model';
import { DotOpenContentEvent } from '../../models/dot-open-content-event.model';
import { DotLegacyScreenSidePanelComponent } from '../dot-legacy-screen-side-panel/dot-legacy-screen-side-panel.component';

/** The admin route every legacy portlet screen is served from. */
const LAYOUT_URL = '/c/portal/layout';

/** Where the request names no portlet, the legacy editor is told it runs in the content search. */
const DEFAULT_PORTLET = 'content';

/**
 * What the legacy editor calls on the window hook it expects its host to supply
 * (`edit_contentlet_js_inc.jsp` declares `var ngEditContentletEvents;` and calls `next` on it).
 */
interface DotLegacyEditorHook {
    next: (detail: DotLegacyScreenEvent) => void;
}

/**
 * The legacy (JSP) content editor as a side panel's content (#38303). Moved here from Content
 * Drive (#37759) so every surface opens it through `DotEditContentOpenerService`.
 *
 * It renders the generic {@link DotLegacyScreenSidePanelComponent}, which loads the editor and
 * passes its events through, and turns those events into the same five outputs the new editor
 * panel has (`saved`, `languageChanged`, `pageEditorRequested`, `deleted`, `closed`), plus
 * `openContent` when a block editor inside it asks to open other content. A save is always reported
 * with the full contentlet, fetched by the saved inode, and a restored version is a save.
 *
 * It is kept apart from the new-editor panel so that removing the legacy editor later never touches
 * it. Remove with the legacy editor.
 */
@Component({
    selector: 'dot-legacy-editor-side-panel',
    imports: [ConfirmDialogModule, DotLegacyScreenSidePanelComponent],
    // Fills the drawer, so the iframe can take its whole height.
    host: { class: 'flex flex-1' },
    // Its own instance, so the prompt renders in this panel's `<p-confirmDialog>` and never in
    // another one mounted elsewhere on the page.
    providers: [ConfirmationService],
    templateUrl: './dot-legacy-editor-side-panel.component.html'
})
export class DotLegacyEditorSidePanelComponent implements DotSidePanelContent {
    readonly #panel = inject(DotSidePanelRef);
    readonly #zone = inject(NgZone);
    readonly #destroyRef = inject(DestroyRef);
    // Root (admin app) instances, on purpose: the workflow wizard and push publish dialogs are
    // mounted once for every admin route and listen to these. Never provide them on this component.
    readonly #workflowEventHandler = inject(DotWorkflowEventHandlerService);
    readonly #pushPublishDialog = inject(DotPushPublishDialogService);
    readonly #iframeService = inject(DotIframeService);
    readonly #events = inject(DotEventsService);
    readonly #confirmation = inject(ConfirmationService);
    readonly #messages = inject(DotMessageService);
    readonly #contentletService = inject(DotContentletService);
    readonly #httpErrorManager = inject(DotHttpErrorManagerService);

    /** What to open. */
    readonly request = input<DotEditContentRequest | null>(null);

    /**
     * The create screen the server named for a `new` request (`/api/v1/portlet/_actionurl/<type>`).
     * The opener resolves it before the panel opens, so a refused create never opens a panel.
     */
    readonly createUrl = input<string | null>(null);

    /** Every save, and every restored version, with the full saved contentlet. */
    readonly saved = output<DotCMSContentlet>();

    /**
     * The author switched language inside the legacy editor, which reloads itself in that language
     * and sends no event. The opener can name the language in its URL.
     */
    readonly languageChanged = output<number>();

    /**
     * The legacy editor asked to open a page in the page editor: the first save of a new page. The
     * opener closes the panel and opens it.
     */
    readonly pageEditorRequested = output<DotPageEditorRequest>();

    /** The content was removed from inside the editor. The editor's own close follows. */
    readonly deleted = output<void>();

    /** The panel closed, for any reason. Emitted once. */
    readonly closed = output<void>();

    /** A block editor inside the legacy editor asked to open other content. */
    readonly openContent = output<DotOpenContentEvent>();

    /**
     * Whether the author changed something since the last save. Set from the legacy editor's own
     * `edit-contentlet-data-updated` event and cleared by a save or a reload.
     */
    readonly #dirty = signal(false);

    /**
     * The language the editor is showing: the request's, until the author switches language
     * inside the editor. Reset whenever a new request opens.
     */
    readonly #languageId = linkedSignal(() => this.request()?.languageId ?? 0);

    /** The legacy editor screen for the request, or `null` when there is nothing valid to load. */
    protected readonly $screen = computed<DotLegacyScreenRequest | null>(() => {
        const request = this.request();
        const url =
            request?.mode === 'new'
                ? buildCreateUrl(request, this.createUrl())
                : buildEditUrl(request);

        return url ? { url, title: request?.title ?? '' } : null;
    });

    /**
     * A Bring Back is under way: the editor was asked for `getVersionBack` and will reload with the
     * restored version. Reported as a save once that reload lands.
     */
    #restoreInFlight = false;

    /** The window of the document the editor loaded last. */
    #editorWindow: Window | null = null;

    constructor() {
        // Other admin code asks the legacy editor to run one of its functions through this service:
        // the workflow wizard hands its answer back with `saveAssignCallBackAngular`, and the compare
        // dialog's Bring Back calls `getVersionBack`. The full-page editor's iframe answers them the
        // same way (`IframeComponent`).
        this.#iframeService
            .ran()
            .pipe(takeUntilDestroyed())
            .subscribe((call) => this.#runInEditor(call));

        this.#panel.closed.pipe(take(1), takeUntilDestroyed()).subscribe(() => this.closed.emit());
    }

    /**
     * Close intent: the X button, ESC, a mask click, or browser Back routed by the opener.
     *
     * With unsaved changes, it asks first, with the new-editor side panel's prompt and wording,
     * and closes the panel only once the author discards them.
     */
    requestClose(): void {
        if (!this.#dirty()) {
            this.#panel.close();

            return;
        }

        this.#confirmation.confirm({
            header: this.#messages.get('edit.content.unsaved.changes.title'),
            message: this.#messages.get('edit.content.unsaved.changes.message'),
            acceptLabel: this.#messages.get('edit.content.unsaved.changes.keep'),
            rejectLabel: this.#messages.get('edit.content.unsaved.changes.discard'),
            acceptIcon: 'hidden',
            rejectIcon: 'hidden',
            rejectButtonStyleClass: 'p-button-outlined',
            // "Keep editing", or X / ESC on the prompt itself: stay open with the edits.
            accept: () => undefined,
            reject: (type?: ConfirmEventType) => {
                if (type === ConfirmEventType.REJECT) {
                    this.#dirty.set(false);
                    this.#panel.close();
                }
            }
        });
    }

    /**
     * Runs every time the editor loads a document: the first load, and every reload it does on its
     * own (a save, a language switch, a restored version). A new document holds none of the old
     * one's changes, may name another language, and needs the window hook supplied again.
     *
     * @param editorWindow The window of the document the editor loaded.
     */
    protected onLoaded(editorWindow: Window): void {
        this.#editorWindow = editorWindow;
        this.#dirty.set(false);

        const search = new URLSearchParams(editorWindow.location?.search ?? '');

        this.#followLanguage(search);

        if (this.#restoreInFlight) {
            this.#restoreInFlight = false;
            this.#reportSaved(search.get('inode'));
        }

        // The editor calls this hook, not an `ng-event`, when the content it saved no longer
        // exists (`deleted-contenlet`, sic), right before its generic `close`.
        const hook: DotLegacyEditorHook = {
            next: (detail) => this.#zone.run(() => this.#handleHook(detail))
        };
        (
            editorWindow as Window & { ngEditContentletEvents?: DotLegacyEditorHook }
        ).ngEditContentletEvents = hook;
    }

    /**
     * Reacts to the legacy editor events this panel needs and ignores every other one.
     *
     * @param detail The event the legacy editor dispatched.
     */
    protected onLegacyEvent(detail: DotLegacyScreenEvent | undefined): void {
        switch (detail?.name) {
            case 'close': {
                // The first save of a new page closes the editor and names the page to open.
                const redirectUrl = detail.data?.['redirectUrl'];

                if (typeof redirectUrl === 'string' && redirectUrl) {
                    this.pageEditorRequested.emit({
                        url: redirectUrl,
                        languageId: Number(detail.data?.['languageId'])
                    });
                } else {
                    this.#panel.close();
                }

                break;
            }

            case 'deleted-page':
                this.deleted.emit();
                this.#panel.close();
                break;

            case 'workflow-wizard':
                this.#workflowEventHandler.open(
                    detail.data as unknown as DotCMSWorkflowActionEvent
                );
                break;

            case 'compare-contentlet':
                // The admin app mounts the compare dialog on every route and listens for this.
                this.#events.notify('compare-contentlet', detail.data);
                break;

            case 'push-publish':
                this.#pushPublishDialog.open(detail.data as unknown as DotPushPublishDialogData);
                break;

            case 'save-page': {
                this.#dirty.set(false);
                const payload =
                    detail.payload && typeof detail.payload === 'object' ? detail.payload : {};
                this.#reportSaved(stringOrNull(payload['contentletInode']));
                break;
            }

            case 'edit-contentlet-data-updated':
                this.#dirty.set(detail.payload === true);
                break;

            case 'edit-contentlet':
                this.openContent.emit({
                    request: withoutUnset({
                        mode: 'edit',
                        inode: stringOrUndefined(detail.data?.['inode']),
                        contentType: stringOrUndefined(detail.data?.['contentType'])
                    }),
                    source: 'block-editor'
                });
                break;

            case 'create-contentlet':
                // The screen's own `url` is ignored: the opener resolves the create screen itself.
                this.openContent.emit({
                    request: withoutUnset({
                        mode: 'new',
                        contentType: stringOrUndefined(detail.data?.['contentType']),
                        folderPath: stringOrUndefined(detail.data?.['folderPath'])
                    }),
                    source: 'block-editor'
                });
                break;
        }
    }

    /**
     * Handles a call through the window hook. Only a removed contentlet matters; it is reported
     * as `deleted`, never as a save, and the `close` the editor sends next closes the panel.
     *
     * @param detail What the editor passed to the hook.
     */
    #handleHook(detail: DotLegacyScreenEvent | undefined): void {
        if (detail?.name === 'deleted-contenlet') {
            this.deleted.emit();
        }
    }

    /**
     * Reports a save with the full contentlet, fetched by the inode the editor saved or restored,
     * so every listener gets the same shape the new editor reports.
     *
     * @param inode The saved version's inode; nothing is reported without one.
     */
    #reportSaved(inode: string | null): void {
        if (!inode) {
            return;
        }

        this.#contentletService
            .getContentletByInode(inode)
            .pipe(takeUntilDestroyed(this.#destroyRef))
            .subscribe({
                next: (contentlet) => this.saved.emit(contentlet),
                error: (error: HttpErrorResponse) => this.#httpErrorManager.handle(error)
            });
    }

    /**
     * Follows a language switch inside the legacy editor. Its language selector reloads the
     * editor with `lang=<id>` (`edit_contentlet_basic_properties.jsp`, `changeLanguage`), so the
     * language is read off each reload.
     *
     * @param search The query string the editor reloaded with.
     */
    #followLanguage(search: URLSearchParams): void {
        const languageId = Number(search.get('lang'));

        if (Number.isInteger(languageId) && languageId > 0 && languageId !== this.#languageId()) {
            this.#languageId.set(languageId);
            this.languageChanged.emit(languageId);
        }
    }

    /**
     * Calls a function on the legacy editor's window, when the editor has it. A call for a function
     * this editor does not define is meant for some other frame and is ignored.
     *
     * @param call The function to run and its arguments.
     */
    #runInEditor({ name, args = [] }: DotFunctionInfo): void {
        const editorWindow = this.#editorWindow as unknown as Record<string, unknown> | null;
        const fn = editorWindow?.[name];

        if (typeof fn === 'function') {
            // Bring Back reloads the editor and sends no event; remember it for that reload.
            if (name === 'getVersionBack') {
                this.#restoreInFlight = true;
            }
            this.#zone.run(() => fn.apply(editorWindow, args));
        }
    }
}

/**
 * The create screen for a `new` request: the URL the opener resolved from the server, with the
 * folder pre-selected. The panel never asks the server itself, so a type the server refuses never
 * opens a panel.
 *
 * @param request What to open.
 * @param createUrl The create screen the opener resolved.
 * @returns The URL, or `null` when the request names no type or no screen was resolved.
 */
function buildCreateUrl(request: DotEditContentRequest, createUrl: string | null): string | null {
    if (!request.contentType || !createUrl) {
        return null;
    }

    return withFolder(createUrl, request.folderInode);
}

/**
 * Builds the legacy edit screen URL for an inode, the same screen the UVE and the full-page editor
 * load, with the experiment variant and the site when the request names them.
 *
 * @param request What to open.
 * @returns The URL, or `null` when the request names no inode to edit.
 */
function buildEditUrl(request: DotEditContentRequest | null): string | null {
    if (request?.mode !== 'edit' || !request.inode) {
        return null;
    }

    const params = new URLSearchParams({
        p_p_id: 'content',
        p_p_action: '1',
        p_p_state: 'maximized',
        p_p_mode: 'view',
        _content_struts_action: '/ext/contentlet/edit_contentlet',
        _content_cmd: 'edit',
        inode: request.inode,
        angularCurrentPortlet: request.portletId || DEFAULT_PORTLET
    });

    if (request.variantName) {
        params.set('variantName', request.variantName);
    }

    if (request.siteId) {
        params.set('host_id', request.siteId);
    }

    return `${LAYOUT_URL}?${params.toString()}`;
}

/**
 * Pre-selects the folder on the legacy create screen, which reads it from a `folder=<inode>` param,
 * as the full-page create does.
 *
 * @param url The create screen URL the server named.
 * @param folderInode The folder the new content goes in, if any.
 * @returns The URL with the folder appended.
 */
function withFolder(url: string, folderInode: string | undefined): string {
    if (!folderInode) {
        return url;
    }

    const separator = url.includes('?') ? '&' : '?';

    return `${url}${separator}folder=${encodeURIComponent(folderInode)}`;
}

/**
 * Drops the keys a legacy event left unset, so a request built from one names only what the event
 * actually said.
 *
 * @param request The request, possibly with `undefined` values.
 * @returns The same request without them.
 */
function withoutUnset(request: DotEditContentRequest): DotEditContentRequest {
    return Object.fromEntries(
        Object.entries(request).filter(([, value]) => value !== undefined)
    ) as unknown as DotEditContentRequest;
}

/** A legacy event value as a non-empty string, or `undefined`. */
function stringOrUndefined(value: unknown): string | undefined {
    return typeof value === 'string' && value ? value : undefined;
}

/** A legacy event value as a non-empty string, or `null`. */
function stringOrNull(value: unknown): string | null {
    return stringOrUndefined(value) ?? null;
}
