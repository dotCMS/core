import { Component, EventEmitter, OnInit, Output } from '@angular/core';
import { FormBuilder, FormGroup } from '@angular/forms';

@Component({
    selector: 'dot-bubble-form',
    templateUrl: './bubble-form.component.html',
    styleUrls: ['./bubble-form.component.scss']
})
export class BubbleFormComponent implements OnInit {
    @Output() formValues = new EventEmitter<unknown>();
    form: FormGroup;

    constructor(private fb: FormBuilder) {}

    ngOnInit(): void {
        this.form = this.fb.group({
            alt: null,
            title: null,
            src: null
        });
    }

    onSubmit() {
        this.formValues.emit({ ...this.form.value });
    }

    setFormData(values) {
        this.form.setValue(values);
    }
}
