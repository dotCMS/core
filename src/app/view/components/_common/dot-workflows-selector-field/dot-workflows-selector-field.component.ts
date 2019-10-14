import { DotWorkflowService } from '@services/dot-workflow/dot-workflow.service';
import { Observable } from 'rxjs';
import { DotCMSWorkflow } from 'dotcms-models';
import { Component, OnInit, forwardRef } from '@angular/core';
import { DotMessageService } from '@services/dot-messages-service';
import { NG_VALUE_ACCESSOR, ControlValueAccessor } from '@angular/forms';

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
    ]
})
export class DotWorkflowsSelectorFieldComponent implements ControlValueAccessor, OnInit {
    options$: Observable<DotCMSWorkflow[]>;
    label$: Observable<string>;
    value: DotCMSWorkflow[] = [];
    disabled = false;
    messagesKey: { [key: string]: string } = {};

    constructor(
        private dotWorkflowService: DotWorkflowService,
        public dotMessageService: DotMessageService
    ) {}

    propagateChange = (_: any) => {};

    /**
     * Set the function to be called when the control receives a change event.
     *
     * @param {*} fn
     * @memberof DotWorkflowsSelectorFieldComponent
     */
    registerOnChange(fn): void {
        this.propagateChange = fn;
    }

    registerOnTouched(): void {}

    ngOnInit() {
        this.options$ = this.dotWorkflowService.get();
        this.label$ = this.dotMessageService.getMessages([
            'dot.common.select.workflows',
            'dot.common.archived'
        ]);
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
