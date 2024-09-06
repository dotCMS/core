import { Meta, moduleMetadata, StoryObj } from '@storybook/angular';

import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { InputTextModule } from 'primeng/inputtext';

const FloatLabelTemplate = `
  <span class="p-float-label">
    <input type="text" id="inputtext" pInputText [(ngModel)]="value1">
    <label for="inputtext">Enter Name</label>
  </span>
`;

const meta: Meta = {
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
    },
    render: (args) => ({
        props: args,
        template: FloatLabelTemplate
    })
};
export default meta;

type Story = StoryObj;

export const Basic: Story = {
    parameters: {
        docs: {
            source: {
                code: FloatLabelTemplate
            }
        }
    }
};
