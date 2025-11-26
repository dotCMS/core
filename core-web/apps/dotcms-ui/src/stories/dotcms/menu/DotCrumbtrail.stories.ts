import { Meta, StoryObj, moduleMetadata, componentWrapperDecorator } from '@storybook/angular';

import { DotCrumbtrailComponent } from '../../../app/view/components/dot-crumbtrail/dot-crumbtrail.component';

const meta: Meta<DotCrumbtrailComponent> = {
    title: 'DotCMS/Menu/DotCrumbtrail',
    component: DotCrumbtrailComponent,
    decorators: [
        moduleMetadata({
            imports: [DotCrumbtrailComponent]
        }),
        componentWrapperDecorator(
            (story) => `<div class="card flex justify-content-center">${story}</div>`
        )
    ],
    parameters: {
        docs: {
            description: {
                component:
                    'Breadcrumb provides contextual information about page hierarchy.: https://primefaces.org/primeng/showcase/#/breadcrumb'
            }
        }
    },
    render: (args) => ({
        props: args,
        template: `<dot-crumbtrail class="max-w-full" />`
    })
};

export default meta;

type Story = StoryObj<DotCrumbtrailComponent>;

export const Default: Story = {};
