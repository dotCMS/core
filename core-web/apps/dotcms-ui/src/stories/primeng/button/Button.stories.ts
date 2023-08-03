import { moduleMetadata } from '@storybook/angular';
import { Meta, Story } from '@storybook/angular/types-6-0';

import { ButtonModule } from 'primeng/button';

import { MainTemplate, IconOnlyTemplate } from './templates';

export default {
    title: 'PrimeNG/Button/Button',
    decorators: [
        moduleMetadata({
            imports: [ButtonModule]
        })
    ],
    parameters: {
        layout: 'centered',
        docs: {
            description: {
                component: 'All the buttons, more information: https://primeng.org/button'
            }
        }
    }
} as Meta;

export const Main: Story = () => {
    return {
        template: MainTemplate
    };
};

export const IconOnly: Story = () => {
    return {
        template: IconOnlyTemplate
    };
};
