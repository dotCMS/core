import { Meta, moduleMetadata, StoryObj } from '@storybook/angular';

import { ButtonModule } from 'primeng/button';

const meta: Meta = {
    title: 'PrimeNG/Misc/Badge',
    parameters: {
        docs: {
            description: {
                component:
                    'Badge is a small status indicator for another element.: https://primefaces.org/primeng/showcase/#/badge'
            }
        }
    },
    decorators: [
        moduleMetadata({
            imports: [ButtonModule]
        })
    ]
};
export default meta;

type Story = StoryObj;

const TooltipPrimaryTemplate = `
  <p><span class="p-badge">2</span></p>
  <p><span class="p-badge p-badge-lg">4</span></p>
  <p><span class="p-badge p-badge-xl">6</span></p>
`;

export const Primary: Story = {
    parameters: {
        docs: {
            source: {
                code: TooltipPrimaryTemplate
            }
        }
    },
    render: (args) => ({
        props: args,
        template: TooltipPrimaryTemplate
    })
};

const TooltipSecondaryTemplate = `
  <p><span class="p-badge p-badge-secondary p-badge-secondary">2</span></p>
  <p><span class="p-badge p-badge-secondary p-badge-lg p-badge-sucess">4</span></p>
  <p><span class="p-badge p-badge-secondary p-badge-xl">6</span></p>
`;

export const Secondary: Story = {
    parameters: {
        docs: {
            source: {
                code: TooltipSecondaryTemplate
            }
        }
    },
    render: (args) => ({
        props: args,
        template: TooltipSecondaryTemplate
    })
};
