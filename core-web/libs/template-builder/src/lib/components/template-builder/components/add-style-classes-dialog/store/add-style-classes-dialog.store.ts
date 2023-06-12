import { ComponentStore, tapResponse } from '@ngrx/component-store';

import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { switchMap } from 'rxjs/operators';

import { DotRemoveConfirmDialogState as DotAddStyleClassesDialogState } from '../../../models/models';

export const STYLE_CLASSES_FILE_URL = '/application/templates/classes.json';

/**
 *
 *
 * @export
 * @class DotAddStyleClassesDialogStore
 * @extends {ComponentStore<DotAddStyleClassesDialogState>}
 */
@Injectable()
export class DotAddStyleClassesDialogStore extends ComponentStore<DotAddStyleClassesDialogState> {
    public styleClasses$ = this.select((state) => state.styleClasses);

    constructor(private http: HttpClient) {
        super({ styleClasses: [] });
    }

    // Effects
    /**
     * @description This effect fetchs the style classes from the file only once
     *
     * @memberof DotAddStyleClassesDialogStore
     */
    readonly getStyleClassesFromFile = this.effect((trigger$) => {
        return trigger$.pipe(
            switchMap(() =>
                this.http.get(STYLE_CLASSES_FILE_URL).pipe(
                    tapResponse(
                        ({ classes }: { classes: string[] }) => {
                            this.patchState({
                                styleClasses: classes.map((styleClasses) => ({
                                    klass: styleClasses
                                }))
                            });
                        },
                        (_) => {
                            this.patchState({ styleClasses: [] });
                        }
                    )
                )
            )
        );
    });
}
