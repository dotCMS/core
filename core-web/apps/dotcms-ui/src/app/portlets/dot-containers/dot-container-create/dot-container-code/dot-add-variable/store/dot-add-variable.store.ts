import { Injectable } from '@angular/core';
import { ComponentStore } from '@ngrx/component-store';
import { DotCMSContentType, DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { catchError, switchMap, tap } from 'rxjs/operators';
import { DotContentTypeService } from '@dotcms/data-access';
import { Observable, of } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { DotGlobalMessageService } from '@components/_common/dot-global-message/dot-global-message.service';
import { DotHttpErrorManagerService } from '@services/dot-http-error-manager/dot-http-error-manager.service';

export interface DotAddVariableState {
    variables: DotCMSContentTypeField[];
}

@Injectable()
export class DotAddVariableStore extends ComponentStore<DotAddVariableState> {
    constructor(
        private dotContentTypeService: DotContentTypeService,
        private dotGlobalMessageService: DotGlobalMessageService,
        private dotHttpErrorManagerService: DotHttpErrorManagerService
    ) {
        super({
            variables: []
        });
    }

    readonly vm$ = this.select(({ variables }) => {
        return {
            variables
        };
    });

    readonly updateVariables = this.updater<DotCMSContentTypeField[]>(
        (state: DotAddVariableState, variables: DotCMSContentTypeField[]) => {
            return {
                ...state,
                variables
            };
        }
    );

    readonly getVariables = this.effect((origin$: Observable<string>) => {
        return origin$.pipe(
            switchMap((containerVariable) => {
                return this.dotContentTypeService.getContentType(containerVariable);
            }),
            tap((contentType: DotCMSContentType) => {
                this.updateVariables(contentType.fields);
            }),
            catchError((err: HttpErrorResponse) => {
                this.dotGlobalMessageService.error(err.statusText);
                this.dotHttpErrorManagerService.handle(err);

                return of(null);
            })
        );
    });
}
