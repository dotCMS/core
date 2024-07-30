/* eslint-disable no-console */
import {
    Meta,
    StoryObj,
    moduleMetadata,
    componentWrapperDecorator,
    argsToTemplate,
    applicationConfig
} from '@storybook/angular';

import { importProvidersFrom } from '@angular/core';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { MenuItem } from 'primeng/api';
import { ToastModule } from 'primeng/toast';

import { DotCollapseBreadcrumbComponent } from '@dotcms/ui';

type Args = DotCollapseBreadcrumbComponent & {
    model: MenuItem[];
    maxItems: number;
};

const meta: Meta<Args> = {
    title: 'DotCMS/Menu/DotCollapseBreadcrumb',
    component: DotCollapseBreadcrumbComponent,
    decorators: [
        applicationConfig({
            providers: [importProvidersFrom(BrowserAnimationsModule)]
        }),
        moduleMetadata({
            imports: [BrowserAnimationsModule, ToastModule]
        }),
        componentWrapperDecorator(
            (story) =>
                `<div class="card flex justify-content-center w-50rem h-10rem relative">${story}</div>`
        )
    ],
    parameters: {
        layout: 'centered',
        docs: {
            description: {
                component:
                    'Breadcrumb provides contextual information about page hierarchy.: https://primefaces.org/primeng/showcase/#/breadcrumb'
            }
        }
    },
    args: {
        maxItems: 4,
        model: [
            { label: 'Electronics', command: console.log },
            { label: 'Computer', command: console.log },
            { label: 'Accessories', command: console.log },
            { label: 'Keyboard', command: console.log },
            { label: 'Wireless', command: console.log }
        ]
    },
    argTypes: {
        model: {
            description: 'Menu items to display'
        },
        maxItems: {
            description: 'Max items to display',
            control: { type: 'number' }
        }
    },
    render: (args: Args) => {
        return {
            props: {
                ...args
            },
            template: `<dot-collapse-breadcrumb ${argsToTemplate(args)} />`
        };
    }
};

export default meta;

type Story = StoryObj<Args>;

export const Default: Story = {};
