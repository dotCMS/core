import { Meta, moduleMetadata, StoryObj } from '@storybook/angular';

import { FormsModule } from '@angular/forms';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { InputMask, InputMaskModule } from 'primeng/inputmask';

const InputMaskTemplate = `<p-inputMask [(ngModel)]="val" mask="99-9999" placeholder="99-9999" />`;

type Args = InputMask & { val: string };

const meta: Meta<Args> = {
    title: 'PrimeNG/Form/InputText/InputMask',
    component: InputMask,
    parameters: {
        docs: {
            description: {
                component:
                    'InputMask component is used to enter input in a certain format such as numeric, date, currency, email and phone.: https://primeng.org/inputmask'
            }
        }
    },
    decorators: [
        moduleMetadata({
            imports: [InputMaskModule, BrowserAnimationsModule, FormsModule]
        })
    ],
    args: {
        val: ''
    },
    argTypes: {
        val: {
            name: 'val',
            description: 'Input text'
        }
    },
    render: (args) => ({
        props: args,
        template: InputMaskTemplate
    })
};
export default meta;

type Story = StoryObj<Args>;

export const Basic: Story = {
    parameters: {
        docs: {
            source: {
                code: InputMaskTemplate
            }
        }
    }
};
