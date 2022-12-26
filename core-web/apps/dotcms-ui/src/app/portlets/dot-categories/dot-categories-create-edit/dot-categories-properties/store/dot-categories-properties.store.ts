import { Injectable } from '@angular/core';
import { ComponentStore } from '@ngrx/component-store';
import { Observable, of } from 'rxjs';
import { catchError, switchMap, tap } from 'rxjs/operators';
import { HttpErrorResponse } from '@angular/common/http';

import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { DotHttpErrorManagerService } from '@dotcms/app/api/services/dot-http-error-manager/dot-http-error-manager.service';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { DotCategoriesUtillService } from '@dotcms/app/api/services/dot-categories/dot-categories-utill.service';
import { DotGlobalMessageService } from '@components/_common/dot-global-message/dot-global-message.service';

import {
    DotCategory,
    DotCategoryPayload
} from '@dotcms/app/shared/models/dot-categories/dot-categories.model';

export interface DotCategoriesPropertiesState {
    category: DotCategory;
    apiLink: string;
}

@Injectable()
export class DotCategoriesPropertiesStore extends ComponentStore<DotCategoriesPropertiesState> {
    constructor(
        private dotMessageService: DotMessageService,
        private dotGlobalMessageService: DotGlobalMessageService,
        private dotCategoriesUtillService: DotCategoriesUtillService,
        private dotHttpErrorManagerService: DotHttpErrorManagerService,
        private dotRouterService: DotRouterService
    ) {
        super({
            category: null,
            apiLink: ''
        });
    }

    readonly vm$ = this.select((state: DotCategoriesPropertiesState) => {
        return state;
    });

    readonly category$ = this.select(({ category }: DotCategoriesPropertiesState) => {
        return {
            category
        };
    });

    /**
     * Updates the content type state.
     * @return boolean
     * @memberof DotCategoriesPropertiesStore
     */
    readonly updateIsContentTypeButtonEnabled = this.updater<boolean>(
        (state: DotCategoriesPropertiesState) => {
            return {
                ...state
            };
        }
    );

    /**
     * Updates the category state.
     * @return DotCategory
     * @memberof DotCategoriesPropertiesStore
     */
    readonly updateCategoryState = this.updater<DotCategory>(
        (state: DotCategoriesPropertiesState, category: DotCategory) => {
            return {
                ...state,
                category: category
            };
        }
    );

    /**
     * Saves the category state make http call and updates the state.
     * @return DotCategoryPayload
     * @memberof DotCategoriesPropertiesStore
     */
    readonly saveCategory = this.effect((origin$: Observable<DotCategoryPayload>) => {
        return origin$.pipe(
            switchMap((category: DotCategoryPayload) => {
                this.dotGlobalMessageService.loading(this.dotMessageService.get('publishing'));

                return this.dotCategoriesUtillService.create(category);
            }),
            tap((category: DotCategory) => {
                this.dotGlobalMessageService.success(
                    this.dotMessageService.get('message.container.published')
                );
                this.updateCategoryState(category);
                this.dotRouterService.goToURL('/categories');
            }),
            catchError((err: HttpErrorResponse) => {
                this.dotGlobalMessageService.error(err.statusText);
                this.dotHttpErrorManagerService.handle(err);

                return of(null);
            })
        );
    });

    /**
     * Updated the category state make http call and updates the state.
     * @return DotCategoryPayload
     * @memberof DotCategoriesPropertiesStore
     */
    readonly editCategory = this.effect((origin$: Observable<DotCategoryPayload>) => {
        return origin$.pipe(
            switchMap((category: DotCategoryPayload) => {
                this.dotGlobalMessageService.loading(this.dotMessageService.get('update'));

                return this.dotCategoriesUtillService.update(category);
            }),
            tap((category: DotCategory) => {
                this.dotGlobalMessageService.success(
                    this.dotMessageService.get('message.container.updated')
                );
                this.updateCategoryState(category);
            }),
            catchError((err: HttpErrorResponse) => {
                this.dotGlobalMessageService.error(err.statusText);
                this.dotHttpErrorManagerService.handle(err);

                return of(null);
            })
        );
    });
}
