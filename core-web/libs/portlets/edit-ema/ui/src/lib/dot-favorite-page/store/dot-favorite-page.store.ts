import { ComponentStore } from '@ngrx/component-store';
import { tapResponse } from '@ngrx/operators';
import { Observable, throwError, of } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { mergeMap, switchMap, take } from 'rxjs/operators';

import {
    DotPageRenderService,
    DotMessageService,
    DotWorkflowActionsFireService,
    DotHttpErrorManagerService,
    DotTempFileUploadService
} from '@dotcms/data-access';
import { DotCMSContentlet, DotCMSTempFile, DotPageRenderParameters } from '@dotcms/dotcms-models';

import { DotFavoritePageFormData } from '../dot-favorite-page.component';

export const enum DotFavoritePageActionState {
    SAVED = 'SAVED',
    DELETED = 'DELETED'
}

export interface DotFavoritePageState {
    pageRenderedHtml?: string;
    formState: DotFavoritePageFormData;
    imgWidth: number;
    imgHeight: number;
    renderThumbnail: boolean;
    showFavoriteEmptySkeleton: boolean;
    loading: boolean;
    closeDialog: boolean;
    actionState: DotFavoritePageActionState;
}

interface DotFavoritePageInitialProps {
    favoritePageUrl: string;
    favoritePage?: DotCMSContentlet;
}

export const CMS_OWNER_ROLE_ID = '6b1fa42f-8729-4625-80d1-17e4ef691ce7';

const IMG_RATIO_43 = 1.333;

export const CMS_OWNER_ROLE_LIST = ['CMS OWNER'];

@Injectable()
export class DotFavoritePageStore extends ComponentStore<DotFavoritePageState> {
    private dotPageRenderService = inject(DotPageRenderService);
    private dotMessageService = inject(DotMessageService);
    private dotHttpErrorManagerService = inject(DotHttpErrorManagerService);
    private dotTempFileUploadService = inject(DotTempFileUploadService);
    private dotWorkflowActionsFireService = inject(DotWorkflowActionsFireService);

    readonly vm$ = this.state$;

    // SELECTORS
    public readonly actionState$ = this.select(({ actionState }) => actionState);
    public readonly closeDialog$ = this.select(({ closeDialog }) => closeDialog);
    public readonly renderThumbnail$ = this.select(({ renderThumbnail }) => renderThumbnail);
    public readonly formState$ = this.select(({ formState }) => formState);

    // UPDATERS
    readonly setRenderThumbnail = this.updater((state: DotFavoritePageState, data: boolean) => {
        return { ...state, renderThumbnail: data };
    });

    readonly setShowFavoriteEmptySkeleton = this.updater(
        (state: DotFavoritePageState, data: boolean) => {
            return { ...state, showFavoriteEmptySkeleton: data };
        }
    );

    // EFFECTS
    readonly saveFavoritePage = this.effect((data$: Observable<DotFavoritePageFormData>) => {
        return data$.pipe(
            switchMap((formData: DotFavoritePageFormData) => {
                this.patchState({ loading: true });

                let observableResponse: Observable<DotCMSContentlet>;
                if (formData.inode && !((formData.thumbnail as unknown) instanceof File)) {
                    observableResponse = this.publishContentletAndWaitForIndex(formData);
                } else {
                    observableResponse = this.createAndPublishFavoritePage(formData);
                }

                return observableResponse.pipe(
                    take(1),
                    tapResponse({
                        next: () => {
                            this.patchState({
                                closeDialog: true,
                                loading: false,
                                actionState: DotFavoritePageActionState.SAVED
                            });
                        },
                        error: (error: HttpErrorResponse) => {
                            this.dotHttpErrorManagerService.handle(error);
                            this.patchState({
                                loading: false
                            });
                            return of(null);
                        }
                    })
                );
            })
        );
    });

    readonly deleteFavoritePage = this.effect((data$: Observable<string>) => {
        return data$.pipe(
            switchMap((inode: string) => {
                this.patchState({ loading: true });

                return this.dotWorkflowActionsFireService.deleteContentlet<DotCMSContentlet>({
                    inode: inode
                });
            }),
            take(1),
            tapResponse({
                next: () => {
                    this.patchState({
                        closeDialog: true,
                        loading: false,
                        actionState: DotFavoritePageActionState.DELETED
                    });
                },
                error: (error: HttpErrorResponse) => {
                    this.dotHttpErrorManagerService.handle(error);
                    this.patchState({
                        loading: false
                    });
                    return of(null);
                }
            })
        );
    });

    private createAndPublishFavoritePage = (formData: DotFavoritePageFormData) => {
        if (formData.thumbnail) {
            const file = new File([formData.thumbnail], 'image.png');

            return this.dotTempFileUploadService.upload(file).pipe(
                switchMap(([{ id, image }]: DotCMSTempFile[]) => {
                    if (!image) {
                        return throwError(
                            this.dotMessageService.get('favoritePage.dialog.error.tmpFile.upload')
                        );
                    }

                    formData.thumbnail = id;

                    return this.publishContentletAndWaitForIndex(formData);
                })
            );
        } else {
            return this.publishContentletAndWaitForIndex(formData);
        }
    };

    private publishContentletAndWaitForIndex = (formData: DotFavoritePageFormData) => {
        return this.dotWorkflowActionsFireService.publishContentletAndWaitForIndex<DotCMSContentlet>(
            'dotFavoritePage',
            {
                screenshot: formData.thumbnail,
                title: formData.title,
                url: formData.url,
                order: formData.order,
                inode: formData.inode || null
            },
            {
                READ: CMS_OWNER_ROLE_LIST,
                WRITE: CMS_OWNER_ROLE_LIST,
                PUBLISH: CMS_OWNER_ROLE_LIST
            }
        );
    };

    private setUnknownPageInitialStateData(
        formInitialState: DotFavoritePageFormData,
        favoritePage: DotCMSContentlet
    ) {
        this.patchState({
            loading: false,
            formState: {
                ...formInitialState,
                title: favoritePage?.title
            },
            imgHeight: IMG_RATIO_43,
            imgWidth: 1024,
            pageRenderedHtml: '',
            renderThumbnail: !(favoritePage && !!favoritePage['screenshot']),
            showFavoriteEmptySkeleton: favoritePage && !favoritePage['screenshot']
        });
    }

    constructor() {
        super(null);
    }

    /**
     * Sets initial state data from props, roles and current logged user data
     *
     * @param {DotFavoritePageInitialProps} props
     * @memberof DotFavoritePageStore
     */
    setInitialStateData({ favoritePageUrl, favoritePage }: DotFavoritePageInitialProps): void {
        this.setState({
            formState: null,
            imgWidth: null,
            imgHeight: null,
            renderThumbnail: null,
            showFavoriteEmptySkeleton: null,
            loading: true,
            closeDialog: false,
            pageRenderedHtml: '',
            actionState: null
        });

        const formInitialState: DotFavoritePageFormData = {
            inode: favoritePage?.inode || '',
            order: favoritePage ? favoritePage['order'] : 1,
            thumbnail: favoritePage ? favoritePage['screenshot'] : '',
            title: favoritePage?.title || '',
            url: favoritePageUrl
        };

        const urlParams = { url: favoritePageUrl.split('?')[0] };
        const searchParams = new URLSearchParams(favoritePageUrl.split('?')[1]);

        for (const entry of searchParams) {
            urlParams[entry[0]] = entry[1];
        }

        this.dotPageRenderService
            .checkPermission(urlParams)
            .pipe(
                take(1),
                mergeMap(() => this.dotPageRenderService.get({ url: favoritePageUrl }))
            )
            .subscribe(
                (pageRender: DotPageRenderParameters): void => {
                    this.patchState({
                        loading: false,
                        formState: {
                            ...formInitialState,
                            title:
                                pageRender.urlContentMap?.title ||
                                favoritePage?.title ||
                                pageRender.page.title
                        },
                        imgHeight:
                            parseInt(pageRender.viewAs.device?.cssHeight, 10) ||
                            (parseInt(pageRender.viewAs.device?.cssWidth, 10) || 1024) /
                                IMG_RATIO_43,
                        imgWidth: parseInt(pageRender.viewAs.device?.cssWidth, 10) || 1024,
                        pageRenderedHtml: pageRender.page.rendered,
                        renderThumbnail: !(favoritePage && !!favoritePage['screenshot']),
                        showFavoriteEmptySkeleton: favoritePage && !favoritePage['screenshot']
                    });
                },
                (error: HttpErrorResponse) => {
                    if (error.status === 404) {
                        this.setUnknownPageInitialStateData(formInitialState, favoritePage);
                    } else {
                        this.dotHttpErrorManagerService.handle(error);
                    }
                }
            );
    }
}
