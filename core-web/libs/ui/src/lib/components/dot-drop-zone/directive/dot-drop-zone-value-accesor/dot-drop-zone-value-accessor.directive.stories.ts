import { moduleMetadata, Story, Meta } from '@storybook/angular';

import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
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
                align-items: center;
                border: 1px dashed #ced4da;
                border-radius: 5px;
            }
        `
    ],
    template: `
        <form [formGroup]="myForm">
            <dot-drop-zone formControlName="file" dotDropZoneValueAccessor>
                <div class="dot-drop-zone__content" id="dot-drop-zone__content">Content</div>
            </dot-drop-zone>
        </form>
    `
})
class DotDropZoneValueAccessorTestComponent implements OnInit {
    myForm: FormGroup;

    constructor(private fb: FormBuilder) {}

    ngOnInit() {
        this.myForm = this.fb.group({
            file: ''
        });

        this.myForm.valueChanges.subscribe((value) => {
            // eslint-disable-next-line no-console
            console.log('value', value);
        });
    }
}

export default {
    title: 'Library/ui/Components/DropZone/ValueAccessor',
    component: DotDropZoneValueAccessorTestComponent,
    decorators: [
        moduleMetadata({
            imports: [FormsModule, ReactiveFormsModule, CommonModule, DotDropZoneComponent],
            declarations: [DotDropZoneValueAccessorDirective]
        })
    ]
} as Meta<DotDropZoneComponent>;

const Template: Story<DotDropZoneComponent> = (args: DotDropZoneComponent) => ({
    props: args,
    template: `
        <dot-drop-zone-value-accessor></dot-drop-zone-value-accessor>
    `
});

export const Base = Template.bind({});
