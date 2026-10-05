import { EMPTY, Observable, of } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { Injectable, Provider, computed, inject, signal } from '@angular/core';

import { catchError, map, switchMap, take } from 'rxjs/operators';

import {
    DotActionUrlService,
    DotContentSearchService,
    DotContentTypeService,
    DotHttpErrorManagerService,
    DotRouterService
} from '@dotcms/data-access';
import {
    DotCMSBaseTypesContentTypes,
    DotCMSContentlet,
    DotCMSContentType,
    FeaturedFlags
} from '@dotcms/dotcms-models';
import {
    EDIT_CONTENT_NAVIGATION_OVERRIDE,
    EditContentDialogData,
    EditContentIdentity,
    EditContentNavigationOverride
} from '@dotcms/edit-content';
import { DotFolderTreeNodeContentData } from '@dotcms/portlets/content-drive/ui';

import { DotLegacyEditorRequest } from '../../components/dot-legacy-editor-side-panel/dot-legacy-editor-side-panel.model';
import { DotContentDriveStore } from '../../store/dot-content-drive.store';
import { SYSTEM_HOST } from '../constants';
import { DotContentDrivePanelLocation, DotContentDrivePanelRequest } from '../models';

/** Shape of the `/api/content/_search` entity we read the resolved contentlet from. */
interface ContentSearchEntity {
    jsonObjectView: { contentlets: DotCMSContentlet[] };
}

/** Sent to the legacy editor as `angularCurrentPortlet`, so it knows which portlet hosts it. */
const CONTENT_DRIVE_PORTLET_ID = 'content-drive';

// Provided at the Content Drive shell level (not `root`) so it can inject the shell-scoped
// DotContentDriveStore and read the list's language filter and default language from it.
@Injectable()
export class DotContentDriveNavigationService implements EditContentNavigationOverride {
    readonly #dotContentTypeService = inject(DotContentTypeService);
    readonly #dotRouterService = inject(DotRouterService);
    readonly #httpErrorManager = inject(DotHttpErrorManagerService);
    readonly #contentSearch = inject(DotContentSearchService);
    readonly #actionUrl = inject(DotActionUrlService);
    readonly #store = inject(DotContentDriveStore);

    /**
     * What a side panel was asked to open, and in which editor, or `null` when none is open. The
     * single source both panel requests below are derived from, so only one panel is ever open.
     */
    readonly #panelRequest = signal<DotContentDrivePanelRequest | null>(null);

    /**
     * The content to show in the Edit Content (new editor) side panel, or `null` when it is
     * closed. The shell renders that panel while this is set.
     */
    readonly $editPanelRequest = computed<EditContentDialogData | null>(() => {
        const request = this.#panelRequest();

        return request?.editor === 'new' ? request.data : null;
    });

    /**
     * The content to show in the legacy-editor side panel, or `null` when it is closed. The shell
     * renders that panel while this is set.
     */
    readonly $legacyPanelRequest = computed<DotLegacyEditorRequest | null>(() => {
        const request = this.#panelRequest();

        return request?.editor === 'legacy' ? request.data : null;
    });

    readonly #panelLocation = signal<DotContentDrivePanelLocation | null>(null);

    /**
     * What the URL must say about the open panel (#37759, FR-020). Set from the request when a
     * panel opens, then free to follow the panel without touching the request, which would
     * remount it.
     */
    readonly $panelLocation = this.#panelLocation.asReadonly();
    /**
     * Navigates to the appropriate editor based on the content type.
     * Routes to the page editor for HTML pages, or the contentlet editor for other types.
     *
     * @param contentlet - The content item to edit
     */
    editContent(contentlet: DotCMSContentlet) {
        if (contentlet.baseType === DotCMSBaseTypesContentTypes.HTMLPAGE) {
            this.editPage(contentlet);
        } else {
            this.#editContentlet(contentlet);
        }
    }

    /**
     * Navigates to the edit page editor for a page contentlet.
     * Uses the contentlet's URL map or URL along with the language ID for routing.
     *
     * @param contentlet - The page content item to edit
     */
    editPage(contentlet: DotCMSContentlet) {
        const url = contentlet.urlMap || contentlet.url;

        this.#dotRouterService.goToEditPage({ url, language_id: contentlet.languageId });
    }

    /**
     * Opens the create form for a content type in the side panel of the editor that type chose:
     * the legacy panel for a type that has not opted into the new editor, the new-editor panel
     * otherwise (#37759, FR-002). Either way the author stays in Content Drive.
     *
     * The form starts in the language the list is filtered by, or Content Drive's default language
     * when there is no language filter, so the new content shows in the list once saved (FR-003).
     *
     * @param contentTypeVariable - The variable name of the content type to create
     * @param folder - Where the new content goes, usually {@link currentFolder}. `folderPath`
     * (`hostname/path`) pre-selects the Host/Folder field in the new editor; `folderInode`
     * pre-selects the target folder in the legacy editor.
     */
    createContent(
        contentTypeVariable: string,
        folder: { folderPath?: string; folderInode?: string } = {}
    ): void {
        this.#dotContentTypeService
            .getContentType(contentTypeVariable)
            .pipe(
                take(1),
                catchError((error: HttpErrorResponse) => {
                    this.#httpErrorManager.handle(error);

                    return EMPTY;
                })
            )
            .subscribe((contentType) => {
                const languageId = this.#createLanguageId();

                if (usesLegacyEditor(contentType)) {
                    // The variable the server answered with, not the one the caller (or a link)
                    // passed in. Unknown language only if the default failed to load: 1 is what
                    // the legacy create screen itself assumes when given no language.
                    this.#openLegacyCreate(contentType.variable, contentType.name, {
                        folderInode: folder.folderInode,
                        languageId: languageId ?? 1
                    });

                    return;
                }

                this.#openPanel({
                    editor: 'new',
                    data: {
                        mode: 'new',
                        contentTypeId: contentTypeVariable,
                        folderPath: folder.folderPath,
                        languageId,
                        title: contentType.name
                    }
                });
            });
    }

    /**
     * Opens the legacy create form, once the server has named its screen. Resolving it first means
     * a type the server won't serve shows the standard error and opens nothing: no panel, no
     * `createContent` in the URL, no history entry (#37759, edge case "Legacy create form cannot
     * be resolved").
     *
     * @param contentTypeVariable The type to create.
     * @param title The type's name, for the panel header.
     * @param options Where the content goes and the language it starts in.
     */
    #openLegacyCreate(
        contentTypeVariable: string,
        title: string,
        { folderInode, languageId }: { folderInode?: string; languageId: number }
    ): void {
        this.#actionUrl
            .getCreateContentletUrl(contentTypeVariable, languageId)
            .pipe(take(1))
            .subscribe({
                next: (createUrl) =>
                    this.#openPanel({
                        editor: 'legacy',
                        data: {
                            mode: 'new',
                            contentTypeVariable,
                            folderInode,
                            languageId,
                            title,
                            portletId: CONTENT_DRIVE_PORTLET_ID,
                            createUrl
                        }
                    }),
                error: (error: HttpErrorResponse) => this.#httpErrorManager.handle(error)
            });
    }

    /**
     * The folder Content Drive is showing, as a create needs it, so new content lands where the
     * author is looking. Shared by the create action and `createContent` links.
     *
     * At the site root both fall back to the current site (empty path / no inode). System Host is a
     * destination in its own right, and the site in the switcher is only context while it is
     * selected: pasting the location onto the hostname would produce `demo.dotcms.comSYSTEM_HOST`,
     * which resolves to nothing.
     *
     * @returns `folderPath` (`hostname/path`) for the new editor, `folderInode` for the legacy one.
     */
    currentFolder(): { folderPath?: string; folderInode?: string } {
        if (this.#store.$systemHostSelected()) {
            return { folderInode: SYSTEM_HOST.identifier };
        }

        const hostname = this.#store.currentSite()?.hostname;
        const path = this.#store.path();
        const data = this.#store.selectedNode()?.data;
        const inode =
            data?.type === 'folder' || data?.type === 'site'
                ? (data as DotFolderTreeNodeContentData).inode
                : undefined;

        return {
            folderPath: hostname ? `${hostname}${path ?? ''}` : undefined,
            folderInode: inode || undefined
        };
    }

    /**
     * "Switch to the old editor" in Content Drive's new-editor panel: the type was just set back to
     * the legacy editor, so the same content reopens, in the same language, in the legacy panel
     * (#37759, FR-028). The type is not looked up again. The URL already names this content,
     * unless the panel moved in place to related content, in which case it follows the switched
     * content. From a create there is no content yet: a legacy create for the
     * same type opens instead, in the folder being browsed and the language the create started in.
     *
     * Content Drive answers only for the panel it opened. Any other editor that inherits the
     * override (a related content opened from a relationship field) is declined, and navigates as
     * it does outside Content Drive.
     *
     * @param opened What the editor was opened with.
     * @param contentlet The content being edited, or `null` for a create that was never saved.
     * @param contentTypeVariable The type that was just set back to the legacy editor.
     * @returns Whether Content Drive reopened it.
     */
    switchToLegacyEditor(
        opened: EditContentIdentity,
        contentlet: DotCMSContentlet | null,
        contentTypeVariable: string
    ): boolean {
        const open = this.#ownEditor(opened);

        if (!open) {
            return false;
        }

        if (!contentlet) {
            this.#openLegacyCreate(contentTypeVariable, open.title ?? contentTypeVariable, {
                folderInode: this.currentFolder().folderInode,
                languageId: open.languageId ?? this.#createLanguageId() ?? 1
            });

            return true;
        }

        this.#panelRequest.set({
            editor: 'legacy',
            data: {
                mode: 'edit',
                inode: contentlet.inode,
                identifier: contentlet.identifier,
                languageId: contentlet.languageId,
                title: contentlet.title,
                portletId: CONTENT_DRIVE_PORTLET_ID
            }
        });

        // Inside the panel, related content opens in place, so the content switched can differ
        // from the one the URL names. The URL follows what the legacy panel reopens (FR-020).
        const location = this.#panelLocation();
        if (
            location?.kind === 'edit' &&
            (location.editContent !== contentlet.identifier ||
                location.editContentLang !== contentlet.languageId)
        ) {
            this.#panelLocation.set({
                kind: 'edit',
                editContent: contentlet.identifier,
                editContentLang: contentlet.languageId
            });
        }

        return true;
    }

    /**
     * The content of Content Drive's new-editor panel failed to load, after the standard error
     * was shown: close the panel, keeping the folder, filters and page (#37759, FR-029). Any other
     * editor is declined, as for {@link switchToLegacyEditor}.
     *
     * @param opened What the editor was opened with.
     * @returns Whether Content Drive closed its panel.
     */
    leaveOnLoadError(opened: EditContentIdentity): boolean {
        if (!this.#ownEditor(opened)) {
            return false;
        }

        this.closeEditPanel();

        return true;
    }

    /**
     * The new-editor panel Content Drive has open, when it is the editor that was opened with
     * `opened`: the same content for an edit, the same type for a create. What the editor was
     * opened with comes from the request Content Drive sent, and stays the same across the
     * editor's in-place reloads (a language switch).
     *
     * @param opened What the editor was opened with.
     * @returns The open request's data, or `null` for any other editor.
     */
    #ownEditor(opened: EditContentIdentity): EditContentDialogData | null {
        const request = this.#panelRequest();

        if (request?.editor !== 'new') {
            return null;
        }

        const { data } = request;
        const isOwn =
            data.mode === 'edit'
                ? !!opened.inode && opened.inode === data.contentletInode
                : !opened.inode && opened.contentTypeId === data.contentTypeId;

        return isOwn ? data : null;
    }

    /**
     * A panel saved. The first save of a create switches the URL to the saved content, so a refresh
     * reopens it instead of a second empty form (#37759, FR-025). A later save only follows a
     * language the saved version names, such as a new translation (FR-020). The request is left
     * alone, so the panel is not remounted.
     *
     * @param saved The saved content's identifier and language.
     */
    panelSaved({ identifier, languageId }: { identifier: string; languageId: number }): void {
        const location = this.#panelLocation();

        if (location?.kind === 'create') {
            this.#panelLocation.set({
                kind: 'edit',
                editContent: identifier,
                editContentLang: languageId
            });

            return;
        }

        // A new translation saved in the new editor: the saved version names the language. A save
        // of related content reached in place names another content, so it is left alone.
        if (
            location?.kind === 'edit' &&
            location.editContent === identifier &&
            location.editContentLang !== languageId
        ) {
            this.#panelLocation.set({ ...location, editContentLang: languageId });
        }
    }

    /**
     * The author switched language inside the legacy panel: the URL follows it, so a refresh
     * reopens that language (#37759, FR-020). A create names no content yet, so it is left alone.
     *
     * @param languageId The language the editor now shows.
     */
    panelLanguageChanged(languageId: number): void {
        const location = this.#panelLocation();

        if (location?.kind === 'edit') {
            this.#panelLocation.set({ ...location, editContentLang: languageId });
        }
    }

    /** Closes whichever side panel is open and clears what the URL says about it. */
    closeEditPanel(): void {
        this.#panelRequest.set(null);
        this.#panelLocation.set(null);
    }

    /**
     * Opens a side panel and names it in the URL: the identifier and language for an edit, the
     * content type variable for a create.
     *
     * @param request What to open, and in which editor.
     */
    #openPanel(request: DotContentDrivePanelRequest): void {
        this.#panelRequest.set(request);
        this.#panelLocation.set(this.#locationOf(request));
    }

    /**
     * The URL location a freshly opened panel starts from.
     *
     * @param request The request the panel was opened with.
     * @returns The location for that request.
     */
    #locationOf(request: DotContentDrivePanelRequest): DotContentDrivePanelLocation {
        if (request.editor === 'new') {
            const { data } = request;

            return data.mode === 'edit'
                ? {
                      kind: 'edit',
                      editContent: data.identifier ?? '',
                      editContentLang: data.languageId
                  }
                : { kind: 'create', createContent: data.contentTypeId ?? '' };
        }

        const { data } = request;

        return data.mode === 'edit'
            ? { kind: 'edit', editContent: data.identifier ?? '', editContentLang: data.languageId }
            : { kind: 'create', createContent: data.contentTypeVariable ?? '' };
    }

    /**
     * Opens a contentlet in the side panel of the editor its content type chose: the legacy panel
     * for a type that has not opted into the new editor, the new-editor panel otherwise. Either way
     * the author stays in Content Drive (#37759, FR-001).
     *
     * @param contentlet - The contentlet to edit
     */
    #editContentlet(contentlet: DotCMSContentlet) {
        this.#dotContentTypeService
            .getContentType(contentlet.contentType)
            .pipe(
                take(1),
                catchError((error: HttpErrorResponse) => {
                    this.#httpErrorManager.handle(error);

                    return EMPTY;
                })
            )
            .subscribe((contentType) =>
                this.#openEdit(contentlet, contentlet.identifier, contentType)
            );
    }

    /**
     * Opens a contentlet for edit in the panel of the editor its content type chose. The one rule
     * every edit path shares, so a link and a double-click never disagree on the editor (#37759,
     * FR-007, FR-021).
     *
     * @param contentlet The working version to edit.
     * @param identifier The identifier the URL names it by.
     * @param contentType The contentlet's content type.
     */
    #openEdit(
        contentlet: DotCMSContentlet,
        identifier: string,
        contentType: DotCMSContentType | undefined
    ): void {
        if (usesLegacyEditor(contentType)) {
            this.#openPanel({
                editor: 'legacy',
                data: {
                    mode: 'edit',
                    inode: contentlet.inode,
                    identifier,
                    languageId: contentlet.languageId,
                    title: contentlet.title,
                    portletId: CONTENT_DRIVE_PORTLET_ID
                }
            });

            return;
        }

        this.#openPanel({
            editor: 'new',
            data: {
                mode: 'edit',
                contentletInode: contentlet.inode,
                identifier,
                languageId: contentlet.languageId,
                title: contentlet.title
            }
        });
    }

    /**
     * Opens the editor for a content addressed by its stable `identifier` (e.g. from a shared
     * `?editContent=<identifier>` URL, or Part 1's redirect from `c/content/<inode>`). Resolves the
     * identifier to its current working inode (the editor loads by inode), looks up its content
     * type, then opens it in the panel of the editor that type chose. No-op when the content can't
     * be resolved (deleted, no permission, bad id).
     */
    openEditByIdentifier(identifier: string, languageId?: number): void {
        const anyVersion = `+identifier:${identifier} +working:true`;
        const preferred = this.#preferredLanguageId(languageId);

        this.#resolveWorkingVersion(
            preferred ? `${anyVersion} +languageId:${preferred}` : anyVersion
        )
            .pipe(
                // A link to content with no version in the preferred language must still open, so
                // "nothing" falls back to any version rather than being read as "do not open". Only
                // reached when that language is genuinely missing: the link carries the language that
                // was open, so the first lookup normally hits.
                switchMap((version) =>
                    version || !preferred ? of(version) : this.#resolveWorkingVersion(anyVersion)
                ),
                // The link names content, not an editor: its content type picks the editor, so a
                // legacy-editor type never opens in the new editor it opted out of (FR-021).
                switchMap((contentlet) =>
                    contentlet?.inode
                        ? this.#dotContentTypeService
                              .getContentType(contentlet.contentType)
                              .pipe(map((contentType) => ({ contentlet, contentType })))
                        : EMPTY
                ),
                take(1),
                catchError((error: HttpErrorResponse) => {
                    this.#httpErrorManager.handle(error);

                    return EMPTY;
                })
            )
            .subscribe(({ contentlet, contentType }) =>
                this.#openEdit(contentlet, identifier, contentType)
            );
    }

    /**
     * Resolves an identifier query to the single working contentlet it matches, or `undefined`.
     *
     * @param query The Lucene query to run.
     *
     * @return {*} {Observable<DotCMSContentlet | undefined>} The matched contentlet, if any.
     */
    #resolveWorkingVersion(query: string): Observable<DotCMSContentlet | undefined> {
        return this.#contentSearch
            .get<ContentSearchEntity>({ query, limit: 1 })
            .pipe(map((entity) => entity?.jsonObjectView?.contentlets?.[0]));
    }

    /**
     * Which language version the deep link should open, most authoritative first.
     *
     * One identifier has one inode PER LANGUAGE, so the identifier alone does not name a version.
     * `languageId` comes from the URL that opened the panel and names the exact one, so it wins. It is
     * only absent on a link written before the language was recorded; the rest is a best guess for that
     * case — the drive's active Locale filter (its first language, if several are selected), then the
     * environment default. Note both are usually still unresolved here: this runs from the shell's
     * constructor while the store's languages request is in flight, which is exactly why the URL
     * carrying the language matters.
     *
     * @param languageId The language the URL asked for, when it carried one.
     *
     * @return {*} {number | undefined} The language to look for, or `undefined` when none is known.
     */
    #preferredLanguageId(languageId?: number): number | undefined {
        return languageId || this.#createLanguageId();
    }

    /**
     * The language a create starts in: the list's language filter (its first language, if several
     * are selected), else Content Drive's default language (#37759, FR-003). Never the language of
     * the URL, which names an open edit, not the list.
     *
     * @returns The language id, or `undefined` while neither is known.
     */
    #createLanguageId(): number | undefined {
        const [selected] = (this.#store.getFilterValue('languageId') as string[]) ?? [];

        return Number(selected) || this.#store.defaultLanguageId() || undefined;
    }
}

/**
 * Whether a content type edits in the legacy editor: it has not opted into the new editor
 * (`CONTENT_EDITOR2_ENABLED` in its metadata). The type's own setting is the only thing that decides
 * it (#37759, FR-007).
 *
 * @param contentType The content type, or `undefined` when the lookup returned nothing.
 * @returns `true` for the legacy editor.
 */
function usesLegacyEditor(contentType: DotCMSContentType | undefined): boolean {
    return !contentType?.metadata?.[FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED];
}

/**
 * Hands the new editor's "switch to the old editor" and load error to Content Drive's navigation
 * service, for the panel Content Drive opened (#37759, FR-028, FR-029). The shell provides it next
 * to the service. Remove with the legacy editor.
 *
 * @returns The provider for `EDIT_CONTENT_NAVIGATION_OVERRIDE`.
 */
export function provideContentDriveNavigationOverride(): Provider {
    return {
        provide: EDIT_CONTENT_NAVIGATION_OVERRIDE,
        useExisting: DotContentDriveNavigationService
    };
}
