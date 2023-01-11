import { moduleMetadata } from '@storybook/angular';
import { Meta, Story } from '@storybook/angular/types-6-0';

import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { ButtonModule } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';

export default {
    title: 'PrimeNG/Overlay/Tooltip',
    parameters: {
        docs: {
            description: {
                component:
                    'Tooltip directive provides advisory information for a component.: https://primefaces.org/primeng/showcase/#/tooltip'
            }
        }
    },
    decorators: [
        moduleMetadata({
            imports: [TooltipModule, ButtonModule, BrowserAnimationsModule]
        })
    ]
} as Meta;

const TooltipTemplate = `<button pButton label="Submit" icon="pi pi-check" pTooltip="Edit" tooltipPosition="bottom"></button>`;

const Template: Story<never> = (props: never) => {
    const template = TooltipTemplate;

    return {
        props,
        template
    };
};

export const Basic: Story = Template.bind({});

Basic.parameters = {
    docs: {
        source: {
            code: TooltipTemplate
        }
    }
};
