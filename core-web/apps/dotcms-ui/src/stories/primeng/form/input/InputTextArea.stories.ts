import { moduleMetadata } from '@storybook/angular';
import { Meta, Story } from '@storybook/angular/types-6-0';

import { FormsModule } from '@angular/forms';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { InputTextareaModule } from 'primeng/inputtextarea';

import { InputTextAreaTemplate, InputTextAreaTemplateAutoRezise } from './templates';

export default {
    title: 'PrimeNG/Form/InputTextArea',
    component: InputTextareaModule,
    parameters: {
        docs: {
            description: {
                component:
                    'Textarea is a multi-line text input element.: https://primefaces.org/primeng/showcase/#/inputtextarea'
            }
        }
    },
    decorators: [
        moduleMetadata({
            imports: [BrowserModule, BrowserAnimationsModule, InputTextareaModule, FormsModule]
        })
    ]
} as Meta;

const Template: Story<never> = (props: never) => {
    const template = InputTextAreaTemplate;

    return {
        props,
        template
    };
};

export const Basic: Story = Template.bind({});

export const AutoRezise: Story = Template.bind({});

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
