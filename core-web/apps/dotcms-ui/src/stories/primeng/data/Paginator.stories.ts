import { Meta, moduleMetadata, StoryObj, argsToTemplate } from '@storybook/angular';

import { Paginator, PaginatorModule } from 'primeng/paginator';

const meta: Meta<Paginator> = {
    title: 'PrimeNG/Data/Paginator',
    decorators: [
        moduleMetadata({
            imports: [PaginatorModule]
        })
    ],
    parameters: {
        layout: 'centered',
        docs: {
            description: {
                component:
                    'Paginator is a generic component to display content in paged format.: https://primeng.org/paginator'
            }
        }
    },
    render: (args) => {
        return {
            props: { ...args },
            template: `<p-paginator ${argsToTemplate(args)} />`
        };
    }
};
export default meta;

type Story = StoryObj<Paginator>;

export const Primary: Story = {
    args: {
        rows: 10,
        totalRecords: 100,
        pageLinkSize: 3
    }
};
