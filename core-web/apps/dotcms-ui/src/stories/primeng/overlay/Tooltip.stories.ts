import { Meta, moduleMetadata, StoryObj } from '@storybook/angular';

import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { ButtonModule } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';

const TooltipTemplate = `<button pButton label="Submit" icon="pi pi-check" pTooltip="Edit" tooltipPosition="bottom"></button>`;

const meta: Meta = {
    title: 'PrimeNG/Overlay/Tooltip',
    parameters: {
        docs: {
            description: {
                component:
                    'Tooltip directive provides advisory information for a component.: https://primefaces.org/primeng/showcase/#/tooltip'
            },
            source: {
                code: TooltipTemplate
            }
        }
    },
    decorators: [
        moduleMetadata({
            imports: [TooltipModule, ButtonModule, BrowserAnimationsModule]
        })
    ],
    render: (args) => ({
        props: args,
        template: TooltipTemplate
    })
};
export default meta;

type Story = StoryObj;

export const Basic: Story = {};
