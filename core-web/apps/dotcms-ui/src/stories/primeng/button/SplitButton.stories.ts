// also exported from '@storybook/angular' if you can deal with breaking changes in 6.1
import { Meta, moduleMetadata, Story } from '@storybook/angular';

import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { SplitButtonModule } from 'primeng/splitbutton';

import { BasicSplitButtonTemplate, OutlinedSplitButtonTemplate } from './templates';

export default {
    title: 'PrimeNG/Button/SplitButton',
    decorators: [
        moduleMetadata({
            imports: [SplitButtonModule, BrowserAnimationsModule]
        })
    ],
    parameters: {
        layout: 'centered',
        docs: {
            description: {
                component:
                    'SplitButton groups a set of commands in an overlay with a default command: https://primeng.org/splitbutton'
            }
        }
    },
    props: {}
} as Meta;

const items = [
    {
        label: 'Update',
        icon: 'pi pi-refresh',
        command: () => {
            //
        }
    },
    {
        label: 'Delete',
        icon: 'pi pi-times',
        command: () => {
            //
        }
    },
    {
        label: 'Angular.io',
        icon: 'pi pi-info',
        command: () => {
            //
        }
    },
    { separator: true },
    {
        label: 'Setup',
        icon: 'pi pi-cog',
        command: () => {
            //
        }
    }
];

export const Basic: Story = () => {
    return {
        template: BasicSplitButtonTemplate,
        props: {
            items
        }
    };
};

Basic.parameters = {
    docs: {
        source: {
            code: BasicSplitButtonTemplate
        }
    }
};
export const Outlined: Story = () => {
    return {
        template: OutlinedSplitButtonTemplate,
        props: {
            items
        }
    };
};

Outlined.parameters = {
    docs: {
        source: {
            code: OutlinedSplitButtonTemplate
        }
    }
};
