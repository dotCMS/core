import { ComponentStore } from '@ngrx/component-store';
import { Observable, of, pipe } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { catchError, filter, pluck, switchMap, take, tap } from 'rxjs/operators';

import {
    DotContentTypeService,
    DotHttpErrorManagerService,
    DotMessageService,
    DotRouterService,
    DotGlobalMessageService
} from '@dotcms/data-access';
import {
    DotCMSContentType,
    DotContainer,
    DotContainerEntity,
    DotContainerPayload,
    DotContainerStructure
} from '@dotcms/dotcms-models';
import { isEqual } from '@dotcms/utils';

import { DotContainersService } from '../../../../../api/services/dot-containers/dot-containers.service';

export interface DotContainerPropertiesState {
    showPrePostLoopInput: boolean;
    isContentTypeVisible: boolean;
    isContentTypeButtonEnabled: boolean;
    container: DotContainer;
    containerStructures: DotContainerStructure[];
    contentTypes: DotCMSContentType[];
    originalForm: DotContainerPayload;
    apiLink: string;
    invalidForm: boolean;
}

@Injectable()
export class DotContainerPropertiesStore extends ComponentStore<DotContainerPropertiesState> {
    private dotMessageService = inject(DotMessageService);
    private dotGlobalMessageService = inject(DotGlobalMessageService);
    private dotContainersService = inject(DotContainersService);
    private dotHttpErrorManagerService = inject(DotHttpErrorManagerService);
    private activatedRoute = inject(ActivatedRoute);
    private dotRouterService = inject(DotRouterService);
    private dotContentTypeService = inject(DotContentTypeService);

    constructor() {
        super({
            showPrePostLoopInput: false,
            isContentTypeVisible: false,
            isContentTypeButtonEnabled: false,
            containerStructures: [],
            contentTypes: [],
            container: null,
            originalForm: null,
            apiLink: '',
            invalidForm: true
        });
        this.activatedRoute.data
            .pipe(
                pluck('container'),
                take(1),
                filter((containerEntity) => !!containerEntity)
            )
            .subscribe((containerEntity: DotContainerEntity) => {
                const { container, contentTypes } = containerEntity;
                if (container && contentTypes?.length > 0) {
                    this.updatePrePostLoopAndContentTypeVisibility({
                        showPrePostLoopInput: !!container.preLoop || !!container.postLoop,
                        isContentTypeVisible: true,
                        container: container,
                        containerStructures: contentTypes ?? []
                    });
                } else {
                    this.updateContainerState(containerEntity);
                }

                this.updateApiLink(this.getApiLink(container.identifier));
            });
    }

    readonly vm$ = this.select((state: DotContainerPropertiesState) => {
        return state;
    });

    readonly containerAndStructure$ = this.select(
        ({ container, containerStructures }: DotContainerPropertiesState) => {
            return {
                container,
                containerStructures
            };
        }
    );

    readonly updatePrePostLoopAndContentTypeVisibility = this.updater<{
        showPrePostLoopInput: boolean;
        isContentTypeVisible: boolean;
        container: DotContainer;
        containerStructures: DotContainerStructure[];
    }>(
        (
            state: DotContainerPropertiesState,
            {
                showPrePostLoopInput,
                isContentTypeVisible,
                container,
                containerStructures
            }: DotContainerPropertiesState
        ) => {
            return {
                ...state,
                showPrePostLoopInput,
                isContentTypeVisible,
                container,
                containerStructures
            };
        }
    );

    readonly updatePrePostLoopInputVisibility = this.updater<boolean>(
        (state: DotContainerPropertiesState, showPrePostLoopInput: boolean) => {
            return {
                ...state,
                showPrePostLoopInput
            };
        }
    );

    readonly updateContentTypeVisibility = this.updater<boolean>(
        (state: DotContainerPropertiesState, isContentTypeVisible: boolean) => {
            return {
                ...state,
                isContentTypeVisible
            };
        }
    );

    /**
     * Update form status
     * @memberof DotContainerPropertiesStore
     */
    readonly updateFormStatus = this.updater<{
        invalidForm: boolean;
        container: DotContainerPayload;
    }>((state: DotContainerPropertiesState, { invalidForm, container }) => {
        return {
            ...state,
            isContentTypeButtonEnabled: container.maxContentlets > 0,
            invalidForm: isEqual(state.originalForm, container) || invalidForm
        };
    });

    /**
     * Update Original Form
     * @memberof DotContainerPropertiesStore
     */
    readonly updateOriginalFormState = this.updater<DotContainerPayload>(
        (state: DotContainerPropertiesState, originalForm: DotContainerPayload) => {
            return {
                ...state,
                originalForm: originalForm
            };
        }
    );

    /**
     * Update Content Type and PrePost loop visibility
     * @memberof DotContainerPropertiesStore
     */
    readonly updateContentTypeAndPrePostLoopVisibility = this.updater<{
        isContentTypeVisible: boolean;
        showPrePostLoopInput: boolean;
    }>(
        (
            state: DotContainerPropertiesState,
            { isContentTypeVisible, showPrePostLoopInput }: DotContainerPropertiesState
        ) => {
            return {
                ...state,
                isContentTypeVisible,
                showPrePostLoopInput
            };
        }
    );

    readonly updateContainerState = this.updater<DotContainerEntity>(
        (state: DotContainerPropertiesState, container: DotContainerEntity) => {
            return {
                ...state,
                container: container.container,
                containerStructures: container.contentTypes
            };
        }
    );

    readonly updateApiLink = this.updater<string>(
        (state: DotContainerPropertiesState, apiLink: string) => {
            return {
                ...state,
                apiLink
            };
        }
    );

    readonly updateContentTypes = this.updater<DotCMSContentType[]>(
        (state: DotContainerPropertiesState, contentTypes: DotCMSContentType[]) => {
            return {
                ...state,
                contentTypes
            };
        }
    );

    readonly loadContentTypesAndUpdateVisibility = this.effect<void>(
        pipe(
            switchMap(() => {
                return this.dotContentTypeService.getContentTypes({ per_page: 999 });
            }),
            tap((contentTypes: DotCMSContentType[]) => {
                this.updateContentTypes(contentTypes);
            }),
            catchError((err: HttpErrorResponse) => {
                this.dotHttpErrorManagerService.handle(err);

                return of(null);
            })
        )
    );

    readonly saveContainer = this.effect((origin$: Observable<DotContainerPayload>) => {
        return origin$.pipe(
            switchMap((container: DotContainerPayload) => {
                this.dotGlobalMessageService.loading(this.dotMessageService.get('publishing'));

                return this.dotContainersService.create(container);
            }),
            tap((container: DotContainerEntity) => {
                this.dotGlobalMessageService.success(
                    this.dotMessageService.get('message.container.published')
                );
                this.updateContainerState(container);
                this.dotRouterService.goToURL('/containers');
            }),
            catchError((err: HttpErrorResponse) => {
                this.dotGlobalMessageService.error(err.statusText);
                this.dotHttpErrorManagerService.handle(err);

                return of(null);
            })
        );
    });

    readonly editContainer = this.effect((origin$: Observable<DotContainerPayload>) => {
        return origin$.pipe(
            switchMap((container: DotContainerPayload) => {
                this.dotGlobalMessageService.loading(this.dotMessageService.get('update'));

                return this.dotContainersService.update(container);
            }),
            tap((container: DotContainerEntity) => {
                this.dotGlobalMessageService.success(
                    this.dotMessageService.get('message.container.updated')
                );
                this.updateContainerState(container);
                this.dotRouterService.goToURL('/containers');
            }),
            catchError((err: HttpErrorResponse) => {
                this.dotGlobalMessageService.error(err.statusText);
                this.dotHttpErrorManagerService.handle(err);

                return of(null);
            })
        );
    });

    /**
     * It returns a string that is the URL to the working directory of the container
     * @param {string} identifier - The container identifier.
     * @returns The API link for the container.
     */
    private getApiLink(identifier: string): string {
        return identifier
            ? `/api/v1/containers/working?containerId=${identifier}&includeContentType=true`
            : '';
    }
}
