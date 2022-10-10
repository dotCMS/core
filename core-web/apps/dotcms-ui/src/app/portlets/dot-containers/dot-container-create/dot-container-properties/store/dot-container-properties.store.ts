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
    DotContainerRequest,
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
    containerStructures: DotContainerStructure[];
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
            containerStructures: [],
            container: null
        });
        this.activatedRoute.data
            .pipe(pluck('container'), take(1))
            .subscribe((containerEntity: DotContainerEntity) => {
                if (containerEntity) {
                    const { container, containerStructures } = containerEntity;
                    if (container.preLoop || container.postLoop) {
                        this.updatePrePostLoopAndContentTypeVisibility({
                            showPrePostLoopInput: true,
                            isContentTypeVisible: true,
                            container: container,
                            containerStructures: containerStructures ?? []
                        });
                    } else {
                        this.updateContainerState(containerEntity);
                    }
                }
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

    readonly updateIsContentTypeButtonEnabled = this.updater<boolean>(
        (state: DotContainerPropertiesState, isContentTypeButtonEnabled: boolean) => {
            return {
                ...state,
                isContentTypeButtonEnabled
            };
        }
    );

    readonly updateContentTypeVisibilty = this.updater<boolean>(
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
                containerStructures: container.containerStructures
            };
        }
    );

    readonly saveContainer = this.effect((origin$: Observable<DotContainerRequest>) => {
        return origin$.pipe(
            switchMap((container: DotContainerRequest) => {
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

    readonly editContainer = this.effect((origin$: Observable<DotContainerRequest>) => {
        return origin$.pipe(
            switchMap((container: DotContainerRequest) => {
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
}
