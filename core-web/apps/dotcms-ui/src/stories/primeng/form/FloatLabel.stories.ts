import { Meta, moduleMetadata, Story } from '@storybook/angular';

import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { InputTextModule } from 'primeng/inputtext';

export default {
    title: 'PrimeNG/Form/FloatLabel',
    parameters: {
        docs: {
            description: {
                component:
                    'All input text components support floating labels: https://primeng.org/inputtext#floatlabel'
            }
        }
    },
    decorators: [
        moduleMetadata({
            imports: [BrowserAnimationsModule, InputTextModule]
        })
    ],
    args: {
        checked: false
    }
} as Meta;

const FloatLabelTemplate = `
  <span class="p-float-label">
    <input type="text" id="inputtext" pInputText [(ngModel)]="value1">
    <label for="inputtext">Enter Name</label>
  </span>
`;

const Template: Story<{ checked: boolean }> = (props: { checked: boolean }) => {
    const template = FloatLabelTemplate;

    return {
        props,
        template
    };
};

export const Basic: Story = Template.bind({});

Basic.parameters = {
    docs: {
        source: {
            code: FloatLabelTemplate
        }
    }
};
