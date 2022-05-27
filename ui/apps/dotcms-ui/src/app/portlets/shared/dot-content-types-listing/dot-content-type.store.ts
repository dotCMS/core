import { ComponentStore } from '@ngrx/component-store';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { DotCMSAssetDialogFields, DotCopyContentTypeDialogFormFields } from '@dotcms/dotcms-models';
import { DotContentTypeService } from '@services/dot-content-type';
import { catchError, switchMap, tap, withLatestFrom } from 'rxjs/operators';
import { DotHttpErrorManagerService } from '@services/dot-http-error-manager/dot-http-error-manager.service';
import { Router } from '@angular/router';

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

    constructor(
        private readonly dotContentTypeService: DotContentTypeService,
        private readonly httpErrorManagerService: DotHttpErrorManagerService,
        private readonly router: Router
    ) {
        super(initialState);
    }
}
