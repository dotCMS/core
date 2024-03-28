// How to write stories? https://storybook.js.org/docs/6.5/angular/writing-stories/introduction
// Controls https://storybook.js.org/docs/6.5/angular/essentials/controls
// Annotations: https://storybook.js.org/docs/6.5/angular/essentials/controls#annotation

import { Meta, Story } from '@storybook/angular';

import { Button } from 'primeng/button';

export default {
    title: 'PrimeNG/Button',
    component: Button,
    args: {
        label: 'Button',
        disabled: false,
        icon: false,
        size: 'p-button-md',
        iconPos: 'left',
        severity: '-',
        type: '-'
    },
    argTypes: {
        size: {
            options: ['p-button-sm', 'p-button-md', 'p-button-lg'],
            control: { type: 'radio' }
        },
        severity: {
            options: ['-', 'p-button-secondary', 'p-button-danger'],
            control: { type: 'select' }
        },
        type: {
            options: ['-', 'p-button-text', 'p-button-outlined', 'p-button-link'],
            control: { type: 'select' }
        },
        iconPos: {
            control: 'inline-radio',
            options: ['left', 'right']
        }
    }
} as Meta;

export const Main: Story = (args) => {
    const parts = [];
    for (const key of Object.keys(args)) {
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
            icon: args.icon ? 'pi pi-home' : '',
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
};
