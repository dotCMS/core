import { Subject } from 'rxjs';

import {
    AfterViewInit,
    ChangeDetectionStrategy,
    Component,
    OnDestroy,
    OnInit
} from '@angular/core';
import { FormsModule } from '@angular/forms';

import { AutoCompleteModule } from 'primeng/autocomplete';
import { ButtonModule } from 'primeng/button';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

import { takeUntil } from 'rxjs/operators';

import { DotMessagePipeModule } from '@dotcms/ui';

import { DotAddStyleClassesDialogStore } from './store/add-style-classes-dialog.store';

import { StyleClassModel } from '../../models/models';

const COMMA_SPACES_REGEX = /(,|\s)(.*)/;

@Component({
    selector: 'dotcms-add-style-classes-dialog',
    standalone: true,
    imports: [AutoCompleteModule, FormsModule, ButtonModule, DotMessagePipeModule],
    providers: [DotAddStyleClassesDialogStore],
    templateUrl: './add-style-classes-dialog.component.html',
    styleUrls: ['./add-style-classes-dialog.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class AddStyleClassesDialogComponent implements OnInit, AfterViewInit, OnDestroy {
    classes: StyleClassModel[];

    selectedClasses: StyleClassModel[] = [];

    filteredClasses: StyleClassModel[];

    private autoCompleteInput: HTMLInputElement;

    private destroy$: Subject<void> = new Subject<void>();

    constructor(
        private ref: DynamicDialogRef,
        private store: DotAddStyleClassesDialogStore,
        public dynamicDialogConfig: DynamicDialogConfig<{
            selectedClasses: string[];
        }>
    ) {}

    ngOnInit() {
        const { selectedClasses } = this.dynamicDialogConfig.data;

        this.selectedClasses = selectedClasses.map((cssClass) => ({
            cssClass
        }));

        this.store.styleClasses$.pipe(takeUntil(this.destroy$)).subscribe((classes) => {
            this.classes = classes;
        });

        this.store.fetchStyleClasses();
    }

    ngAfterViewInit() {
        this.autoCompleteInput = document.getElementById('auto-complete-input') as HTMLInputElement;
    }

    ngOnDestroy() {
        this.destroy$.next();
        this.destroy$.complete();
    }

    /**
     * @description Filters the classes based on the query
     *
     * @param {{ query: string }} { query }
     * @return {*}
     * @memberof AddStyleClassesDialogComponent
     */
    filterClasses({ query }: { query: string }): void {
        const queryIsNotEmpty = query.trim().length > 0;
        const queryContainsDelimiter = query.includes(',') || query.includes(' ');

        if (queryIsNotEmpty && queryContainsDelimiter) {
            this.addClassByCommaOrSpace(query);

            return;
        }

        this.filteredClasses = this.getFilteredClasses(query, queryIsNotEmpty);
    }
    /**
     * Returns the filtered classes
     *
     * @param {string} query
     * @param {boolean} queryIsNotEmpty
     * @return {StyleClassModel[]}
     */
    private getFilteredClasses(query: string, queryIsNotEmpty: boolean): StyleClassModel[] {
        const filtered: StyleClassModel[] = [];

        this.classes.forEach((classObj) => {
            if (this.classMatchesQuery(query, classObj) && !this.classAlreadySelected(classObj)) {
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
    private classAlreadySelected(classObj: StyleClassModel): boolean {
        return this.selectedClasses.some(({ cssClass }) => cssClass === classObj.cssClass);
    }

    /**
     * @description Adds a class to the selected classes if it founds a comma or a space in the query
     *
     * @param {string} query
     * @memberof AddStyleClassesDialogComponent
     */
    addClassByCommaOrSpace(query: string): void {
        // Removes all the chars after the comma or space
        query = query.replace(COMMA_SPACES_REGEX, '');

        this.selectedClasses.push({ cssClass: query });

        // Reset the input value
        this.autoCompleteInput.value = '';
        // Reset the filtered classes
        this.filteredClasses = [];
    }

    /**
     * @description Closes the dialog and returns the selected classes
     *
     * @memberof AddStyleClassesDialogComponent
     */
    saveClass(): void {
        this.ref.close(this.selectedClasses.map((styleClass) => styleClass.cssClass));
    }
}
