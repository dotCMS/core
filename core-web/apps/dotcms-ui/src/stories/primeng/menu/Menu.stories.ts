import { action } from '@storybook/addon-actions';
import { Meta, StoryObj, moduleMetadata, componentWrapperDecorator } from '@storybook/angular';

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
                command: () => action('update')
            },
            {
                label: 'Delete',
                icon: 'pi pi-times',
                command: () => action('delete')
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
        }),
        componentWrapperDecorator((story) => `<div class="card w-50rem h-25rem">${story}</div>`)
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
        template: `<p-menu [model]="items" appendTo="body" />`
    })
};

export const Overlay: Story = {
    render: (args) => ({
        props: args,
        template: `
            <p-menu #menu [popup]="true" appendTo="body" [model]="items" />
            <button type="button" pButton icon="pi pi-list" label="Show" (click)="menu.toggle($event)"></button>`
    })
};

export const WithCustomLabels: Story = {
    args: {
        items: [
            {
                id: 'custom-label',
                label: `
                    <div> <p class="font-bold">My custom label</p></div>`,
                escape: false,
                target: '_self'
            },
            { separator: true },
            {
                id: 'my-account',
                label: 'my-account',
                icon: 'pi pi-user',
                visible: true,
                command: () => action('my-account')
            }
        ]
    },
    render: (args) => ({
        props: args,
        template: `
            <p-menu #menu [popup]="true" appendTo="body" [model]="items" />
            <button type="button" pButton icon="pi pi-list" label="Show" (click)="menu.toggle($event)"></button>`
    })
};
