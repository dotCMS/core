import { ComponentStore, tapResponse } from '@ngrx/component-store';
import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { switchMap } from 'rxjs/operators';

import { DotAddStyleClassesDialogState, StyleClassModel } from '../../../models/models';

export const STYLE_CLASSES_FILE_URL = '/application/templates/classes.json';

const COMMA_SPACES_REGEX = /(,|\s)(.*)/;

/**
 *
 *
 * @export
 * @class DotAddStyleClassesDialogStore
 * @extends {ComponentStore<DotAddStyleClassesDialogState>}
 */
@Injectable()
export class DotAddStyleClassesDialogStore extends ComponentStore<DotAddStyleClassesDialogState> {
    public vm$ = this.select((state) => ({
        filteredClasses: state.filteredClasses,
        selectedClasses: state.selectedClasses,
        styleClasses: state.styleClasses
    }));

    constructor(private http: HttpClient) {
        super({ styleClasses: [], selectedClasses: [], filteredClasses: [] });
    }

    readonly init = this.updater((state, { selectedClasses }: { selectedClasses: string[] }) => {
        return {
            ...state,
            selectedClasses: selectedClasses.map((cssClass) => ({
                cssClass
            }))
        };
    });

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

    // Updaters

    /**
     * @description Filters the classes based on the query
     *
     * @param { query: string } { query }
     * @return {*}
     * @memberof DotAddStyleClassesDialogStore
     */
    readonly filterClasses = this.updater((state, query: string) => {
        const { styleClasses, selectedClasses } = state;
        const queryIsNotEmpty = query.trim().length > 0;

        // To select the text if it has "," or space
        const queryContainsDelimiter = query.includes(',') || query.includes(' ');

        if (queryIsNotEmpty && queryContainsDelimiter)
            return {
                ...state,
                filteredClasses: [], // I need to reset the filter, because I'm doing the selection manually
                selectedClasses: [
                    ...state.selectedClasses,
                    { cssClass: query.replace(COMMA_SPACES_REGEX, '') }
                ]
            };

        return {
            ...state,
            filteredClasses: this.getFilteredClasses({
                query,
                queryIsNotEmpty,
                styleClasses,
                selectedClasses
            })
        };
    });

    /**
     * @description This method removes the last class from the selected classes
     *
     * @memberof DotAddStyleClassesDialogStore
     */
    readonly removeLastClass = this.updater((state) => {
        return {
            ...state,
            selectedClasses: state.selectedClasses.slice(0, -1)
        };
    });

    /**
     * @description This method adds a class to the selected classes
     *
     * @memberof DotAddStyleClassesDialogStore
     */
    readonly addClass = this.updater((state, classToAdd: StyleClassModel) => {
        return {
            ...state,
            selectedClasses: [...state.selectedClasses, classToAdd]
        };
    });

    // Util methods

    /**
     * @description This method fetchs the style classes from  "/application/templates/classes.json"
     *
     * @return {*}  {Observable<object>}
     * @memberof DotAddStyleClassesDialogStore
     */
    private getStyleClassesFromFile(): Observable<object> {
        return this.http.get(STYLE_CLASSES_FILE_URL);
    }

    /**
     * @description This method filters the classes based on the query and if the class is already selected
     *
     * @private
     * @param {{
     *         query: string;
     *         queryIsNotEmpty: boolean;
     *         styleClasses: StyleClassModel[];
     *         selectedClasses: StyleClassModel[];
     *     }} {
     *         query,
     *         queryIsNotEmpty,
     *         styleClasses,
     *         selectedClasses
     *     }
     * @return {*}  {StyleClassModel[]}
     * @memberof DotAddStyleClassesDialogStore
     */
    private getFilteredClasses({
        query,
        queryIsNotEmpty,
        styleClasses,
        selectedClasses
    }: {
        query: string;
        queryIsNotEmpty: boolean;
        styleClasses: StyleClassModel[];
        selectedClasses: StyleClassModel[];
    }): StyleClassModel[] {
        const filtered: StyleClassModel[] = [];

        styleClasses.forEach((classObj) => {
            if (
                this.classMatchesQuery(query, classObj) &&
                !this.classAlreadySelected(classObj, selectedClasses)
            ) {
                filtered.push(classObj);
            }
        });

        // If no classes were found and query is not empty, create a new class based on the query
        if (queryIsNotEmpty && filtered.length === 0) {
            filtered.push({ cssClass: query.trim() });
        }

        return filtered;
    }

    /**
     * Checks if a class matches the query
     *
     * @param {string} query
     * @param {StyleClassModel} classObj
     * @return {boolean}
     */
    private classMatchesQuery(query: string, classObj: StyleClassModel): boolean {
        const queryLowerCased = query.toLowerCase();
        const cssClassLowerCased = classObj.cssClass.toLowerCase();

        return cssClassLowerCased.startsWith(queryLowerCased);
    }

    /**
     * Checks if a class is already selected
     *
     * @param {StyleClassModel} classObj
     * @return {boolean}
     */
    private classAlreadySelected(
        classObj: StyleClassModel,
        selectedClasses: StyleClassModel[]
    ): boolean {
        return selectedClasses.some(({ cssClass }) => cssClass === classObj.cssClass);
    }
}
