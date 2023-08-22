import { moduleMetadata } from '@storybook/angular';
import { Meta, Story } from '@storybook/angular/types-6-0';

import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { InputMask, InputMaskModule } from 'primeng/inputmask';

export default {
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
            imports: [InputMaskModule, BrowserAnimationsModule]
        })
    ],
    args: {
        val: ''
    }
} as Meta;

const InputMaskTemplate = `<p-inputMask [(ngModel)]="val" mask="99-9999" placeholder="99-9999"></p-inputMask>`;

const Template: Story<InputMask> = (props: InputMask) => {
    const template = InputMaskTemplate;

    return {
        props,
        template
    };
};

export const Basic: Story = Template.bind({});

Basic.argTypes = {
    val: {
        name: 'val',
        description: 'Input text'
    }
};

Basic.parameters = {
    docs: {
        source: {
            code: InputMaskTemplate
        }
    }
};
