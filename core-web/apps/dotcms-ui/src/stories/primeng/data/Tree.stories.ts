import {
    Meta,
    StoryObj,
    moduleMetadata,
    componentWrapperDecorator,
    argsToTemplate
} from '@storybook/angular';

import { FormsModule } from '@angular/forms';

import { Tree, TreeModule } from 'primeng/tree';

import { files } from '../../utils/tree-node-files';

const meta: Meta<Tree> = {
    title: 'PrimeNG/Data/Tree',
    decorators: [
        moduleMetadata({
            imports: [TreeModule, FormsModule]
        }),
        componentWrapperDecorator(
            (story) =>
                `<div class="card flex justify-content-center w-25rem h-25rem">${story}</div>`
        )
    ],
    component: Tree,
    parameters: {
        layout: 'centered',
        docs: {
            description: {
                component:
                    'Tree is used to display hierarchical data: https://www.primefaces.org/primeng-v15-lts/tree'
            }
        }
    },
    args: {
        value: [...files]
    },
    render: (args: Tree) => ({
        props: {
            ...args
        },
        template: `
        <p-tree ${argsToTemplate(args)} />`
    })
};
export default meta;

type Story = StoryObj<Tree>;

export const Default: Story = {};

export const VirtualScroll: Story = {
    args: {
        virtualScroll: true,
        virtualScrollItemSize: 30,
        virtualScrollOptions: {
            autoSize: true,
            style: {
                width: '200px',
                height: '200px'
            }
        }
    }
};
