import {
    Meta,
    StoryObj,
    moduleMetadata,
    componentWrapperDecorator,
    argsToTemplate
} from '@storybook/angular';

import { Breadcrumb, BreadcrumbModule } from 'primeng/breadcrumb';

type Args = Breadcrumb;

const meta: Meta<Args> = {
    title: 'PrimeNG/Menu/Breadcrumbs',
    component: Breadcrumb,
    decorators: [
        moduleMetadata({
            imports: [BreadcrumbModule]
        }),
        componentWrapperDecorator(
            (story) => `<div class="card flex justify-content-center">${story}</div>`
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
        model: [
            { label: 'Electronics', url: '/' },
            { label: 'Computer', url: '/' },
            { label: 'Accessories', url: '/' },
            { label: 'Keyboard', url: '/' },
            { label: 'Wireless', url: '/' }
        ],
        home: { icon: 'pi pi-home' }
    },
    argTypes: {
        model: { description: 'Defines the data' }
    },
    render: (args: Args) => {
        return {
            props: {
                ...args
            },
            template: `<p-breadcrumb class="max-w-full" ${argsToTemplate(args)} />`
        };
    }
};

export default meta;

type Story = StoryObj<Args>;

export const Default: Story = {};
