import { Story, Meta } from '@storybook/angular/types-6-0';
import { moduleMetadata } from '@storybook/angular';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { ListboxModule } from 'primeng/listbox';

export default {
    title: 'PrimeNG/Form/ListBox',
    parameters: {
        docs: {
            description: {
                component:
                    'Listbox is used to select one or more values from a list of items: https://primefaces.org/primeng/showcase/#/listbox'
            }
        }
    },
    decorators: [
        moduleMetadata({
            imports: [ListboxModule, BrowserAnimationsModule]
        })
    ],
    args: {
        cities: [
            { label: 'Select City', value: null },
            { label: 'New York', value: { id: 1, name: 'New York', code: 'NY' } },
            { label: 'Rome', value: { id: 2, name: 'Rome', code: 'RM' } },
            { label: 'London', value: { id: 3, name: 'London', code: 'LDN' } },
            { label: 'Istanbul', value: { id: 4, name: 'Istanbul', code: 'IST' } },
            { label: 'Paris', value: { id: 5, name: 'Paris', code: 'PRS' } }
        ],
        selectedCity: []
    }
} as Meta;

const ListBoxTemplate = `<p-listbox [options]="cities" [(ngModel)]="selectedCity"></p-listbox>`;

const Template: Story<any> = (props: any) => {
    const template = ListBoxTemplate;
    return {
        props,
        template
    };
};

export const Basic: Story = Template.bind({});

Basic.parameters = {
    docs: {
        source: {
            code: ListBoxTemplate
        }
    },
    iframeHeight: 800
};
