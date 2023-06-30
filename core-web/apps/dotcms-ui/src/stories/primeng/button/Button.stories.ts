import { moduleMetadata } from '@storybook/angular';
import { Meta, Story } from '@storybook/angular/types-6-0';

import { ButtonModule } from 'primeng/button';

import { createButtonTemplate } from '../../utils/button';

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

const BasicTemplate = createButtonTemplate();
const OutlinedTemplate = createButtonTemplate('p-button-outlined');
const TextTemplate = createButtonTemplate('p-button-text');

export const Basic: Story = () => {
    return {
        template: BasicTemplate
    };
};

Basic.parameters = {
    docs: {
        source: {
            code: BasicTemplate
        }
    }
};

export const Outlined: Story = () => {
    return {
        template: OutlinedTemplate
    };
};

Outlined.parameters = {
    docs: {
        source: {
            code: OutlinedTemplate
        }
    }
};

export const Text: Story = () => {
    return {
        template: TextTemplate
    };
};

Text.parameters = {
    docs: {
        source: {
            code: TextTemplate
        }
    }
};
