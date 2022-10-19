import { Injectable } from '@angular/core';
import { DotRolesService } from '@dotcms/app/api/services/dot-roles/dot-roles.service';
import { DotRole } from '@dotcms/app/shared/models/dot-role/dot-role.model';
import { ComponentStore, tapResponse } from '@ngrx/component-store';
import { forkJoin, Observable, of, throwError } from 'rxjs';
import { switchMap, take } from 'rxjs/operators';
import { DotFavoritePageFormData } from '../dot-favorite-page.component';
import { DotMessageService } from '@dotcms/app/api/services/dot-message/dot-messages.service';
import { HttpErrorResponse } from '@angular/common/http';
import { DotHttpErrorManagerService } from '@dotcms/app/api/services/dot-http-error-manager/dot-http-error-manager.service';
import { DotTempFileUploadService } from '@dotcms/app/api/services/dot-temp-file-upload/dot-temp-file-upload.service';
import { DotWorkflowActionsFireService } from '@dotcms/app/api/services/dot-workflow-actions-fire/dot-workflow-actions-fire.service';
import { DotCMSContentlet, DotCMSTempFile } from '@dotcms/dotcms-models';
import { DotCurrentUserService } from '@dotcms/app/api/services/dot-current-user/dot-current-user.service';
import { DotCurrentUser } from '@dotcms/app/shared/models/dot-current-user/dot-current-user';

export const enum DotFavoritePageActionState {
    SAVED = 'SAVED',
    DELETED = 'DELETED'
}

export interface DotFavoritePageState {
    roleOptions: DotRole[];
    currentUserRoleId: string;
    pageRenderedHtml?: string;
    isAdmin: boolean;
    imgWidth: number;
    imgHeight: number;
    loading: boolean;
    closeDialog: boolean;
    actionState: DotFavoritePageActionState;
}

interface DotFavoritePageInitialProps {
    isAdmin: boolean;
    imgWidth: string;
    imgHeight: string;
    pageRenderedHtml?: string;
}

export const CMS_OWNER_ROLE_ID = '6b1fa42f-8729-4625-80d1-17e4ef691ce7';

const IMG_RATIO_43 = 1.333;

@Injectable()
export class DotFavoritePageStore extends ComponentStore<DotFavoritePageState> {
    readonly vm$ = this.state$;

    // SELECTORS
    public readonly actionState$ = this.select(({ actionState }) => actionState);
    public readonly closeDialog$ = this.select(({ closeDialog }) => closeDialog);
    public readonly currentUserRoleId$ = this.select(({ currentUserRoleId }) => currentUserRoleId);

    // EFFECTS
    readonly saveFavoritePage = this.effect((data$: Observable<DotFavoritePageFormData>) => {
        return data$.pipe(
            switchMap((formData: DotFavoritePageFormData) => {
                this.patchState({ loading: true });

                const file = new File([formData.thumbnail], 'image.png');
                const individualPermissions = {
                    READ: []
                };
                individualPermissions.READ = formData.permissions
                    ? formData.permissions.map((role: DotRole) => role.id)
                    : [];
                individualPermissions.READ.push(formData.currentUserRoleId, CMS_OWNER_ROLE_ID);

                return this.dotTempFileUploadService.upload(file).pipe(
                    switchMap(([{ id, image }]: DotCMSTempFile[]) => {
                        if (!image) {
                            return throwError(
                                this.dotMessageService.get(
                                    'favoritePage.dialog.error.tmpFile.upload'
                                )
                            );
                        }

                        return this.dotWorkflowActionsFireService.publishContentletAndWaitForIndex<DotCMSContentlet>(
                            'dotFavoritePage',
                            {
                                screenshot: id,
                                title: formData.title,
                                url: formData.url,
                                order: formData.order
                            },
                            individualPermissions
                        );
                    }),
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

                            return of(null);
                        }
                    )
                );
            })
        );
    });

    constructor(
        private dotCurrentUser: DotCurrentUserService,
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
    setInitialStateData(props: DotFavoritePageInitialProps): void {
        const propsData = {
            isAdmin: props.isAdmin,
            imgWidth: parseInt(props.imgWidth, 10) || 1024,
            imgHeight:
                parseInt(props.imgHeight, 10) ||
                (parseInt(props.imgWidth, 10) || 1024) / IMG_RATIO_43
        };

        this.setState({
            roleOptions: [],
            currentUserRoleId: '',
            isAdmin: propsData.isAdmin,
            imgWidth: propsData.imgWidth,
            imgHeight: propsData.imgHeight,
            loading: true,
            closeDialog: false,
            pageRenderedHtml: props.pageRenderedHtml,
            actionState: null
        });

        forkJoin([this.dotRolesService.search(), this.dotCurrentUser.getCurrentUser()])
            .pipe(take(1))
            .subscribe(([roles, currentUser]: [DotRole[], DotCurrentUser]): void => {
                this.patchState({
                    loading: false,
                    currentUserRoleId: currentUser.roleId,
                    roleOptions: roles
                });
            });
    }
}
