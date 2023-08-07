import { Subject } from 'rxjs';

import { AsyncPipe, NgIf } from '@angular/common';
import {
    AfterViewInit,
    ChangeDetectionStrategy,
    Component,
    OnDestroy,
    OnInit,
    ViewChild
} from '@angular/core';
import { FormsModule } from '@angular/forms';

import { AutoComplete, AutoCompleteModule } from 'primeng/autocomplete';
import { ButtonModule } from 'primeng/button';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotMessagePipe } from '@dotcms/ui';

import { DotAddStyleClassesDialogStore } from './store/add-style-classes-dialog.store';

import { StyleClassModel } from '../../models/models';

@Component({
    selector: 'dotcms-add-style-classes-dialog',
    standalone: true,
    imports: [AutoCompleteModule, FormsModule, ButtonModule, DotMessagePipe, NgIf, AsyncPipe],
    templateUrl: './add-style-classes-dialog.component.html',
    styleUrls: ['./add-style-classes-dialog.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class AddStyleClassesDialogComponent implements OnInit, AfterViewInit, OnDestroy {
    public vm$ = this.store.vm$;
    @ViewChild(AutoComplete)
    autoComplete: AutoComplete;
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

        this.store.init({ selectedClasses });

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
     * @description Filter the classes based on the query
     *
     * @param {{ query: string }} { query }
     * @memberof AddStyleClassesDialogComponent
     */
    filterClasses({ query }: { query: string }) {
        this.store.filterClasses(query);

        // To reset the input when the user types a comma or space
        if (query.includes(',') || query.includes(' ')) {
            this.autoCompleteInput.value = '';
        }
    }

    /**
     * @description Closes the dialog and returns the selected classes
     *
     * @memberof AddStyleClassesDialogComponent
     */
    saveClass(selectedClasses: StyleClassModel[]): void {
        this.ref.close(selectedClasses.map((styleClass) => styleClass.cssClass));
    }

    /**
     * @description Selects a class and adds it to the selected classes
     *
     * @param {StyleClassModel} newClass
     * @memberof AddStyleClassesDialogComponent
     */
    onSelect(newClass: StyleClassModel): void {
        this.store.addClass(newClass);
    }

    /**
     * @description Removes the last class from the selected classes
     *
     * @memberof AddStyleClassesDialogComponent
     */
    onUnselect(): void {
        this.store.removeLastClass();
    }

    /**
     * @description Used to listen for enter presses
     *
     * @param {KeyboardEvent} event
     * @memberof AddStyleClassesDialogComponent
     */
    onKeyUp(event: KeyboardEvent): void {
        if (event.key === 'Enter' && this.autoCompleteInput.value) {
            this.autoComplete.selectItem({ cssClass: this.autoCompleteInput.value });
        }
    }
}
