import { action } from '@storybook/addon-actions';
import {
    Meta,
    StoryObj,
    moduleMetadata,
    componentWrapperDecorator,
    applicationConfig,
    argsToTemplate
} from '@storybook/angular';

import { importProvidersFrom } from '@angular/core';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { MenuItem } from 'primeng/api';
import { ToastModule } from 'primeng/toast';

import { DotCollapseBreadcrumbComponent } from '@dotcms/ui';

type Args = DotCollapseBreadcrumbComponent & { model: MenuItem[]; maxItems: number };

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
            (story) => `<div class="card flex justify-center w-50rem h-40 relative">${story}</div>`
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
    argTypes: {
        model: {
            description: 'Menu items to display'
        },
        maxItems: {
            description: 'Max items to display',
            control: { type: 'number' }
        }
    },
    render: (args) => ({
        props: args,
        template: `<dot-collapse-breadcrumb ${argsToTemplate(args)} />`
    })
};

export default meta;

type Story = StoryObj<Args>;

export const Default: Story = {
    args: {
        maxItems: 4,
        model: [
            { label: 'Electronics', command: action('command') },
            { label: 'Computer', command: action('command') },
            { label: 'Accessories', command: action('command') },
            { label: 'Keyboard', command: action('command') },
            { label: 'Wireless', command: action('command') }
        ]
    }
};
