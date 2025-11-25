import { Meta, moduleMetadata, StoryObj } from '@storybook/angular';

import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { InputSwitch, InputSwitchModule } from 'primeng/inputswitch';

const InputSwitchTemplate = `<p-inputSwitch [(ngModel)]="data" />`;

type Args = InputSwitch & { data: boolean };

const meta: Meta<Args> = {
    title: 'PrimeNG/Form/InputSwitch',
    component: InputSwitch,
    parameters: {
        docs: {
            description: {
                component:
                    'InputSwitch is used to select a boolean value.: https://primeng.org/inputswitch'
            },
            source: {
                code: InputSwitchTemplate
            }
        }
    },
    decorators: [
        moduleMetadata({
            imports: [InputSwitchModule, BrowserAnimationsModule]
        })
    ],
    args: {
        data: false
    },
    argTypes: {
        data: {
            name: 'checked',
            description: 'Boolean'
        }
    },
    render: (args) => ({
        props: args,
        template: InputSwitchTemplate
    })
};
export default meta;

type Story = StoryObj<Args>;

export const Basic: Story = {};
