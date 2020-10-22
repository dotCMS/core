// also exported from '@storybook/angular' if you can deal with breaking changes in 6.1
import { Meta } from '@storybook/angular/types-6-0';
import { MenuModule, Menu } from 'primeng/menu';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { ButtonModule } from 'primeng/button';

export default {
    title: 'PrimeNG/Menu/Menu',
    component: Menu,
    parameters: {
        docs: {
            description: {
                component:
                    'Menu is a navigation / command component that supports dynamic and static positioning: https://primefaces.org/primeng/showcase/#/menu'
            }
        }
    }
} as Meta;

const items = [
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

export const Basic = (_args: Menu) => {
    return {
        props: {
            items
        },
        moduleMetadata: {
            imports: [MenuModule, BrowserAnimationsModule]
        },
        template: `<p-menu [model]="items"></p-menu>`
    };
};

export const Overlay = (_args: Menu) => {
    return {
        props: {
            items
        },
        moduleMetadata: {
            imports: [MenuModule, BrowserAnimationsModule, ButtonModule]
        },
        template: `<p-menu #menu [popup]="true" [model]="items"></p-menu>
    <button type="button" pButton icon="pi pi-list" label="Show" (click)="menu.toggle($event)"></button>`
    };
};
