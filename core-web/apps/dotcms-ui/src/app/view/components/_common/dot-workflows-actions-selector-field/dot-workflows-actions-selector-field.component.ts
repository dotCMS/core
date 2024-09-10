import { Observable } from 'rxjs';

import {
    Component,
    forwardRef,
    Input,
    OnChanges,
    OnInit,
    SimpleChanges,
    ViewChild
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

import { SelectItem, SelectItemGroup } from 'primeng/api';
import { Dropdown } from 'primeng/dropdown';

import { tap } from 'rxjs/operators';

import { DotCMSWorkflow, DotCMSWorkflowAction } from '@dotcms/dotcms-models';

import { DotWorkflowsActionsSelectorFieldService } from './services/dot-workflows-actions-selector-field.service';

interface DropdownEvent {
    originalEvent: MouseEvent;
    value: DotCMSWorkflowAction;
}

@Component({
    selector: 'dot-workflows-actions-selector-field',
    templateUrl: './dot-workflows-actions-selector-field.component.html',
    styleUrls: ['./dot-workflows-actions-selector-field.component.scss'],
    providers: [
        {
            multi: true,
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => DotWorkflowsActionsSelectorFieldComponent)
        }
    ]
})
export class DotWorkflowsActionsSelectorFieldComponent
    implements ControlValueAccessor, OnChanges, OnInit
{
    @ViewChild('dropdown') dropdown: Dropdown;
    @Input() workflows: DotCMSWorkflow[];

    actions$: Observable<SelectItemGroup[]>;
    disabled = false;
    value: string;

    constructor(
        private dotWorkflowsActionsSelectorFieldService: DotWorkflowsActionsSelectorFieldService
    ) {}

    ngOnInit() {
        this.actions$ = this.dotWorkflowsActionsSelectorFieldService.get().pipe(
            tap((actions: SelectItemGroup[]) => {
                const actionsIds = this.getActionsIds(actions);

                if (this.shouldClearDropdown(this.dropdown, actionsIds, this.value)) {
                    this.dropdown.clear();
                }
            })
        );
        this.dotWorkflowsActionsSelectorFieldService.load(this.workflows);
    }

    ngOnChanges(changes: SimpleChanges) {
        if (!changes.workflows.firstChange) {
            this.dotWorkflowsActionsSelectorFieldService.load(changes.workflows.currentValue);
        }
    }

    /**
     * Update value on change of the multiselect
     *
     * @param {DropdownEvent} { value }
     * @memberof DotWorkflowsActionsSelectorFieldComponent
     */
    handleChange({ value }: DropdownEvent): void {
        this.propagateChange(value || '');
    }

    propagateChange = (_: unknown) => {
        /**/
    };

    /**
     * Set the function to be called when the control receives a change event.
     *
     * @param {*} fn
     * @memberof DotWorkflowsActionsSelectorFieldComponent
     */
    registerOnChange(fn): void {
        this.propagateChange = fn;
    }

    registerOnTouched(): void {
        /**/
    }

    /**
     * Set disable state.
     *
     * @param {boolean} value
     * @memberof DotWorkflowsActionsSelectorFieldComponent
     */
    setDisabledState(value: boolean): void {
        this.disabled = value;
    }

    /**
     * Write a new value to the element
     *
     * @param {string} value
     * @memberof DotWorkflowsActionsSelectorFieldComponent
     */
    writeValue(value: string): void {
        if (value) {
            this.value = value;
        }
    }

    private getActionsIds(actions: SelectItemGroup[]): string[] {
        return actions.reduce((acc: string[], { items }: SelectItemGroup) => {
            return [...acc, ...items.map((item: SelectItem) => item.value)];
        }, []);
    }

    /**
     * Determines whether the dropdown should be cleared based on the provided options and the current value.
     *
     * @param {Dropdown} dropdown - The dropdown component instance.
     * @param {string[]} options - Array of available options for the dropdown.
     * @param {string} value - The current value selected in the dropdown.
     * @returns {boolean} - Returns `true` if the dropdown should be cleared (i.e., if the dropdown exists, there are available options,
     *                      and the current value is not in the list of options). Otherwise, returns `false`.
     */
    private shouldClearDropdown(dropdown: Dropdown, options: string[], value: string): boolean {
        return dropdown && options.length && !options.includes(value);
    }
}
