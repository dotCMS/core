import { Subject } from 'rxjs';

import { AfterViewInit, ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { AutoCompleteModule } from 'primeng/autocomplete';
import { ButtonModule } from 'primeng/button';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

import { StyleClassModel } from '../../models/models';

@Component({
    selector: 'dotcms-add-style-classes-dialog',
    standalone: true,
    imports: [AutoCompleteModule, FormsModule, ButtonModule],
    templateUrl: './add-style-classes-dialog.component.html',
    styleUrls: ['./add-style-classes-dialog.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class AddStyleClassesDialogComponent implements OnInit, AfterViewInit {
    classes: StyleClassModel[];

    selectedClasses: StyleClassModel[] = [];

    filteredClasses: StyleClassModel[];

    private autoCompleteInput: HTMLInputElement;

    constructor(
        private ref: DynamicDialogRef,
        public dynamicDialogConfig: DynamicDialogConfig<{
            classes: Subject<StyleClassModel[]>;
            selectedClasses: string[];
        }>
    ) {}

    ngOnInit() {
        const { classes: classes$, selectedClasses } = this.dynamicDialogConfig.data;

        classes$.subscribe((classes) => {
            this.classes = classes;
        });

        this.selectedClasses = selectedClasses.map((klass) => ({
            klass
        }));
    }

    ngAfterViewInit() {
        this.autoCompleteInput = document.getElementById('my-autocomplete') as HTMLInputElement;
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

        //in a real application, make a request to a remote url with the query and return filtered results, for demo we filter at client side
        if ((query.includes(',') || query.includes(' ')) && query.trim().length > 0) {
            this.addClassByCommaOrSpace(query);

            return;
        }

        for (let i = 0; i < this.classes.length; i++) {
            const currentClass = this.classes[i];
            if (
                currentClass.klass.toLowerCase().indexOf(query.toLowerCase()) == 0 &&
                !this.selectedClasses.includes(currentClass)
            ) {
                filtered.push(currentClass);
            }
        }

        if (query.trim().length > 0 && !filtered.length) filtered.unshift({ klass: query.trim() });

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
        query = query.replace(/(,|\s)(.*)/, '');

        this.selectedClasses.push({ klass: query });

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
        this.ref.close(this.selectedClasses.map((styleClass) => styleClass.klass));
    }
}
