import { ComponentStore, tapResponse } from '@ngrx/component-store';
import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { switchMap } from 'rxjs/operators';

import { DotAddStyleClassesDialogState } from '../../../models/models';

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
    readonly fetchStyleClasses = this.effect((trigger$) => {
        return trigger$.pipe(
            switchMap(() =>
                this.getStyleClassesFromFile().pipe(
                    // This operator is used to handle the error
                    tapResponse(
                        // 200 response
                        ({ classes = [] }: { classes: string[] }) => {
                            this.patchState({
                                styleClasses: classes.map((cssClass) => ({
                                    cssClass
                                }))
                            });
                        },
                        // Here is the error, if it fails for any reason I just fill the state with an empty array
                        (_) => {
                            this.patchState({ styleClasses: [] });
                        }
                    )
                )
            )
        );
    });

    /* @description This method fetchs the style classes from  "/application/templates/classes.json"
     *
     * @return {*}  {Observable<object>}
     * @memberof DotAddStyleClassesDialogStore
     */
    getStyleClassesFromFile(): Observable<object> {
        return this.http.get(STYLE_CLASSES_FILE_URL);
    }
}
