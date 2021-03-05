import { Story, Meta } from '@storybook/angular/types-6-0';
import { InputNumberModule } from 'primeng/inputnumber';
import { moduleMetadata } from '@storybook/angular';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

export default {
    title: 'PrimeNG/Form/InputText/InputNumber',
    component: InputNumberModule,
    parameters: {
        docs: {
            description: {
                component:
                    'InputNumber is an input component to provide numerical input: https://primefaces.org/primeng/showcase/#/inputnumber'
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

const InputNumberTemplate = `
  <p-inputNumber [(ngModel)]="val" mode="decimal"></p-inputNumber>
`;

const Template: Story<any> = (props: any) => {
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
