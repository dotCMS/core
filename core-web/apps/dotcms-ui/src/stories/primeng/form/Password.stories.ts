import { moduleMetadata } from '@storybook/angular';
import { Meta, Story } from '@storybook/angular/types-6-0';

import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { PasswordModule } from 'primeng/password';

export default {
    title: 'PrimeNG/Form/Password',
    parameters: {
        docs: {
            description: {
                component:
                    'Password displays strength indicator for password fields: https://primeng.org/password'
            }
        }
    },
    decorators: [
        moduleMetadata({
            imports: [PasswordModule, BrowserAnimationsModule]
        })
    ]
} as Meta;

const PasswordTemplate = `<input type="password" pPassword />`;

const Template: Story<never> = (props: never) => {
    const template = PasswordTemplate;

    return {
        props,
        template
    };
};

export const Basic: Story = Template.bind({});

Basic.parameters = {
    docs: {
        source: {
            code: PasswordTemplate
        }
    }
};
