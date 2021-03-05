import { Story, Meta } from '@storybook/angular/types-6-0';
import { moduleMetadata } from '@storybook/angular';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { PasswordModule } from 'primeng/password';

export default {
    title: 'PrimeNG/Form/Password',
    parameters: {
        docs: {
            description: {
                component:
                    'Password displays strength indicator for password fields: https://primefaces.org/primeng/showcase/#/password'
            }
        }
    },
    decorators: [
        moduleMetadata({
            imports: [PasswordModule, BrowserAnimationsModule]
        })
    ],
    args: {}
} as Meta;

const PasswordTemplate = `<input type="password" pPassword />`;

const Template: Story<any> = (props: any) => {
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
