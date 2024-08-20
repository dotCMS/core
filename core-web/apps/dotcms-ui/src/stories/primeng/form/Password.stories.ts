import { Meta, moduleMetadata, StoryObj } from '@storybook/angular';

import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { PasswordModule } from 'primeng/password';

const PasswordTemplate = `<input type="password" pPassword />`;

const meta: Meta = {
    title: 'PrimeNG/Form/Password',
    parameters: {
        docs: {
            description: {
                component:
                    'Password displays strength indicator for password fields: https://primeng.org/password'
            },
            source: {
                code: PasswordTemplate
            }
        }
    },
    decorators: [
        moduleMetadata({
            imports: [PasswordModule, BrowserAnimationsModule]
        })
    ],
    render: (args) => ({
        props: args,
        template: PasswordTemplate
    })
};
export default meta;

type Story = StoryObj;

export const Basic: Story = {};
