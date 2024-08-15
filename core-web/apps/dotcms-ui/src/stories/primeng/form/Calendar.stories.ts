import { Meta, moduleMetadata, StoryObj } from '@storybook/angular';

import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { ButtonModule } from 'primeng/button';
import { Calendar } from 'primeng/calendar';

const TODAY = new Date();
const DAYS_TO_DISABLE = 5;
const DISABLED_DAYS: Date[] = [...Array(DAYS_TO_DISABLE)].map(
    (_, index) => new Date(Date.now() + (index + 1) * 24 * 60 * 60 * 1000)
);

const meta: Meta<Calendar> = {
    title: 'PrimeNG/Form/Calendar',
    component: Calendar,
    decorators: [
        moduleMetadata({
            imports: [BrowserAnimationsModule, ButtonModule]
        })
    ],
    args: {
        disabled: false,
        readonlyInput: true,
        showIcon: true,
        showTime: true,
        showClear: true,
        inline: false,
        placeholder: 'Select a date',
        minDate: TODAY,
        disabledDates: DISABLED_DAYS,
        selectionMode: 'single'
    },
    argTypes: {
        disabled: {
            control: 'boolean',
            description: 'When specified, disables the component.'
        },
        inline: {
            control: 'boolean',
            description:
                'When enabled, displays the calendar as inline. Default is false for popup mode.'
        },
        showIcon: {
            control: 'boolean',
            description: 'When enabled, displays a button with icon next to input.'
        },
        showClear: {
            control: 'boolean',
            description: 'When enabled, a clear icon is displayed to clear the value.'
        },
        readonlyInput: {
            control: 'boolean',
            description: 'When specified, prevents entering the date manually with keyboard.'
        },
        showTime: {
            control: 'boolean',
            description: 'Whether to display timepicker.'
        },

        placeholder: {
            control: 'text',
            description: 'Placeholder text for the input.'
        },
        disabledDates: {
            control: 'object',
            description: 'Array with dates that should be disabled (not selectable). Date[]'
        },
        selectionMode: {
            options: ['single', 'multiple', 'range'],
            control: { type: 'radio' },
            description:
                'Defines the quantity of the selection, valid values are "single", "multiple" and "range".'
        }
    }
};

export default meta;

type Story = StoryObj<Calendar>;

export const Primary: Story = {};
