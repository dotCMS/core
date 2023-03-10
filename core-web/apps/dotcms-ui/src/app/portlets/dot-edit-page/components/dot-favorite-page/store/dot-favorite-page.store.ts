import { ComponentStore, tapResponse } from '@ngrx/component-store';
import { Observable, throwError, of, forkJoin } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { switchMap, take } from 'rxjs/operators';

import { DotHttpErrorManagerService } from '@dotcms/app/api/services/dot-http-error-manager/dot-http-error-manager.service';
import { DotTempFileUploadService } from '@dotcms/app/api/services/dot-temp-file-upload/dot-temp-file-upload.service';
import {
    DotCurrentUserService,
    DotPageRenderService,
    DotRolesService,
    DotMessageService,
    DotWorkflowActionsFireService,
    DotContentletService
} from '@dotcms/data-access';
import {
    DotRole,
    DotCMSContentlet,
    DotCMSTempFile,
    DotCurrentUser,
    DotPageRenderParameters,
    DotContentletPermissions
} from '@dotcms/dotcms-models';

import { DotFavoritePageFormData } from '../dot-favorite-page.component';

export const enum DotFavoritePageActionState {
    SAVED = 'SAVED',
    DELETED = 'DELETED'
}

export interface DotFavoritePageState {
    roleOptions: DotRole[];
    pageRenderedHtml?: string;
    formState: DotFavoritePageFormData;
    isAdmin: boolean;
    imgWidth: number;
    imgHeight: number;
    renderThumbnail: boolean;
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

@Injectable()
export class DotFavoritePageStore extends ComponentStore<DotFavoritePageState> {
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
                    tapResponse(
                        () => {
                            this.patchState({
                                closeDialog: true,
                                loading: false,
                                actionState: DotFavoritePageActionState.SAVED
                            });
                        },
                        (error: HttpErrorResponse) => {
                            this.dotHttpErrorManagerService.handle(error);

                            this.patchState({
                                loading: false
                            });

                            return of(null);
                        }
                    )
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
            tapResponse(
                () => {
                    this.patchState({
                        closeDialog: true,
                        loading: false,
                        actionState: DotFavoritePageActionState.DELETED
                    });
                },
                (error: HttpErrorResponse) => {
                    this.dotHttpErrorManagerService.handle(error);

                    this.patchState({
                        loading: false
                    });

                    return of(null);
                }
            )
        );
    });

    private createAndPublishFavoritePage = (formData: DotFavoritePageFormData) => {
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
    };

    private publishContentletAndWaitForIndex = (formData: DotFavoritePageFormData) => {
        const individualPermissions = {
            READ: []
        };
        individualPermissions.READ = formData.permissions || [];
        individualPermissions.READ.push(formData.currentUserRoleId, CMS_OWNER_ROLE_ID);

        return this.dotWorkflowActionsFireService.publishContentletAndWaitForIndex<DotCMSContentlet>(
            'dotFavoritePage',
            {
                screenshot: formData.thumbnail,
                title: formData.title,
                url: formData.url,
                order: formData.order,
                inode: formData.inode || null
            },
            individualPermissions
        );
    };

    private getContentletPermissionsObservable = (identifier: string) => {
        return identifier
            ? this.dotContentletService.getContentletPermissions(identifier)
            : of({ READ: [] });
    };

    constructor(
        private dotCurrentUser: DotCurrentUserService,
        private dotPageRenderService: DotPageRenderService,
        private dotContentletService: DotContentletService,
        private dotRolesService: DotRolesService,
        private dotMessageService: DotMessageService,
        private dotHttpErrorManagerService: DotHttpErrorManagerService,
        private dotTempFileUploadService: DotTempFileUploadService,
        private dotWorkflowActionsFireService: DotWorkflowActionsFireService
    ) {
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
            roleOptions: [],
            formState: null,
            isAdmin: null,
            imgWidth: null,
            imgHeight: null,
            renderThumbnail: null,
            loading: true,
            closeDialog: false,
            pageRenderedHtml: '',
            actionState: null
        });

        const formInitialState: DotFavoritePageFormData = {
            currentUserRoleId: '',
            inode: favoritePage?.inode || '',
            order: favoritePage ? favoritePage['order'] : 1,
            permissions: [],
            thumbnail: favoritePage ? favoritePage['screenshot'] : '',
            title: favoritePage?.title || '',
            url: favoritePageUrl
        };
        forkJoin([
            this.dotRolesService.search(),
            this.dotCurrentUser.getCurrentUser(),
            this.dotPageRenderService.get({ url: favoritePageUrl }),
            this.getContentletPermissionsObservable(favoritePage?.identifier) // TODO: replace with new Permissions endpoint
        ])
            .pipe(take(1))
            .subscribe(
                ([roles, currentUser, pageRender, permissionsStored]: [
                    DotRole[],
                    DotCurrentUser,
                    DotPageRenderParameters,
                    DotContentletPermissions
                ]): void => {
                    this.patchState({
                        loading: false,
                        isAdmin: currentUser.admin,
                        formState: {
                            ...formInitialState,
                            currentUserRoleId: currentUser.roleId,
                            permissions: permissionsStored.READ,
                            title: favoritePage?.title || pageRender.page.title
                        },
                        imgHeight:
                            parseInt(pageRender.viewAs.device?.cssHeight, 10) ||
                            (parseInt(pageRender.viewAs.device?.cssWidth, 10) || 1024) /
                                IMG_RATIO_43,
                        imgWidth: parseInt(pageRender.viewAs.device?.cssWidth, 10) || 1024,
                        pageRenderedHtml: pageRender.page.rendered,
                        renderThumbnail: !(favoritePage && !!favoritePage['screenshot']),
                        roleOptions: roles
                    });
                }
            );
    }
}
