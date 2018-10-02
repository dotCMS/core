import { Component, OnInit, Output, EventEmitter } from '@angular/core';
import { FormGroup, FormControl, Validators, FormBuilder } from '@angular/forms';
import { DotMessageService } from '../../../../../api/services/dot-messages-service';
import { FieldVariable } from '..';

@Component({
    selector: 'dot-add-variable-form',
    styleUrls: ['./add-variable-form.component.scss'],
    templateUrl: './add-variable-form.component.html'
})
export class AddVariableFormComponent implements OnInit {

    @Output() saveVariable = new EventEmitter<FieldVariable>();

    messages: {[key: string]: string} = {};
    form: FormGroup;

    constructor(public dotMessageService: DotMessageService, private fb: FormBuilder) {}

    ngOnInit(): void {
        this.dotMessageService
            .getMessages([
                'contenttypes.field.variables.key_header.label',
                'contenttypes.field.variables.value_header.label',
                'contenttypes.field.variables.add_button.label'
            ])
            .subscribe((messages: {[key: string]: string}) => {
                this.messages = messages;
            });
        this.form = this.fb.group({
            key: new FormControl('', Validators.required),
            value: new FormControl('', Validators.required)
        });
    }

    /**
     * Handle Create Variable event from form
     * @memberof ContentTypeFieldsVariablesComponent
     */
    addVariableForm(): void {
        const variable: FieldVariable = {
            key: this.form.controls.key.value,
            value: this.form.controls.value.value
        };
        this.saveVariable.emit(variable);
        this.form.reset();
    }
}
