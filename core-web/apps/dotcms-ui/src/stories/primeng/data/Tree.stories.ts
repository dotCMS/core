import {
    Meta,
    StoryObj,
    moduleMetadata,
    componentWrapperDecorator,
    argsToTemplate
} from '@storybook/angular';

import { FormsModule } from '@angular/forms';

import { Tree, TreeModule } from 'primeng/tree';

import { generateFakeTree } from '../../utils/tree-node-files';

const meta: Meta<Tree> = {
    title: 'PrimeNG/Data/Tree',
    component: Tree,
    decorators: [
        moduleMetadata({
            imports: [TreeModule, FormsModule]
        }),
        componentWrapperDecorator(
            (story) => `<div class="card flex justify-center w-[25rem] h-25rem">${story}</div>`
        )
    ],
    parameters: {
        layout: 'centered',
        docs: {
            description: {
                component: 'Tree is used to display hierarchical data: https://primeng.org/tree'
            }
        }
    },
    args: {
        value: [...generateFakeTree()]
    },
    render: (args) => ({
        props: args,
        template: `<p-tree ${argsToTemplate(args)} />`
    })
};
export default meta;

type Story = StoryObj<Tree>;

export const Default: Story = {};

export const Checkbox: Story = {
    args: {
        selectionMode: 'checkbox'
    }
};

export const VirtualScroll: Story = {
    args: {
        value: [...generateFakeTree(1000)],
        virtualScroll: true,
        virtualScrollItemSize: 30,
        virtualScrollOptions: {
            autoSize: true,
            style: {
                width: '200px',
                height: '300px'
            }
        }
    }
};
