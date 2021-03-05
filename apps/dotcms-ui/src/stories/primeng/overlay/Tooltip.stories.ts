import { Story, Meta } from '@storybook/angular/types-6-0';
import { moduleMetadata } from '@storybook/angular';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { TooltipModule } from 'primeng/tooltip';
import { ButtonModule } from 'primeng/button';

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
    ],
    args: {}
} as Meta;

const TooltipTemplate = `<button pButton label="Submit" icon="pi pi-check" pTooltip="Edit" tooltipPosition="bottom"></button>`;

const Template: Story<any> = (props: any) => {
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
