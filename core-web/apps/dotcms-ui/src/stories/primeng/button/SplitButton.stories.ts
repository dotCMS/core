import { Meta, moduleMetadata, StoryObj } from '@storybook/angular';

import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { SplitButtonModule } from 'primeng/splitbutton';

import { BasicSplitButtonTemplate, OutlinedSplitButtonTemplate } from './templates';

const meta: Meta = {
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
    }
};
export default meta;

type Story = StoryObj;

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

export const Basic: Story = {
    parameters: {
        docs: {
            source: {
                code: BasicSplitButtonTemplate
            }
        }
    },
    render: () => {
        return {
            template: BasicSplitButtonTemplate,
            props: {
                items
            }
        };
    }
};

export const Outlined: Story = {
    parameters: {
        docs: {
            source: {
                code: OutlinedSplitButtonTemplate
            }
        }
    },
    render: () => {
        return {
            template: OutlinedSplitButtonTemplate,
            props: {
                items
            }
        };
    }
};
