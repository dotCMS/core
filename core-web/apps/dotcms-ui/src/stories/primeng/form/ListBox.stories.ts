import { Meta, moduleMetadata, StoryObj } from '@storybook/angular';

import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { ListboxModule, Listbox } from 'primeng/listbox';

type City = {
    label: string;
    value: {
        id: number;
        name: string;
        code: string;
    };
};

type Args = Listbox & { cities: City[]; selectedCity: City[] };

const ListBoxTemplate = `<p-listbox [options]="cities" [(ngModel)]="selectedCity" />`;

const meta: Meta<Args> = {
    title: 'PrimeNG/Form/ListBox',
    component: Listbox,
    parameters: {
        docs: {
            description: {
                component:
                    'Listbox is used to select one or more values from a list of items: https://primeng.org/listbox'
            },
            source: {
                code: ListBoxTemplate
            }
        },
        iframeHeight: 800
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
    },
    render: (args) => ({
        props: args,
        template: ListBoxTemplate
    })
};
export default meta;

type Story = StoryObj;

export const Basic: Story = {};
