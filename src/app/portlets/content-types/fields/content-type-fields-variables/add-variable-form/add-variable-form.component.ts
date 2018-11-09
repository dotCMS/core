import { Component, OnInit, Output, EventEmitter, ViewChild, ElementRef } from '@angular/core';
import { FormGroup, FormControl, Validators, FormBuilder } from '@angular/forms';
import { DotMessageService } from '../../../../../api/services/dot-messages-service';
import { FieldVariable } from '..';
import { take } from 'rxjs/operators';

@Component({
    selector: 'dot-add-variable-form',
    styleUrls: ['./add-variable-form.component.scss'],
    templateUrl: './add-variable-form.component.html'
})
export class AddVariableFormComponent implements OnInit {
    @ViewChild('key') keyField: ElementRef;

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
            .pipe(take(1))
            .subscribe((messages: {[key: string]: string}) => {
                this.messages = messages;
            });
        this.form = this.getNewForm();
    }

    /**
     * Handle Create Variable event from form
     * @memberof ContentTypeFieldsVariablesComponent
     */
    addVariableForm($event: Event | KeyboardEvent): void {
        $event.stopPropagation();

        if (this.form.valid) {
            const variable: FieldVariable = {
                key: this.form.controls.key.value,
                value: this.form.controls.value.value
            };
            this.saveVariable.emit(variable);
            this.form = this.getNewForm();
            this.keyField.nativeElement.focus();
        }

    }

    private getNewForm(): FormGroup {
        return this.fb.group({
            key: new FormControl('', Validators.required),
            value: new FormControl('', Validators.required)
        });
    }
}
