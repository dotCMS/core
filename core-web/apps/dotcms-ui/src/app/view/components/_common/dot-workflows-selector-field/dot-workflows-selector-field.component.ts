import { Observable } from 'rxjs';

import { CommonModule } from '@angular/common';
import { Component, forwardRef, inject, OnInit } from '@angular/core';
import { ControlValueAccessor, FormsModule, NG_VALUE_ACCESSOR } from '@angular/forms';

import { MultiSelectModule } from 'primeng/multiselect';

import { DotWorkflowService } from '@dotcms/data-access';
import { DotCMSWorkflow } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

@Component({
    selector: 'dot-workflows-selector-field',
    templateUrl: './dot-workflows-selector-field.component.html',
    styleUrls: ['./dot-workflows-selector-field.component.scss'],
    providers: [
        {
            multi: true,
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => DotWorkflowsSelectorFieldComponent)
        }
    ],
    imports: [CommonModule, FormsModule, MultiSelectModule, DotMessagePipe]
})
export class DotWorkflowsSelectorFieldComponent implements ControlValueAccessor, OnInit {
    private dotWorkflowService = inject(DotWorkflowService);

    options$: Observable<DotCMSWorkflow[]>;
    value: DotCMSWorkflow[] = [];
    disabled = false;

    propagateChange = (_: unknown) => {
        /**/
    };

    /**
     * Set the function to be called when the control receives a change event.
     *
     * @param {*} fn
     * @memberof DotWorkflowsSelectorFieldComponent
     */
    registerOnChange(fn): void {
        this.propagateChange = fn;
    }

    registerOnTouched(): void {
        /**/
    }

    ngOnInit() {
        this.options$ = this.dotWorkflowService.get();
    }

    /**
     * Update value on change of the multiselect
     *
     * @param {DotCMSWorkflow[]} value
     * @memberof DotWorkflowsSelectorFieldComponent
     */
    handleChange(value: DotCMSWorkflow[]): void {
        this.propagateChange(value);
    }

    /**
     * Set disable state.
     *
     * @param boolean isDisabled
     * @memberof DotWorkflowsSelectorFieldComponent
     */
    setDisabledState(isDisabled: boolean): void {
        this.disabled = isDisabled;
    }

    /**
     * Write a new value to the element
     *
     * @param {DotCMSWorkflow[]} value
     * @memberof DotWorkflowsSelectorFieldComponent
     */
    writeValue(value: DotCMSWorkflow[]): void {
        this.value = value;
    }
}
