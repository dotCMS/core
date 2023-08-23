// also exported from '@storybook/angular' if you can deal with breaking changes in 6.1
import { moduleMetadata } from '@storybook/angular';
import { Meta, Story } from '@storybook/angular/types-6-0';

import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { MultiSelect, MultiSelectModule } from 'primeng/multiselect';

export default {
    title: 'PrimeNG/Form/MultiSelect',
    component: MultiSelect,
    decorators: [
        moduleMetadata({
            imports: [MultiSelectModule, BrowserAnimationsModule]
        })
    ],
    parameters: {
        docs: {
            description: {
                component:
                    'MultiSelect is used to multiple values from a list of options: https://primeng.org/multiselect'
            }
        }
    },
    args: {
        cities: [
            { name: 'New York', code: 'NY' },
            { name: 'Rome', code: 'RM' },
            { name: 'London', code: 'LDN' },
            { name: 'Istanbul', code: 'IST' },
            { name: 'Paris', code: 'PRS' }
        ],
        selectedCities: [{ name: 'Paris', code: 'PRS' }]
    }
} as Meta;

const MultiSelectTemplate = `
<p-multiSelect [options]="cities" defaultLabel="Select a City" optionLabel="name"></p-multiSelect>`;

const Template: Story<MultiSelect> = (props: MultiSelect) => {
    const template = MultiSelectTemplate;

    return {
        props,
        template
    };
};

export const Primary: Story = Template.bind({});

Primary.parameters = {
    docs: {
        source: {
            code: MultiSelectTemplate
        },
        iframeHeight: 300
    }
};
