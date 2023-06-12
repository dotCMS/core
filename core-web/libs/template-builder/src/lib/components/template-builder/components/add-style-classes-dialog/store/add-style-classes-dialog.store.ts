import { ComponentStore, tapResponse } from '@ngrx/component-store';

import { Injectable } from '@angular/core';

import { switchMap } from 'rxjs/operators';

import { DotStyleClassesService } from '@dotcms/data-access';

import { DotAddStyleClassesDialogState } from '../../../models/models';

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

    constructor(private styleClassesService: DotStyleClassesService) {
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
                this.styleClassesService.getStyleClassesFromFile().pipe(
                    tapResponse(
                        ({ classes }: { classes: string[] }) => {
                            this.patchState({
                                styleClasses: classes.map((styleClasses) => ({
                                    cssClass: styleClasses
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
