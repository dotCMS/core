import { Injectable } from '@angular/core';
import { ComponentStore } from '@ngrx/component-store';
import { Observable, of } from 'rxjs';
import { catchError, pluck, switchMap, take, tap } from 'rxjs/operators';
import { HttpErrorResponse } from '@angular/common/http';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { DotGlobalMessageService } from '@components/_common/dot-global-message/dot-global-message.service';
import {
    DotContainer,
    DotContainerEntity,
    DotContainerPayload,
    DotContainerStructure
} from '@dotcms/app/shared/models/container/dot-container.model';
import { DotContainersService } from '@services/dot-containers/dot-containers.service';
import { DotHttpErrorManagerService } from '@dotcms/app/api/services/dot-http-error-manager/dot-http-error-manager.service';
import { ActivatedRoute } from '@angular/router';

export interface DotContainerPropertiesState {
    showPrePostLoopInput: boolean;
    isContentTypeVisible: boolean;
    isContentTypeButtonEnabled: boolean;
    container: DotContainer;
    contentTypes: DotContainerStructure[];
    apiLink: string;
}

@Injectable()
export class DotContainerPropertiesStore extends ComponentStore<DotContainerPropertiesState> {
    constructor(
        private dotMessageService: DotMessageService,
        private dotGlobalMessageService: DotGlobalMessageService,
        private dotContainersService: DotContainersService,
        private dotHttpErrorManagerService: DotHttpErrorManagerService,
        private activatedRoute: ActivatedRoute
    ) {
        super({
            showPrePostLoopInput: false,
            isContentTypeVisible: false,
            isContentTypeButtonEnabled: false,
            contentTypes: [],
            container: null,
            apiLink: ''
        });
        this.activatedRoute.data
            .pipe(pluck('container'), take(1))
            .subscribe((containerEntity: DotContainerEntity) => {
                if (containerEntity) {
                    const { container, contentTypes } = containerEntity;
                    if (container && (container.preLoop || container.postLoop)) {
                        this.updatePrePostLoopAndContentTypeVisibility({
                            showPrePostLoopInput: true,
                            isContentTypeVisible: true,
                            container: container,
                            contentTypes: contentTypes ?? []
                        });
                    } else {
                        this.updateContainerState(containerEntity);
                    }

                    this.updateApiLink(container.identifier);
                }
            });
    }

    readonly vm$ = this.select((state: DotContainerPropertiesState) => {
        return state;
    });

    readonly containerAndStructure$ = this.select(
        ({ container, contentTypes }: DotContainerPropertiesState) => {
            return {
                container,
                contentTypes
            };
        }
    );

    readonly updatePrePostLoopAndContentTypeVisibility = this.updater<{
        showPrePostLoopInput: boolean;
        isContentTypeVisible: boolean;
        container: DotContainer;
        contentTypes: DotContainerStructure[];
    }>(
        (
            state: DotContainerPropertiesState,
            {
                showPrePostLoopInput,
                isContentTypeVisible,
                container,
                contentTypes
            }: DotContainerPropertiesState
        ) => {
            return {
                ...state,
                showPrePostLoopInput,
                isContentTypeVisible,
                container,
                contentTypes
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

    readonly updateIsContentTypeButtonEnabled = this.updater<boolean>(
        (state: DotContainerPropertiesState, isContentTypeButtonEnabled: boolean) => {
            return {
                ...state,
                isContentTypeButtonEnabled
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

    readonly updateContainerState = this.updater<DotContainerEntity>(
        (state: DotContainerPropertiesState, container: DotContainerEntity) => {
            return {
                ...state,
                container: container.container,
                contentTypes: container.contentTypes
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
        return identifier ? `/api/v1/containers/${identifier}/working` : '';
    }
}
