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

import { DotAddStyleClassesDialogStore } from './store/add-style-classes-dialog.store';

import { StyleClassModel } from '../../models/models';

const COMMA_SPACES_REGEX = /(,|\s)(.*)/;

@Component({
    selector: 'dotcms-add-style-classes-dialog',
    standalone: true,
    imports: [AutoCompleteModule, FormsModule, ButtonModule],
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

        this.store.getStyleClassesFromFile();
    }

    ngAfterViewInit() {
        this.autoCompleteInput = document.getElementById('my-autocomplete') as HTMLInputElement;
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
        const filtered: StyleClassModel[] = [];

        if (query.trim().length && (query.includes(',') || query.includes(' '))) {
            this.addClassByCommaOrSpace(query);

            return;
        }

        for (const classObj of this.classes) {
            const queryLowerCased = query.toLowerCase();
            const cssClassLowerCased = classObj.cssClass.toLowerCase();

            const isKlassStartsWithQuery = cssClassLowerCased.startsWith(queryLowerCased);
            const isKlassAlreadySelected = this.selectedClasses.some(
                ({ cssClass: cssClass }) => cssClass === classObj.cssClass
            );

            if (isKlassStartsWithQuery && !isKlassAlreadySelected) {
                filtered.push(classObj);
            }
        }

        if (query.trim().length && !filtered.length) {
            filtered.push({ cssClass: query.trim() });
        }

        this.filteredClasses = filtered;
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
    closeDialog(): void {
        this.ref.close(this.selectedClasses.map((styleClass) => styleClass.cssClass));
    }
}
