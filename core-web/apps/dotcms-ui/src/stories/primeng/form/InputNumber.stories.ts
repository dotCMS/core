import { moduleMetadata } from '@storybook/angular';
import { Meta, Story } from '@storybook/angular/types-6-0';

import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { InputNumberModule } from 'primeng/inputnumber';

export default {
    title: 'PrimeNG/Form/InputText/InputNumber',
    component: InputNumberModule,
    parameters: {
        docs: {
            description: {
                component:
                    'InputNumber is an input component to provide numerical input: https://primeng.org/inputnumber'
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
    }
} as Meta;

export const InputNumberTemplate = `
  <p-inputNumber [(ngModel)]="val" mode="decimal"></p-inputNumber>
`;

const Template: Story<{
    checked: boolean;
}> = (props: { checked: boolean }) => {
    const template = InputNumberTemplate;

    return {
        props,
        template
    };
};

export const Basic: Story = Template.bind({});

Basic.parameters = {
    docs: {
        source: {
            code: InputNumberTemplate
        }
    }
};
