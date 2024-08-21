import { Meta, StoryObj } from '@storybook/angular';

import { Button } from 'primeng/button';

type Args = Button & {
    size: string;
    severity: string;
    rounded: string;
};

const meta: Meta<Args> = {
    title: 'PrimeNG/Button',
    component: Button,
    args: {
        label: 'Button',
        disabled: false,
        size: 'p-button-md',
        iconPos: 'left',
        severity: '-',
        type: '-',
        rounded: '-',
        icon: 'pi pi-home'
    },
    argTypes: {
        size: {
            options: ['p-button-sm', 'p-button-md', 'p-button-lg'],
            control: { type: 'radio' }
        },
        severity: {
            options: ['-', 'p-button-secondary', 'p-button-tertiary', 'p-button-danger'],
            control: { type: 'radio' }
        },
        rounded: {
            options: ['-', 'p-button-rounded'],
            control: { type: 'radio' }
        },
        type: {
            options: ['-', 'p-button-text', 'p-button-outlined', 'p-button-link'],
            control: { type: 'radio' }
        },
        iconPos: {
            control: 'inline-radio',
            options: ['left', 'right']
        }
    }
};
export default meta;

type Story = StoryObj<Args>;

export const Main: Story = {
    render: (args) => {
        const argsWithClasses = ['size', 'severity', 'type', 'rounded'];
        const parts = [];

        for (const key of argsWithClasses) {
            if (
                typeof args[key] === 'string' &&
                args[key].trim() !== '-' &&
                args[key].trim().length > 0
            ) {
                parts.push(args[key].trim());
            }
        }

        const joined = parts.join(' ');

        return {
            props: {
                label: args.label,
                classes: joined,
                disabled: args.disabled,
                icon: args.icon ?? '',
                iconPos: args.iconPos
            },
            template: `
            <p-button
                [icon]="icon"
                [iconPos]="iconPos"
                [disabled]="disabled"
                [label]="label"
                [styleClass]="classes">
            </p-button>`
        };
    }
};
