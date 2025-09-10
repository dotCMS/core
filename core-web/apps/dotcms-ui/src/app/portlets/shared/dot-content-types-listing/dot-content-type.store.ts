import { ComponentStore } from '@ngrx/component-store';
import { Observable } from 'rxjs';

import { Injectable, inject } from '@angular/core';
import { Router } from '@angular/router';

import { catchError, switchMap, tap, withLatestFrom } from 'rxjs/operators';

import { DotContentTypeService, DotHttpErrorManagerService } from '@dotcms/data-access';
import { DotCMSAssetDialogFields, DotCopyContentTypeDialogFormFields } from '@dotcms/dotcms-models';

export type DotCMSAssetDialogCopyFields = DotCMSAssetDialogFields & {
    data: {
        icon: string;
        host: string;
    };
};

export interface ContentTypeState {
    isVisibleCloneDialog: boolean;
    assetSelected: string | null;
    isSaving: boolean;
}

const initialState: ContentTypeState = {
    isVisibleCloneDialog: false,
    assetSelected: null,
    isSaving: false
};

@Injectable()
export class DotContentTypeStore extends ComponentStore<ContentTypeState> {
    private readonly dotContentTypeService = inject(DotContentTypeService);
    private readonly httpErrorManagerService = inject(DotHttpErrorManagerService);
    private readonly router = inject(Router);

    readonly assetSelected$ = this.select(({ assetSelected }) => assetSelected);
    readonly isSaving$: Observable<boolean> = this.select(({ isSaving }) => isSaving);

    // UPDATERS
    readonly setAssetSelected = this.updater((state, assetSelected: string) => ({
        ...state,
        assetSelected,
        isSaving: false
    }));

    readonly isSaving = this.updater((state, isSaving: boolean) => ({
        ...state,
        isSaving
    }));

    // EFFECTS
    readonly saveCopyDialog = this.effect(
        (copyDialogFormFields$: Observable<DotCopyContentTypeDialogFormFields>) => {
            return copyDialogFormFields$.pipe(
                tap(() => this.isSaving(true)),
                withLatestFrom(this.assetSelected$),
                switchMap(([formFields, assetIdentifier]) =>
                    this.dotContentTypeService
                        .saveCopyContentType(assetIdentifier, formFields)
                        .pipe(
                            tap({
                                next: ({ id }) => {
                                    this.router.navigate(['/content-types-angular/edit', id]);
                                }
                            }),
                            catchError((error) => {
                                this.isSaving(false);

                                return this.httpErrorManagerService.handle(error);
                            })
                        )
                )
            );
        }
    );

    constructor() {
        super(initialState);
    }
}
