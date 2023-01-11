import { moduleMetadata } from '@storybook/angular';
import { Meta, Story } from '@storybook/angular/types-6-0';

import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { InputText, InputTextModule } from 'primeng/inputtext';
import { PasswordModule } from 'primeng/password';

export default {
    title: 'PrimeNG/Form/InputText/Default',
    component: InputText,
    parameters: {
        docs: {
            description: {
                component:
                    'InputText renders a text field to enter data.: https://primefaces.org/primeng/showcase/#/inputtext'
            }
        }
    },
    decorators: [
        moduleMetadata({
            imports: [InputTextModule, BrowserAnimationsModule, PasswordModule]
        })
    ],
    args: {
        checked: false
    }
} as Meta;

const InputTextTemplate = `
  <div class="form-control">
    <p><input type="text" class="p-inputtext-sm" placeholder="Small" pInputText /> <br /></p>
    <p><input type="text" placeholder="Normal" pInputText /> <br /></p>
    <p><input type="text" class="p-inputtext-lg" placeholder="Large"  pInputText /></p>
    <p><input pPassword type="password" placeholder="password" /></p>
  </div>
`;

const Template: Story<InputText> = (props: InputText) => {
    const template = InputTextTemplate;

    return {
        props,
        template
    };
};

export const Basic: Story = Template.bind({});

Basic.parameters = {
    docs: {
        source: {
            code: InputTextTemplate
        }
    }
};
