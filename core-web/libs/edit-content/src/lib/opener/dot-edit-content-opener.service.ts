import { Observable, of } from 'rxjs';

import { HttpErrorResponse, HttpStatusCode } from '@angular/common/http';
import {
    Injectable,
    Injector,
    Signal,
    Type,
    computed,
    inject,
    isSignal,
    signal,
    untracked
} from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { map, switchMap, takeUntil, tap } from 'rxjs/operators';

import {
    DotActionUrlService,
    DotContentEditor,
    DotContentEditorChoice,
    DotContentletEditUrlService,
    DotContentletService,
    DotHttpErrorManagerService
} from '@dotcms/data-access';
import { DotCMSContentlet, DotCMSContentType } from '@dotcms/dotcms-models';
import { DotSidePanelRef, DotSidePanelService } from '@dotcms/ui';

import { DotPanelUrlStateService } from './url/dot-panel-url-state.service';
import { DOT_PANEL_URL_PARAM } from './url/read-panel-url-intent';

import { loadDotEditContentSidePanel } from '../components/dot-edit-content-side-panel/load-dot-edit-content-side-panel';
import { loadDotLegacyEditorSidePanel } from '../components/dot-legacy-editor-side-panel/load-dot-legacy-editor-side-panel';
import { DotEditContentPanelHandlers } from '../models/dot-edit-content-panel-events.model';
import { DotEditContentRequest } from '../models/dot-edit-content-request.model';
import { DotOpenContentEvent } from '../models/dot-open-content-event.model';

/** How to open a panel: the opener's injector, and handlers for any of the panel events. */
export interface DotEditContentOpenOptions extends DotEditContentPanelHandlers {
    /**
     * The opener's injector. The panel resolves its dependencies through it (MessageService for
     * toasts, route-level providers) and closes when it is destroyed. Required.
     */
    injector: Injector;

    /**
     * Shows the legacy editor whatever the content type says, without asking which editor it uses.
     * Only for "switch to the old editor", which reopens content whose type was just set back to
     * the legacy editor while the type's old choice is still remembered. Every other caller leaves
     * the choice to the content type. Remove with the legacy editor.
     */
    editor?: 'legacy';

    /**
     * Writes the open panel to the surface's URL, so it can be shared, survives a refresh and
     * closes with browser Back (#38303, FR-044). Every surface except the page editor sets it.
     */
    url?: DotEditContentUrlOption;
}

/** How a panel is written to the surface's URL. */
export interface DotEditContentUrlOption {
    /** The surface's route. */
    route: ActivatedRoute;
    /**
     * Params written beside the panel's own and cleared with them, such as Content Drive's folder
     * for a create. Not passed on to the panels stacked from inside this one.
     */
    extraParams?: Record<string, string>;
}

/** What the server said about a request before any editor shows. */
interface DotResolvedRequest {
    editor: DotContentEditor;
    /** The content type the editor decision was read from; `null` when the lookup failed. */
    contentType: DotCMSContentType | null;
    /** The contentlet fetched for an edit that named no content type. */
    contentlet: DotCMSContentlet | null;
    /** The create screen for a legacy create. */
    createUrl: string | null;
}

/** The panel events a surface can handle, by the output name both panels use for each. */
const HANDLED_OUTPUTS = ['saved', 'languageChanged', 'pageEditorRequested', 'deleted'] as const;

/**
 * Opens content in the side panel, in whichever editor its content type uses (#38303). Every
 * surface that edits or creates content goes through here, with one request shape, and hears back
 * through the same five events whichever editor showed. Surfaces that keep a panel open for as long
 * as a request is set use {@link injectEditContentPanel} on top of it.
 */
@Injectable({ providedIn: 'root' })
export class DotEditContentOpenerService {
    readonly #sidePanels = inject(DotSidePanelService);
    readonly #editUrl = inject(DotContentletEditUrlService);
    readonly #actionUrl = inject(DotActionUrlService);
    readonly #contentlets = inject(DotContentletService);
    readonly #httpErrorManager = inject(DotHttpErrorManagerService);
    readonly #urlState = inject(DotPanelUrlStateService);

    /**
     * Opens the side panel for a request. Everything server-side is resolved first (the content type
     * of an edit that named none, the editor, the create screen of a legacy create), while the
     * panel's spinner shows; a refused request shows the standard error and no editor. An invalid
     * request opens nothing.
     *
     * Pass a signal to keep the heading and the panel's request following it while the panel stays
     * open; the editor itself is chosen once, from the request as it is now. `options.editor` skips
     * that choice and shows the legacy editor.
     *
     * With `options.url` the panel is written to the surface's URL: a create at once, an edit as soon
     * as its identifier and language are known, which may take fetching the contentlet; the URL
     * follows a language switch and a create's first save, and panels stacked from inside this one
     * are written too.
     *
     * @param request What to open, or a signal of it.
     * @param options The opener's injector and the events it handles.
     * @returns The panel, so the caller can route browser Back through `requestClose()`. Already
     * closed for an invalid request.
     */
    open(
        request: DotEditContentRequest | Signal<DotEditContentRequest>,
        options: DotEditContentOpenOptions
    ): DotSidePanelRef {
        const $source = isSignal(request) ? request : signal(request).asReadonly();
        const initial = untracked($source);

        if (!isOpenable(initial)) {
            return this.#refuse(options);
        }

        const $contentlet = signal<DotCMSContentlet | null>(null);
        const $typeName = signal<string | null>(null);
        const $request = computed(() => withContentlet($source(), $contentlet()));

        // Read by the outlet only once the content has loaded, so the inputs a given editor takes
        // are added below, before its component is handed over.
        const inputs: Record<string, unknown> = { request: $request };

        let showEditor: (component: Promise<Type<unknown>>) => void = () => undefined;
        const component = new Promise<Type<unknown>>((resolve) => (showEditor = resolve));

        const url = options.url
            ? new DotPanelUrlTracker(this.#urlState, options.url, initial.mode)
            : null;

        const ref = this.#sidePanels.open(component, {
            heading: computed(
                () => $request().title || $typeName() || $request().contentType || ''
            ),
            inputs,
            outputs: this.#outputs(options, url),
            injector: options.injector
        });

        ref.closed.subscribe(() => options.closed?.());

        // An edit with no title is fetched for its heading: the legacy JSP events and the Publishing
        // Queue name the type and no title. The URL needs the identifier of an edit, so with the
        // option a request that lacks it is fetched even when it names its content type.
        const fetch =
            initial.mode === 'edit' &&
            (!initial.contentType || !initial.title || (!!url && !initial.identifier));

        this.#fetchContentlet(initial, fetch)
            .pipe(
                tap((contentlet) => url?.register(ref, withContentlet(initial, contentlet))),
                switchMap((contentlet) => this.#resolve(initial, contentlet, options.editor)),
                takeUntil(ref.closed)
            )
            .subscribe({
                next: ({ editor, contentType, contentlet, createUrl }) => {
                    $contentlet.set(contentlet);
                    $typeName.set(contentType?.name ?? null);

                    if (editor === 'new') {
                        showEditor(loadDotEditContentSidePanel());

                        return;
                    }

                    inputs['createUrl'] = signal(createUrl).asReadonly();
                    showEditor(loadDotLegacyEditorSidePanel());
                },
                // The component never resolves: the outlet would skip it on a closed panel anyway.
                error: (error: HttpErrorResponse) => {
                    this.#httpErrorManager.handle(error);
                    ref.close();
                }
            });

        return ref;
    }

    /**
     * Turns down a request that names nothing to open: the standard error, and a panel that is
     * already closed, so the caller's `closed` still fires once.
     *
     * @param options The caller's handlers.
     * @returns A closed panel ref.
     */
    #refuse(options: DotEditContentOpenOptions): DotSidePanelRef {
        this.#httpErrorManager.handle(new HttpErrorResponse({ status: HttpStatusCode.BadRequest }));

        const ref = new DotSidePanelRef(() => undefined);
        ref.closed.subscribe(() => options.closed?.());
        ref.close();

        return ref;
    }

    /**
     * The contentlet an edit needs fetched before anything else: when it names no content type, no
     * title for the heading, or no identifier for the URL.
     *
     * @param request A request that passed {@link isOpenable}.
     * @param fetch Whether to fetch it.
     * @returns The contentlet, or `null` when none was needed; errors when the fetch is refused.
     */
    #fetchContentlet(
        request: DotEditContentRequest,
        fetch: boolean
    ): Observable<DotCMSContentlet | null> {
        return fetch ? this.#contentlets.getContentletByInode(request.inode as string) : of(null);
    }

    /**
     * Asks the server the rest of what the request needs before an editor shows: which editor the
     * type uses, unless the caller forced the legacy one, and the create screen for a legacy create.
     *
     * @param request A request that passed {@link isOpenable}.
     * @param contentlet The contentlet fetched for it, if one was.
     * @param forced The editor the caller forced, if any.
     * @returns The answers, once; errors when the create screen is refused.
     */
    #resolve(
        request: DotEditContentRequest,
        contentlet: DotCMSContentlet | null,
        forced: 'legacy' | undefined
    ): Observable<DotResolvedRequest> {
        const variable = (request.contentType || contentlet?.contentType) as string;
        const choice$: Observable<DotContentEditorChoice> =
            forced === 'legacy'
                ? of({ editor: 'legacy', contentType: null })
                : this.#editUrl.resolveEditor(variable);

        return choice$.pipe(
            switchMap(({ editor, contentType }) => {
                const createUrl$: Observable<string | null> =
                    editor === 'legacy' && request.mode === 'new'
                        ? this.#actionUrl.getCreateContentletUrl(variable, request.languageId)
                        : of(null);

                return createUrl$.pipe(
                    map((createUrl) => ({ editor, contentType, contentlet, createUrl }))
                );
            })
        );
    }

    /**
     * The panel's output handlers: the caller's, by the output name of each event, and
     * `openContent`, which stacks a panel for the content a panel asked for, routing that panel's
     * saves to the asker's reply. The stacked panel opens under the asking panel's own injector,
     * which the panel puts on the event, so it belongs to that panel and closes with it
     * (open-content-from-fields.md); an event without one stacks under the caller's. `closed` is
     * not an output here: it is taken from the panel ref, so it fires even when no editor loaded.
     *
     * With the URL option a language switch and a save also go to the URL, and the stacked panel is
     * written to the URL too, without this panel's extra params.
     *
     * @param options The caller's injector and handlers.
     * @param url The panel's name in the URL, when it is written there.
     * @returns The handlers, by output name.
     */
    #outputs(
        options: DotEditContentOpenOptions,
        url: DotPanelUrlTracker | null
    ): Record<string, (value: never) => void> {
        const outputs: Record<string, (value: never) => void> = {
            openContent: (event: DotOpenContentEvent) =>
                this.open(event.request, {
                    injector: event.injector ?? options.injector,
                    saved: event.onSaved,
                    url: options.url && { route: options.url.route }
                })
        };

        for (const name of HANDLED_OUTPUTS) {
            const handler = options[name];

            if (handler) {
                outputs[name] = handler;
            }
        }

        if (url) {
            outputs['languageChanged'] = (languageId: number) => {
                url.languageChanged(languageId);
                options.languageChanged?.(languageId);
            };
            outputs['saved'] = (contentlet: DotCMSContentlet) => {
                url.saved(contentlet);
                options.saved?.(contentlet);
            };
        }

        return outputs;
    }
}

/**
 * One panel's name in the surface's URL, kept in step with the panel through
 * {@link DotPanelUrlStateService}: `editContent` and `editContentLang` for an edit, `createContent`
 * for a create, with the surface's extra params beside them. The service forgets the panel when it
 * closes.
 */
class DotPanelUrlTracker {
    readonly #urlState: DotPanelUrlStateService;
    readonly #extraParams: Record<string, string>;
    readonly #mode: DotEditContentRequest['mode'];
    #ref: DotSidePanelRef | null = null;
    /** The content the URL names: an edit's from the start, a create's from its first save. */
    #identifier: string | null = null;

    constructor(
        urlState: DotPanelUrlStateService,
        option: DotEditContentUrlOption,
        mode: DotEditContentRequest['mode']
    ) {
        this.#urlState = urlState;
        this.#extraParams = option.extraParams ?? {};
        this.#mode = mode;
    }

    /**
     * Writes the panel to the URL. An edit whose identifier is still unknown is not written.
     *
     * @param ref The panel.
     * @param request The request, completed from the contentlet fetched for it.
     */
    register(ref: DotSidePanelRef, request: DotEditContentRequest): void {
        if (request.mode === 'new') {
            this.#ref = ref;
            this.#urlState.register(ref, {
                [DOT_PANEL_URL_PARAM.CREATE_CONTENT]: request.contentType as string,
                ...this.#extraParams
            });

            return;
        }

        if (!request.identifier) {
            return;
        }

        this.#ref = ref;
        this.#identifier = request.identifier;
        this.#urlState.register(ref, this.#editParams(request.identifier, request.languageId));
    }

    /** The author switched language: the URL names that version, in place. */
    languageChanged(languageId: number): void {
        if (this.#ref && this.#identifier) {
            this.#urlState.update(this.#ref, this.#editParams(this.#identifier, languageId));
        }
    }

    /** A create's first save makes it an edit: the URL names the new content, in place. */
    saved(contentlet: DotCMSContentlet): void {
        if (!this.#ref || this.#mode !== 'new' || this.#identifier || !contentlet.identifier) {
            return;
        }

        this.#identifier = contentlet.identifier;
        this.#urlState.update(
            this.#ref,
            this.#editParams(contentlet.identifier, contentlet.languageId)
        );
    }

    #editParams(identifier: string, languageId: number | undefined): Record<string, string> {
        return {
            [DOT_PANEL_URL_PARAM.EDIT_CONTENT]: identifier,
            ...(languageId ? { [DOT_PANEL_URL_PARAM.EDIT_CONTENT_LANG]: String(languageId) } : {}),
            ...this.#extraParams
        };
    }
}

/**
 * Whether a request names something to open: an inode to edit, or a content type to create.
 *
 * @param request The request.
 */
function isOpenable(request: DotEditContentRequest): boolean {
    return request.mode === 'edit' ? !!request.inode : !!request.contentType;
}

/**
 * Fills what an edit request left out from the contentlet fetched for it, keeping everything the
 * request did say.
 *
 * @param request The request.
 * @param contentlet The contentlet fetched for it, if one was.
 * @returns The request, completed.
 */
function withContentlet(
    request: DotEditContentRequest,
    contentlet: DotCMSContentlet | null
): DotEditContentRequest {
    if (!contentlet) {
        return request;
    }

    return {
        ...request,
        contentType: request.contentType ?? contentlet.contentType,
        identifier: request.identifier ?? contentlet.identifier,
        // `||`, as the fetch rule reads it: an empty title is no heading.
        title: request.title || contentlet.title,
        languageId: request.languageId ?? contentlet.languageId
    };
}
