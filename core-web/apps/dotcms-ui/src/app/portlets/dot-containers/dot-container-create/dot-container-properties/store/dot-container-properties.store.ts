import { Injectable } from '@angular/core';
import { ComponentStore } from '@ngrx/component-store';
import { Observable, of } from 'rxjs';
import { catchError, switchMap, tap } from 'rxjs/operators';
import { HttpErrorResponse } from '@angular/common/http';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { DotGlobalMessageService } from '@components/_common/dot-global-message/dot-global-message.service';
import {
    DotContainer,
    DotContainerRequest
} from '@dotcms/app/shared/models/container/dot-container.model';
import { DotContainersService } from '@services/dot-containers/dot-containers.service';
import { DotHttpErrorManagerService } from '@dotcms/app/api/services/dot-http-error-manager/dot-http-error-manager.service';

export interface DotContainerPropertiesState {
    showPrePostLoopInput: boolean;
    isContentTypeVisible: boolean;
    isContentTypeButtonEnabled: boolean;
}

@Injectable()
export class DotContainerPropertiesStore extends ComponentStore<DotContainerPropertiesState> {
    constructor(
        private dotMessageService: DotMessageService,
        private dotGlobalMessageService: DotGlobalMessageService,
        private dotContainersService: DotContainersService,
        private dotHttpErrorManagerService: DotHttpErrorManagerService
    ) {
        super({
            showPrePostLoopInput: false,
            isContentTypeVisible: false,
            isContentTypeButtonEnabled: false
        });
    }

    readonly vm$ = this.select(
        ({
            showPrePostLoopInput,
            isContentTypeVisible,
            isContentTypeButtonEnabled
        }: DotContainerPropertiesState) => {
            return {
                showPrePostLoopInput,
                isContentTypeVisible,
                isContentTypeButtonEnabled
            };
        }
    );

    readonly updatePrePostLoopAndContentTypeVisibility = this.updater<{
        showPrePostLoopInput: boolean;
        isContentTypeVisible: boolean;
    }>(
        (
            state: DotContainerPropertiesState,
            { showPrePostLoopInput, isContentTypeVisible }: DotContainerPropertiesState
        ) => {
            return {
                ...state,
                showPrePostLoopInput,
                isContentTypeVisible
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

    readonly updateContainerState = this.updater<DotContainer>(
        (state: DotContainerPropertiesState, container: DotContainer) => {
            return {
                ...state,
                container
            };
        }
    );

    readonly saveContainer = this.effect((origin$: Observable<DotContainerRequest>) => {
        return origin$.pipe(
            switchMap((container: DotContainerRequest) => {
                this.dotGlobalMessageService.loading(this.dotMessageService.get('publishing'));

                return this.dotContainersService.create(container);
            }),
            tap((container: DotContainer) => {
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
            tap((container: DotContainer) => {
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
