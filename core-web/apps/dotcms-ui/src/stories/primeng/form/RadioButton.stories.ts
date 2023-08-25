import { moduleMetadata } from '@storybook/angular';
import { Meta, Story } from '@storybook/angular/types-6-0';

import { FormsModule } from '@angular/forms';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { RadioButtonModule } from 'primeng/radiobutton';

export default {
    title: 'PrimeNG/Form/RadioButton',
    parameters: {
        docs: {
            description: {
                component:
                    'RadioButton is an extension to standard radio button element with theming: https://primeng.org/radiobutton'
            }
        }
    },
    decorators: [
        moduleMetadata({
            imports: [RadioButtonModule, BrowserAnimationsModule, FormsModule]
        })
    ],
    args: {
        selectedValue: ''
    }
} as Meta;

const RadioButtonTemplate = `
  <div class="field-checkbox">
    <p-radioButton name="city" value="Chicago" [(ngModel)]="city" inputId="city1"></p-radioButton>
    <label for="city1">Chicago</label>
  </div>
  <div class="field-checkbox">
    <p-radioButton name="city" value="Los Angeles" [(ngModel)]="city" inputId="city2"></p-radioButton>
    <label for="city2">Los Angeles</label>
  </div>
  <div class="field-checkbox">
    <p-radioButton name="city" value="New York" [(ngModel)]="city" inputId="city3"></p-radioButton>
    <label for="city3">New York</label>
  </div>
  <div class="field-checkbox">
    <p-radioButton name="city" value="San Francisco" [(ngModel)]="city" inputId="city4"></p-radioButton>
    <label for="city4">San Francisco</label>
  </div>
`;

const Template: Story<{ selectedValue: string }> = (props: { selectedValue: string }) => {
    const template = RadioButtonTemplate;

    return {
        props,
        template
    };
};

export const Basic: Story = Template.bind({});

Basic.parameters = {
    docs: {
        source: {
            code: RadioButtonTemplate
        }
    }
};
