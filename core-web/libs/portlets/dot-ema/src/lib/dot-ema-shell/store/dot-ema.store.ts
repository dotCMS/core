import { ComponentStore, tapResponse } from '@ngrx/component-store';
import { EMPTY, Observable } from 'rxjs';

import { Injectable } from '@angular/core';

import { catchError, switchMap, tap } from 'rxjs/operators';

import { DotContainerMap, DotLayout, DotPageContainerStructure } from '@dotcms/dotcms-models';

import { DotActionUrlService } from '../../services/dot-action-url/dot-action-url.service';
import {
    DotPageApiService,
    DotPageApiParams,
    DotPageApiResponse
} from '../../services/dot-page-api.service';
import {
    DEFAULT_PERSONA,
    HOST,
    EDIT_CONTENTLET_URL,
    ADD_CONTENTLET_URL
} from '../../shared/consts';
import { SavePagePayload } from '../../shared/models';

export interface EditEmaState {
    editor: DotPageApiResponse;
    url: string;
    dialogIframeURL: string;
    dialogVisible: boolean;
    dialogHeader: string;
    dialogIframeLoading: boolean;
}

@Injectable()
export class EditEmaStore extends ComponentStore<EditEmaState> {
    constructor(
        private dotPageApiService: DotPageApiService,
        private dotActionUrl: DotActionUrlService
    ) {
        super();
    }

    readonly editorState$ = this.select((state) => {
        const pageURL = this.createPageURL({
            url: state.url,
            language_id: state.editor.viewAs.language.id.toString(),
            persona_id: state.editor.viewAs.persona?.identifier ?? DEFAULT_PERSONA.identifier
        });

        return {
            apiURL: `${window.location.origin}/api/v1/page/json/${pageURL}`,
            iframeURL: `${HOST}/${pageURL}`,
            editor: {
                ...state.editor,
                viewAs: {
                    ...state.editor.viewAs,
                    persona: state.editor.viewAs.persona ?? DEFAULT_PERSONA
                }
            }
        };
    });

    readonly dialogState$ = this.select((state) => ({
        dialogIframeURL: state.dialogIframeURL,
        dialogVisible: state.dialogVisible,
        dialogHeader: state.dialogHeader,
        dialogIframeLoading: state.dialogIframeLoading
    }));

    readonly layoutProperties$ = this.select((state) => ({
        layout: state.editor.layout,
        themeId: state.editor.template.theme,
        pageId: state.editor.page.identifier,
        containersMap: this.mapContainers(state.editor.containers)
    }));

    readonly shellProperties$ = this.select((state) => ({
        pageId: state.editor.page.identifier,
        siteId: state.editor.site.identifier,
        languageId: state.editor.viewAs.language.id,
        currentUrl: '/' + state.url,
        host: HOST
    }));

    /**
     * Load the page editor
     *
     * @memberof EditEmaStore
     */
    readonly load = this.effect((params$: Observable<DotPageApiParams>) => {
        return params$.pipe(
            switchMap(({ language_id, url, persona_id }) =>
                this.dotPageApiService.get({ language_id, url, persona_id }).pipe(
                    tap({
                        next: (editor) => {
                            this.setState({
                                editor,
                                url,
                                dialogIframeURL: '',
                                dialogVisible: false,
                                dialogHeader: '',
                                dialogIframeLoading: false
                            });
                        },
                        error: (e) => {
                            // eslint-disable-next-line no-console
                            console.log(e);
                        }
                    }),
                    catchError(() => EMPTY)
                )
            )
        );
    });

    /**
     * Save the page
     *
     * @memberof EditEmaStore
     */
    readonly savePage = this.effect((payload$: Observable<SavePagePayload>) => {
        return payload$.pipe(
            switchMap((payload) => {
                return this.dotPageApiService.save(payload).pipe(
                    tapResponse(
                        () => {
                            payload.whenSaved?.();
                        },
                        (e) => {
                            console.error(e);
                            payload.whenSaved?.();
                        }
                    )
                );
            })
        );
    });

    /**
     * Create a contentlet from the palette
     *
     * @memberof EditEmaStore
     */
    readonly createContentFromPalette = this.effect(
        (contentTypeVariable$: Observable<{ variable: string; name: string }>) => {
            return contentTypeVariable$.pipe(
                switchMap(({ name, variable }) => {
                    return this.dotActionUrl.getCreateContentletUrl(variable).pipe(
                        tapResponse(
                            (url) => {
                                this.setDialog({
                                    url,
                                    title: `Create ${name}`
                                });
                            },
                            (e) => {
                                console.error(e);
                            }
                        )
                    );
                })
            );
        }
    );

    readonly setDialog = this.updater((state, { url, title }: { url: string; title: string }) => {
        return {
            ...state,
            dialogIframeURL: url,
            dialogVisible: true,
            dialogHeader: title,
            dialogIframeLoading: true
        };
    });

    readonly setDialogIframeLoading = this.updater((state, editIframeLoading: boolean) => ({
        ...state,
        dialogIframeLoading: editIframeLoading
    }));

    // This method resets the properties that are being used in for the dialog
    readonly resetDialog = this.updater((state) => {
        return {
            ...state,
            dialogIframeURL: '',
            dialogVisible: false,
            dialogHeader: '',
            dialogIframeLoading: false
        };
    });

    // This method is called when the user clicks on the edit button
    readonly initActionEdit = this.updater((state, payload: { inode: string; title: string }) => {
        return {
            ...state,
            dialogVisible: true,
            dialogHeader: payload.title,
            dialogIframeLoading: true,
            dialogIframeURL: this.createEditContentletUrl(payload.inode)
        };
    });

    // This method is called when the user clicks on the [+ add] button
    readonly initActionAdd = this.updater(
        (state, payload: { containerId: string; acceptTypes: string; language_id: string }) => {
            return {
                ...state,
                dialogVisible: true,
                dialogHeader: 'Search Content', // Does this need translation?
                dialogIframeLoading: true,
                dialogIframeURL: this.createAddContentletUrl(payload)
            };
        }
    );

    // This method is called when the user clicks in the + button in the jsp dialog
    readonly initActionCreate = this.updater(
        (state, payload: { contentType: string; url: string }) => {
            return {
                ...state,
                dialogVisible: true,
                dialogHeader: payload.contentType,
                dialogIframeLoading: true,
                dialogIframeURL: payload.url
            };
        }
    );

    /**
     * Update the page layout
     *
     * @memberof EditEmaStore
     */
    readonly updatePageLayout = this.updater((state, layout: DotLayout) => ({
        ...state,
        editor: {
            ...state.editor,
            layout
        }
    }));

    /**
     * Create the url to edit a contentlet
     *
     * @private
     * @param {string} inode
     * @return {*}
     * @memberof DotEmaComponent
     */
    private createEditContentletUrl(inode: string): string {
        return `${EDIT_CONTENTLET_URL}${inode}`;
    }

    /**
     * Create the url to add a contentlet
     *
     * @private
     * @param {{containerID: string, acceptTypes: string}} {containerID, acceptTypes}
     * @return {*}  {string}
     * @memberof EditEmaStore
     */
    private createAddContentletUrl({
        containerId,
        acceptTypes,
        language_id
    }: {
        containerId: string;
        acceptTypes: string;
        language_id: string;
    }): string {
        return ADD_CONTENTLET_URL.replace('*CONTAINER_ID*', containerId)
            .replace('*BASE_TYPES*', acceptTypes)
            .replace('*LANGUAGE_ID*', language_id);
    }

    private createPageURL({ url, language_id, persona_id }: DotPageApiParams): string {
        return `${url}?language_id=${language_id}&com.dotmarketing.persona.id=${persona_id}`;
    }

    /**
     * Map the containers to a DotContainerMap
     *
     * @private
     * @param {DotPageContainerStructure} containers
     * @return {*}  {DotContainerMap}
     * @memberof EditEmaStore
     */
    private mapContainers(containers: DotPageContainerStructure): DotContainerMap {
        return Object.keys(containers).reduce((acc, id) => {
            acc[id] = containers[id].container;

            return acc;
        }, {});
    }
}
