import { Component, Input, OnChanges, SimpleChanges, forwardRef, OnInit } from '@angular/core';
import { NG_VALUE_ACCESSOR, ControlValueAccessor } from '@angular/forms';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { SelectItemGroup } from 'primeng/primeng';

import { DotCMSWorkflowAction, DotCMSWorkflow } from 'dotcms-models';
import { DotMessageService } from '@services/dot-messages-service';
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
    @Input() workflows: DotCMSWorkflow[];

    actions$: Observable<SelectItemGroup[]>;
    disabled = false;
    placeholder$: Observable<string>;
    value: string;

    constructor(
        private dotWorkflowsActionsSelectorFieldService: DotWorkflowsActionsSelectorFieldService,
        private dotMessageService: DotMessageService
    ) {}

    ngOnInit() {
        this.placeholder$ = this.getPlaceholder();
        this.actions$ = this.dotWorkflowsActionsSelectorFieldService.get();
    }

    ngOnChanges(changes: SimpleChanges) {
        this.dotWorkflowsActionsSelectorFieldService.load(changes.workflows.currentValue);
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

    private getPlaceholder(): Observable<string> {
        return this.dotMessageService
            .getMessages(['contenttypes.selector.workflow.action'])
            .pipe(
                map(
                    (message: { [key: string]: string }) =>
                        message['contenttypes.selector.workflow.action']
                )
            );
    }
}
