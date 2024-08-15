/* eslint-disable no-console */
import { Meta, StoryObj, moduleMetadata } from '@storybook/angular';

import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { MenuItem } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { Menu, MenuModule } from 'primeng/menu';

const items: MenuItem[] = [
    {
        label: 'Options',
        items: [
            {
                label: 'Update',
                icon: 'pi pi-refresh',
                command: () => {
                    console.log('update');
                }
            },
            {
                label: 'Delete',
                icon: 'pi pi-times',
                command: () => {
                    console.log('delete');
                }
            }
        ]
    }
];

type Args = Menu & { items: MenuItem[] };

const meta: Meta<Args> = {
    title: 'PrimeNG/Menu/Menu',
    component: Menu,
    parameters: {
        docs: {
            description: {
                component:
                    'Menu is a navigation / command component that supports dynamic and static positioning: https://primefaces.org/primeng/showcase/#/menu'
            }
        }
    },
    decorators: [
        moduleMetadata({
            imports: [MenuModule, BrowserAnimationsModule, ButtonModule]
        })
    ],
    args: {
        items: [...items]
    }
};
export default meta;

type Story = StoryObj;

export const Basic: Story = {
    render: (args) => ({
        props: args,
        template: `<p-menu #menu [model]="items" />`
    })
};

export const Overlay: Story = {
    render: (args) => ({
        props: args,
        template: `<p-menu #menu [popup]="true" [model]="items" />
    <button type="button" pButton icon="pi pi-list" label="Show" (click)="menu.toggle($event)"></button>`
    })
};
