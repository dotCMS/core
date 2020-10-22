import { Story, Meta } from '@storybook/angular/types-6-0';
import { ChipsModule, Chips } from 'primeng/chips';
import { moduleMetadata } from '@storybook/angular';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

export default {
    title: 'PrimeNG/Form/Chips',
    component: Chips,
    parameters: {
        docs: {
            description: {
                component:
                    'Chips is used to enter multiple values on an input field: https://primefaces.org/primeng/showcase/#/chips'
            }
        }
    },
    decorators: [
        moduleMetadata({
            imports: [ChipsModule, BrowserAnimationsModule]
        })
    ],
    args: {
        values: ['first']
    }
} as Meta;

const ChipsTemplate = `<p-chips [(ngModel)]="values" ></p-chips>`;

const Template: Story<Chips> = (props: Chips) => {
    const template = ChipsTemplate;
    return {
        props,
        template
    };
};

export const Basic: Story = Template.bind({});

Basic.argTypes = {
    values: {
        name: 'values',
        description: 'Array of strings, each representing a chip.'
    }
};

Basic.parameters = {
    docs: {
        source: {
            code: ChipsTemplate
        }
    }
};
