import { Meta, moduleMetadata, StoryObj } from '@storybook/angular';

import { FormsModule } from '@angular/forms';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { TextareaModule } from 'primeng/textarea';

const meta: Meta = {
    title: 'PrimeNG/Form/InputTextArea',
    component: TextareaModule,
    parameters: {
        docs: {
            description: {
                component:
                    'Textarea is a multi-line text input element.: https://primeng.org/inputtextarea'
            }
        }
    },
    decorators: [
        moduleMetadata({
            imports: [BrowserModule, BrowserAnimationsModule, TextareaModule, FormsModule]
        })
    ]
};
export default meta;

type Story = StoryObj;

const InputTextAreaTemplate = `
<div class="flex flex-column gap-3">
    <div class="flex flex-column gap-2">
        <label htmlFor="test">Label</label>
        <textarea pInputTextarea [rows]="5" [cols]="30" placeholder="Some placeholder"></textarea>
        <small id="test-help">You can resize this text area</small>
    </div>
    <div class="flex flex-column gap-2">
        <label htmlFor="test-error">Label</label>
        <textarea pInputTextarea [rows]="5" [cols]="30" placeholder="Some placeholder" class="ng-invalid ng-dirty"></textarea>
        <small id="test-help-error">Please enter a valid text</small>
    </div>
    <div class="flex flex-column gap-2">
        <label htmlFor="test-disabled">Disabled</label>
        <textarea pInputTextarea [rows]="5" [cols]="30" placeholder="Disabled" disabled></textarea>
    </div>
</div>
`;

export const Basic: Story = {
    parameters: {
        source: {
            code: InputTextAreaTemplate
        }
    },
    render: (args) => ({
        props: args,
        template: InputTextAreaTemplate
    })
};

const InputTextAreaTemplateAutoRezise = `
<div class="flex flex-column gap-3">
    <div class="flex flex-column gap-2">
        <label htmlFor="test">Label</label>
        <textarea
            pInputTextarea
            [rows]="5"
            [cols]="30"
            placeholder="Some placeholder"
            [autoResize]="true"
        ></textarea>
        <small id="test-help">You can resize this text area</small>
    </div>
    <div class="flex flex-column gap-2">
        <label htmlFor="test-error">Label</label>
        <textarea
            pInputTextarea
            [rows]="5"
            [cols]="30"
            placeholder="Some placeholder"
            [autoResize]="true"
            class="ng-invalid ng-dirty"
        ></textarea>
        <small id="test-help-error">Please enter a valid text</small>
    </div>
    <div class="flex flex-column gap-2">
        <label htmlFor="test-disabled">Disabled</label>
        <textarea
            pInputTextarea
            [rows]="5"
            [cols]="30"
            placeholder="Disabled"
            [autoResize]="true"
            disabled
        ></textarea>
    </div>
</div>
`;

export const AutoRezise: Story = {
    parameters: {
        source: {
            code: InputTextAreaTemplateAutoRezise
        }
    },
    render: (args) => ({
        props: args,
        template: InputTextAreaTemplateAutoRezise
    })
};
