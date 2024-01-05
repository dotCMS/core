import { ComponentStore, tapResponse } from '@ngrx/component-store';
import { EMPTY, Observable, forkJoin } from 'rxjs';

import { Injectable } from '@angular/core';

import { catchError, map, shareReplay, switchMap, take, tap } from 'rxjs/operators';

import { DotLicenseService } from '@dotcms/data-access';
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
import { ActionPayload, SavePagePayload } from '../../shared/models';
import { insertContentletInContainer } from '../../utils';

type DialogType = 'content' | 'form' | 'widget' | 'shell' | null;

export interface EditEmaState {
    editor: DotPageApiResponse;
    url: string;
    dialogIframeURL: string;
    dialogHeader: string;
    dialogIframeLoading: boolean;
    isEnterpriseLicense: boolean;
    dialogType: DialogType;
}

function getFormId(dotPageApiService) {
    return (source: Observable<unknown>) =>
        source.pipe(
            switchMap(({ payload, formId, whenSaved }) => {
                return dotPageApiService
                    .getFormIndetifier(payload.container.identifier, formId)
                    .pipe(
                        map((newFormId: string) => {
                            return {
                                payload: {
                                    ...payload,
                                    newContentletId: newFormId
                                },
                                whenSaved
                            };
                        })
                    );
            })
        );
}

@Injectable()
export class EditEmaStore extends ComponentStore<EditEmaState> {
    constructor(
        private dotPageApiService: DotPageApiService,
        private dotActionUrl: DotActionUrlService,
        private dotLicenseService: DotLicenseService
    ) {
        super();
    }

    readonly editorState$ = this.select((state) => {
        const pageURL = this.createPageURL({
            url: state.url,
            language_id: state.editor.viewAs.language.id.toString(),
            'com.dotmarketing.persona.id':
                state.editor.viewAs.persona?.identifier ?? DEFAULT_PERSONA.identifier
        });

        return {
            apiURL: `${window.location.origin}/api/v1/page/json/${pageURL}`,
            iframeURL: `${HOST}/${pageURL}` + `&t=${Date.now()}`, // The iframe will only reload if the queryParams changes, so we add a timestamp to force a reload when no queryParams change
            editor: {
                ...state.editor,
                viewAs: {
                    ...state.editor.viewAs,
                    persona: state.editor.viewAs.persona ?? DEFAULT_PERSONA
                }
            },
            isEnterpriseLicense: state.isEnterpriseLicense
        };
    });

    readonly dialogState$ = this.select((state) => ({
        iframeURL: state.dialogIframeURL,
        header: state.dialogHeader,
        iframeLoading: state.dialogIframeLoading,
        type: state.dialogType
    }));

    readonly layoutProperties$ = this.select((state) => ({
        layout: state.editor.layout,
        themeId: state.editor.template.theme,
        pageId: state.editor.page.identifier,
        containersMap: this.mapContainers(state.editor.containers)
    }));

    readonly shellProperties$ = this.select((state) => ({
        page: state.editor.page,
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
            switchMap((params) =>
                forkJoin({
                    pageData: this.dotPageApiService.get(params),
                    licenseData: this.dotLicenseService.isEnterprise().pipe(take(1), shareReplay())
                }).pipe(
                    tap({
                        next: ({ pageData, licenseData }) => {
                            this.setState({
                                editor: pageData,
                                url: params.url,
                                dialogIframeURL: '',
                                dialogHeader: '',
                                dialogIframeLoading: false,
                                isEnterpriseLicense: licenseData,
                                dialogType: null
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

    readonly saveFormToPage = this.effect(
        (
            payload$: Observable<{
                payload: ActionPayload;
                formId: string;
                whenSaved?: () => void;
            }>
        ) => {
            return payload$.pipe(
                getFormId(this.dotPageApiService),
                switchMap(({ whenSaved, payload }) => {
                    const pageContainers = insertContentletInContainer(payload);

                    return this.dotPageApiService
                        .save({
                            pageContainers,
                            pageId: payload.pageId
                        })
                        .pipe(
                            tapResponse(
                                () => {
                                    whenSaved?.();
                                },
                                (e) => {
                                    console.error(e);
                                    whenSaved?.();
                                }
                            )
                        );
                })
            );
        }
    );

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
                                this.setDialogForCreateContent({
                                    url,
                                    name
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

    readonly setDialogForCreateContent = this.updater(
        (state, { url, name }: { url: string; name: string }) => {
            return {
                ...state,
                dialogIframeURL: url,
                dialogHeader: `Create ${name}`,
                dialogIframeLoading: true,
                dialogType: 'content'
            };
        }
    );

    readonly setDialogIframeLoading = this.updater((state, editIframeLoading: boolean) => ({
        ...state,
        dialogIframeLoading: editIframeLoading
    }));

    // This method resets the properties that are being used in for the dialog
    readonly resetDialog = this.updater((state) => {
        return {
            ...state,
            dialogIframeURL: '',
            dialogHeader: '',
            dialogIframeLoading: false,
            dialogType: null
        };
    });

    // This method is called when the user clicks on the edit button
    readonly initActionEdit = this.updater(
        (state, payload: { inode: string; title: string; type: DialogType }) => {
            return {
                ...state,
                dialogHeader: payload.title,
                dialogIframeLoading: true,
                dialogIframeURL: this.createEditContentletUrl(payload.inode),
                dialogType: payload.type
            };
        }
    );

    // This method is called when the user clicks on the [+ add] button
    readonly initActionAdd = this.updater(
        (
            state,
            payload: {
                containerId: string;
                acceptTypes: string;
                language_id: string;
            }
        ) => {
            return {
                ...state,
                dialogHeader: 'Search Content', // Does this need translation?
                dialogIframeLoading: true,
                dialogIframeURL: this.createAddContentletUrl(payload),
                dialogType: 'content'
            };
        }
    );

    readonly initActionAddForm = this.updater((state) => {
        return {
            ...state,
            dialogHeader: 'Search Forms', // Does this need translation?
            dialogIframeLoading: true,
            dialogIframeURL: null,
            dialogType: 'form'
        };
    });

    // This method is called when the user clicks in the + button in the jsp dialog
    readonly initActionCreate = this.updater(
        (state, payload: { contentType: string; url: string }) => {
            return {
                ...state,
                dialogHeader: payload.contentType,
                dialogIframeLoading: true,
                dialogIframeURL: payload.url,
                dialogType: 'content'
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

    private createPageURL(params: DotPageApiParams): string {
        return `${params.url}?language_id=${params.language_id}&com.dotmarketing.persona.id=${params['com.dotmarketing.persona.id']}`;
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
