import { action } from '@storybook/addon-actions';
import { moduleMetadata, StoryObj, Meta } from '@storybook/angular';

import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnInit, Output, inject } from '@angular/core';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule } from '@angular/forms';

import { DotDropZoneValueAccessorDirective } from './dot-drop-zone-value-accessor.directive';

import { DotDropZoneComponent } from '../../dot-drop-zone.component';

/**
 * This component is used to test the value accessor directive on Storybook
 *
 * @class DotDropZoneValueAccessorTestComponent
 * @implements {OnInit}
 */
@Component({
    selector: 'dot-drop-zone-value-accessor',
    styles: [
        `
            .dot-drop-zone__content {
                width: 100%;
                height: 200px;
                background: #f8f9fa;
                display: flex;
                justify-content: center;
                flex-direction: column;
                gap: 1rem;
                align-items: center;
                border: 1px dashed #ced4da;
                border-radius: 5px;
            }
        `
    ],
    template: `
        <form [formGroup]="myForm">
            <dot-drop-zone
                [accept]="accept"
                [maxFileSize]="maxFileSize"
                formControlName="file"
                dotDropZoneValueAccessor>
                <div class="dot-drop-zone__content" id="dot-drop-zone__content">
                    Drop files here.
                    @if (accept.length) {
                        <div>
                            <strong>Allowed Type:</strong>
                            {{ accept }}
                        </div>
                    }
                    @if (maxFileSize) {
                        <div>
                            <strong>Max File Size:</strong>
                            {{ maxFileSize }}
                        </div>
                    }
                </div>
            </dot-drop-zone>
        </form>
    `
})
class DotDropZoneValueAccessorTestComponent implements OnInit {
    private fb = inject(FormBuilder);

    @Input() accept: string[];
    @Input() maxFileSize: number;

    @Output() formChanged = new EventEmitter();
    @Output() formErrors = new EventEmitter();

    myForm: FormGroup;

    ngOnInit() {
        this.myForm = this.fb.group({
            file: null
        });

        this.myForm.valueChanges.subscribe((value) => {
            // eslint-disable-next-line no-console
            this.formChanged.emit(value);

            if (this.myForm.invalid) {
                this.formErrors.emit(this.myForm.errors);
            }
        });
    }
}

const meta: Meta = {
    title: 'Library/ui/Components/DropZone/ValueAccessor',
    component: DotDropZoneValueAccessorTestComponent,
    decorators: [
        moduleMetadata({
            imports: [FormsModule, ReactiveFormsModule, CommonModule, DotDropZoneComponent],
            declarations: [DotDropZoneValueAccessorDirective]
        })
    ],
    parameters: {
        // https://storybook.js.org/docs/6.5/angular/essentials/actions#action-event-handlers
        actions: {
            // detect if the component is emitting the correct HTML events
            handles: ['formChanged', 'formErrors']
        }
    },
    args: {
        formChanged: action('formChanged'),
        formErrors: action('formErrors')
    },
    render: (args) => ({
        props: args,
        template: `
        <dot-drop-zone-value-accessor
            [accept]="accept"
            [maxFileSize]="maxFileSize"
            (formChanged)="formChanged($event)"
            (formErrors)="formErrors($event)"
        ></dot-drop-zone-value-accessor>
    `
    })
};
export default meta;

type Story = StoryObj;

export const Base: Story = {
    args: {
        accept: [],
        maxFileSize: 1000000
    }
};
