import { Meta, moduleMetadata, Story } from '@storybook/angular';

import { FormsModule } from '@angular/forms';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { InputTextareaModule } from 'primeng/inputtextarea';

export default {
    title: 'PrimeNG/Form/InputTextArea',
    component: InputTextareaModule,
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
            imports: [BrowserModule, BrowserAnimationsModule, InputTextareaModule, FormsModule]
        })
    ]
} as Meta;

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

const MainTextArea: Story<unknown> = (props: never) => {
    const template = InputTextAreaTemplate;

    return {
        props,
        template
    };
};

const AutoResizeTextArea: Story<unknown> = (props: never) => {
    const template = InputTextAreaTemplateAutoRezise;

    return {
        props,
        template
    };
};

export const Basic: Story = MainTextArea.bind({});

export const AutoRezise: Story = AutoResizeTextArea.bind({});

Basic.parameters = {
    docs: {
        source: {
            code: InputTextAreaTemplate
        }
    }
};

AutoRezise.parameters = {
    docs: {
        source: {
            code: InputTextAreaTemplateAutoRezise
        }
    }
};
