import {
    ChangeDetectionStrategy,
    Component,
    ElementRef,
    NgZone,
    OnDestroy,
    afterNextRender,
    computed,
    effect,
    inject,
    input,
    linkedSignal,
    output,
    signal,
    untracked,
    viewChild
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

import { ConfirmationService, ConfirmEventType } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { Drawer, DrawerModule } from 'primeng/drawer';

import { take } from 'rxjs/operators';

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
import {
    DotCMSWorkflowActionEvent,
    DotFunctionInfo,
    DotPushPublishDialogData
} from '@dotcms/dotcms-models';
import { DotSidePanelNavController } from '@dotcms/edit-content';
import { DotKeyboardShortcutService, DotMessagePipe, hasOverlayAbove } from '@dotcms/ui';
import { isSameOriginRelativeUrl } from '@dotcms/utils';

import {
    DotLegacyEditorPageRequest,
    DotLegacyEditorRequest,
    DotLegacyEditorSaved
} from './dot-legacy-editor-side-panel.model';

/** The admin route every legacy portlet screen is served from. */
const LAYOUT_URL = '/c/portal/layout';

/**
 * Shared with the new-editor side panel on purpose, so the author's expanded (full-width) choice
 * carries over between the two panels.
 */
const EXPANDED_STORAGE_KEY = 'dot-edit-content-side-panel-expanded';

/** Best-effort read of the expanded preference; `false` when storage is unavailable. */
function readExpandedPreference(): boolean {
    try {
        return localStorage.getItem(EXPANDED_STORAGE_KEY) === 'true';
    } catch {
        return false;
    }
}

/** Persists the expanded preference; storage failures must not break the panel. */
function writeExpandedPreference(expanded: boolean): void {
    try {
        localStorage.setItem(EXPANDED_STORAGE_KEY, String(expanded));
    } catch {
        // best-effort: quota errors / disabled storage are ignored.
    }
}

/** The shape of the `ng-event` CustomEvents the legacy editor dispatches on its own document. */
interface LegacyEditorEventDetail {
    name: string;
    /** A map for `save-page`, a boolean (dirty) for `edit-contentlet-data-updated`. */
    payload?: Record<string, unknown> | boolean;
    data?: Record<string, unknown>;
}

/**
 * Hosts the legacy (JSP) content editor in a side panel over Content Drive (#37759).
 *
 * It looks and behaves like the new-editor side panel (`DotEditContentSidePanelComponent`): the
 * same drawer, width toggle, mask and close rules. It is kept separate from it so that removing the
 * legacy editor later means deleting this folder and the shell's routing branch, nothing else.
 *
 * The panel never navigates. It handles the legacy editor events that need no decision from the
 * opener, and reports the rest through its outputs, so Content Drive decides what a save or a close
 * means for the list and the URL.
 */
@Component({
    selector: 'dot-legacy-editor-side-panel',
    imports: [DrawerModule, ButtonModule, ConfirmDialogModule, DotMessagePipe],
    // Its own instance, so the prompt renders in this panel's `<p-confirmDialog>` and never in
    // another one mounted elsewhere on the page.
    providers: [ConfirmationService],
    templateUrl: './dot-legacy-editor-side-panel.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    // Bound at document level because `appendTo="body"` moves the drawer and its mask out of this
    // component's DOM, so a template listener would never hear the mask click.
    host: {
        '(document:click)': 'onMaskClick($event)'
    }
})
export class DotLegacyEditorSidePanelComponent implements OnDestroy {
    readonly #navController = inject(DotSidePanelNavController);
    readonly #shortcuts = inject(DotKeyboardShortcutService);
    readonly #uiColors = inject(DotUiColorsService);
    readonly #sanitizer = inject(DomSanitizer);
    readonly #zone = inject(NgZone);
    readonly #actionUrl = inject(DotActionUrlService);
    readonly #httpErrorManager = inject(DotHttpErrorManagerService);
    // Root (admin app) instances, on purpose: the workflow wizard and push publish dialogs are
    // mounted once for every admin route and listen to these. Never provide them on this component.
    readonly #workflowEventHandler = inject(DotWorkflowEventHandlerService);
    readonly #pushPublishDialog = inject(DotPushPublishDialogService);
    readonly #iframeService = inject(DotIframeService);
    readonly #events = inject(DotEventsService);
    readonly #confirmation = inject(ConfirmationService);
    readonly #messages = inject(DotMessageService);

    /** What to open, or `null` when the panel is closed. */
    readonly request = input<DotLegacyEditorRequest | null>(null);

    /** The legacy editor saved: the opener refreshes its view. */
    readonly saved = output<DotLegacyEditorSaved>();

    /** The panel closed, for any reason: the opener clears its request and refreshes. */
    readonly closed = output<void>();

    /**
     * The legacy editor asked to open a page in the page editor: the first save of a new page. The
     * opener closes the panel and opens it, as Content Search does.
     */
    readonly pageEditorRequested = output<DotLegacyEditorPageRequest>();

    /**
     * Bring Back restored an older version: the editor reloaded with it and sent no save, so the
     * opener refreshes its view.
     */
    readonly versionRestored = output<void>();

    /**
     * The author switched language inside the legacy editor, which reloads itself in that
     * language and sends no event. The opener updates the URL (#37759, FR-020).
     */
    readonly languageChanged = output<number>();

    /** This panel's own drawer, so the mask-click check compares against ITS mask only. */
    protected readonly $drawer = viewChild(Drawer);

    /** The iframe the legacy editor loads in. */
    protected readonly $iframe = viewChild<ElementRef<HTMLIFrameElement>>('iframe');

    /**
     * Whether the author changed something since the last save. Set from the legacy editor's own
     * `edit-contentlet-data-updated` event and cleared by a save or a reload (FR-018).
     */
    readonly #dirty = signal(false);

    /**
     * The language the editor is showing: the request's, until the author switches language
     * inside the editor. Reset whenever a new request opens.
     */
    readonly #languageId = linkedSignal(() => this.request()?.languageId ?? 0);

    /** Whether the panel is expanded to the full width (vs 80%), seeded from the stored choice. */
    protected readonly $expanded = signal(readExpandedPreference());

    /**
     * The create screen for a `new` request, once the server has named it. The server builds it
     * (`/api/v1/portlet/_actionurl/<type>`), so it arrives asynchronously.
     */
    readonly #createUrl = signal<string | null>(null);

    /** The legacy editor URL for the request, or `null` when there is nothing valid to load. */
    protected readonly $url = computed<SafeResourceUrl | null>(() => {
        const request = this.request();
        const url = request?.mode === 'new' ? this.#createUrl() : this.#buildEditUrl(request);

        return isSameOriginRelativeUrl(url)
            ? this.#sanitizer.bypassSecurityTrustResourceUrl(url)
            : null;
    });

    /** Withdraws the Escape claim; called on destroy. */
    readonly #withdrawShortcuts: () => void;

    /**
     * A Bring Back is under way: the editor was asked for `getVersionBack` and will reload with the
     * restored version. Reported once that reload lands.
     */
    #restoreInFlight = false;

    /** Removes the listeners attached to the current iframe document; replaced on every load. */
    #detachFrame: () => void = () => undefined;

    constructor() {
        // ESC goes through the shared shortcut registry, as in the new-editor panel, so the portlet
        // behind does not act on the same key while the panel is open.
        this.#withdrawShortcuts = this.#shortcuts.register({
            combination: 'escape',
            label: 'edit.content.side-panel.shortcut.close',
            handler: () => this.#onEscape()
        });

        // Join the side-panel stack (and collapse the main nav) after the current render.
        afterNextRender(() => this.#navController.acquire(this));

        // Other admin code asks the legacy editor to run one of its functions through this service:
        // the workflow wizard hands its answer back with `saveAssignCallBackAngular`, which fills
        // the workflow fields and saves (FR-009). The full-page editor's iframe answers it the same
        // way (`IframeComponent`).
        this.#iframeService
            .ran()
            .pipe(takeUntilDestroyed())
            .subscribe((call) => this.#runInEditor(call));

        // A create asks the server for the type's create screen, in the language the request
        // starts in, then pre-selects the folder on it.
        effect((onCleanup) => {
            const request = this.request();

            untracked(() => {
                this.#createUrl.set(null);

                if (request?.mode !== 'new' || !request.contentTypeVariable) {
                    return;
                }

                const subscription = this.#actionUrl
                    .getCreateContentletUrl(request.contentTypeVariable, request.languageId)
                    .pipe(take(1))
                    .subscribe({
                        next: (url) => this.#createUrl.set(withFolder(url, request.folderInode)),
                        error: (error) => {
                            // Nothing to show: report it the standard way and let the opener close.
                            this.#httpErrorManager.handle(error);
                            this.closed.emit();
                        }
                    });

                onCleanup(() => subscription.unsubscribe());
            });
        });
    }

    /**
     * Close intent: the X button, ESC, a mask click, or browser Back routed by the opener.
     *
     * With unsaved changes, it asks first, with the new-editor side panel's prompt and wording
     * (FR-017). Public so the opener can route browser Back through the same prompt as the panel's
     * own controls (FR-019).
     */
    requestClose(): void {
        if (!this.#dirty()) {
            this.closed.emit();

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
                    this.closed.emit();
                }
            }
        });
    }

    /**
     * Runs every time the iframe loads a document. The legacy editor replaces its whole document on
     * a reload (a language switch, a restored version), so listeners are re-attached each time.
     */
    protected onIframeLoad(): void {
        this.#detachFrame();
        // A new document holds none of the changes the old one had.
        this.#dirty.set(false);

        if (this.#restoreInFlight) {
            this.#restoreInFlight = false;
            this.versionRestored.emit();
        }

        const editorWindow = this.$iframe()?.nativeElement.contentWindow;
        const doc = editorWindow?.document;

        this.#followLanguage(editorWindow?.location?.search);

        if (!doc) {
            this.#detachFrame = () => undefined;

            return;
        }

        const html = doc.querySelector('html');
        if (html) {
            this.#uiColors.setColors(html);
        }

        const onLegacyEvent = (event: Event) =>
            this.#zone.run(() =>
                this.#handleLegacyEvent((event as CustomEvent<LegacyEditorEventDetail>).detail)
            );
        // A keydown inside the iframe never reaches the admin page, so the shortcut registry cannot
        // see it: forward Escape ourselves.
        const onKeydown = (event: KeyboardEvent) => {
            if (event.key === 'Escape') {
                this.#zone.run(() => this.#onEscape());
            }
        };

        doc.addEventListener('ng-event', onLegacyEvent);
        doc.addEventListener('keydown', onKeydown);

        this.#detachFrame = () => {
            doc.removeEventListener('ng-event', onLegacyEvent);
            doc.removeEventListener('keydown', onKeydown);
        };
    }

    /**
     * Click-outside handler. Matches this drawer's own mask by identity, not by class, so a click on
     * another drawer's mask never closes this panel. Only the frontmost side panel reacts.
     */
    protected onMaskClick(event: MouseEvent): void {
        if (event.target !== this.$drawer()?.mask) {
            return;
        }

        if (this.#navController.isTop(this)) {
            this.requestClose();
        }
    }

    /** Toggles the full-width state and remembers it for the next panel. */
    protected toggleExpanded(): void {
        const next = !this.$expanded();
        this.$expanded.set(next);
        writeExpandedPreference(next);
    }

    /** Drops the iframe listeners, leaves the side-panel stack and withdraws the Escape claim. */
    ngOnDestroy(): void {
        this.#detachFrame();
        this.#navController.release(this);
        this.#withdrawShortcuts();
    }

    /**
     * Reacts to the legacy editor events this panel needs and ignores every other one (FR-008).
     *
     * @param detail The event the legacy editor dispatched.
     */
    #handleLegacyEvent(detail: LegacyEditorEventDetail | undefined): void {
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
                    this.closed.emit();
                }

                break;
            }

            case 'deleted-page':
                this.closed.emit();
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

            case 'save-page':
                this.#dirty.set(false);
                this.saved.emit(
                    this.#savedFrom(typeof detail.payload === 'object' ? detail.payload : {})
                );
                break;

            case 'edit-contentlet-data-updated':
                this.#dirty.set(detail.payload === true);
                break;
        }
    }

    /**
     * Follows a language switch inside the legacy editor. Its language selector reloads the
     * editor with `lang=<id>` (`edit_contentlet_basic_properties.jsp`, `changeLanguage`), so the
     * language is read off each reload.
     *
     * @param search The query string the editor reloaded with.
     */
    #followLanguage(search: string | undefined): void {
        const languageId = Number(new URLSearchParams(search ?? '').get('lang'));

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
        const editorWindow = this.$iframe()?.nativeElement.contentWindow as unknown as
            | Record<string, unknown>
            | null
            | undefined;
        const fn = editorWindow?.[name];

        if (typeof fn === 'function') {
            // Bring Back reloads the editor and sends no event; remember it for that reload.
            if (name === 'getVersionBack') {
                this.#restoreInFlight = true;
            }
            this.#zone.run(() => fn.apply(editorWindow, args));
        }
    }

    /**
     * Reads what was saved from the `save-page` payload. The payload carries no top-level language,
     * so the language is the one the panel is showing; failing that, the language of the version
     * the save wrote, from the payload's list of language versions.
     *
     * @param payload The legacy editor's save callback map.
     * @returns The saved content's identifier, inode and language.
     */
    #savedFrom(payload: Record<string, unknown>): DotLegacyEditorSaved {
        const inode = String(payload['contentletInode'] ?? '');
        const versions = Array.isArray(payload['allLangContentlets'])
            ? (payload['allLangContentlets'] as { inode?: string; languageId?: number }[])
            : [];

        return {
            identifier: String(payload['contentletIdentifier'] ?? ''),
            inode,
            languageId:
                this.#languageId() ||
                Number(versions.find((version) => version.inode === inode)?.languageId) ||
                0
        };
    }

    /**
     * ESC handler. Always consumes the key while the panel is open, so it never falls through to the
     * portlet behind; only closes when no overlay sits above and this is the frontmost panel.
     *
     * @returns `true`: the key is consumed.
     */
    #onEscape(): boolean {
        if (hasOverlayAbove(this.$drawer()?.container)) {
            return true;
        }

        if (this.#navController.isTop(this)) {
            this.requestClose();
        }

        return true;
    }

    /**
     * Builds the legacy edit screen URL for an inode, the same screen UVE and the full-page editor
     * load.
     *
     * @param request What to open.
     * @returns The URL, or `null` when the request names no inode to edit.
     */
    #buildEditUrl(request: DotLegacyEditorRequest | null): string | null {
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
            angularCurrentPortlet: request.portletId
        });

        return `${LAYOUT_URL}?${params.toString()}`;
    }
}

/**
 * Pre-selects the folder on the legacy create screen, which reads it from a `folder=<inode>` param,
 * as the full-page create does (`DotCreateContentletResolver`).
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
