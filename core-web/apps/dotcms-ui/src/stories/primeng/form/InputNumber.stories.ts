import { Meta, moduleMetadata, StoryObj } from '@storybook/angular';

import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { InputNumberModule } from 'primeng/inputnumber';

const InputNumberTemplate = `<p-inputNumber [(ngModel)]="val" mode="decimal" />`;

const meta: Meta<InputNumberModule> = {
    title: 'PrimeNG/Form/InputText/InputNumber',
    component: InputNumberModule,
    parameters: {
        docs: {
            description: {
                component:
                    'InputNumber is an input component to provide numerical input: https://primeng.org/inputnumber'
            },
            source: {
                code: InputNumberTemplate
            }
        }
    },
    decorators: [
        moduleMetadata({
            imports: [InputNumberModule, BrowserAnimationsModule]
        })
    ],
    args: {
        checked: false
    },
    render: (args) => ({
        props: args,
        template: InputNumberTemplate
    })
};
export default meta;

type Story = StoryObj<InputNumberModule>;

export const Basic: Story = {};
