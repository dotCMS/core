import { Meta, moduleMetadata, StoryObj } from '@storybook/angular';

import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { ToggleSwitch, ToggleSwitchModule } from 'primeng/toggleswitch';

const ToggleSwitchTemplate = `<p-toggleSwitch [(ngModel)]="data" />`;

type Args = ToggleSwitch & { data: boolean };

const meta: Meta<Args> = {
    title: 'PrimeNG/Form/ToggleSwitch',
    component: ToggleSwitch,
    parameters: {
        docs: {
            description: {
                component:
                    'ToggleSwitch is used to select a boolean value.: https://primeng.org/toggleswitch'
            },
            source: {
                code: ToggleSwitchTemplate
            }
        }
    },
    decorators: [
        moduleMetadata({
            imports: [ToggleSwitchModule, BrowserAnimationsModule]
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
        template: ToggleSwitchTemplate
    })
};
export default meta;

type Story = StoryObj<Args>;

export const Basic: Story = {};
