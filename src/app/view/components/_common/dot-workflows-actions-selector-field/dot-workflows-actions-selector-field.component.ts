import {
    Component,
    Input,
    OnChanges,
    SimpleChanges,
    forwardRef,
    OnInit,
    ViewChild
} from '@angular/core';
import { NG_VALUE_ACCESSOR, ControlValueAccessor } from '@angular/forms';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';
import { SelectItemGroup, SelectItem, Dropdown } from 'primeng/primeng';

import { DotCMSWorkflowAction, DotCMSWorkflow } from 'dotcms-models';
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
    implements ControlValueAccessor, OnChanges, OnInit {
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

                if (actionsIds.length && !actionsIds.includes(this.value)) {
                    this.dropdown.clear(null);
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

    propagateChange = (_: any) => {};

    /**
     * Set the function to be called when the control receives a change event.
     *
     * @param {*} fn
     * @memberof DotWorkflowsActionsSelectorFieldComponent
     */
    registerOnChange(fn): void {
        this.propagateChange = fn;
    }

    registerOnTouched(): void {}

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
}
