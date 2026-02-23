import {
    Meta,
    moduleMetadata,
    StoryObj,
    argsToTemplate,
    componentWrapperDecorator
} from '@storybook/angular';

import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { ButtonModule } from 'primeng/button';
import { DatePicker, DatePickerModule } from 'primeng/datepicker';
import { ChevronLeftIcon } from 'primeng/icons/chevronleft';

const TODAY = new Date();
const DAYS_TO_DISABLE = 5;
const DISABLED_DAYS: Date[] = [...Array(DAYS_TO_DISABLE)].map(
    (_, index) => new Date(Date.now() + (index + 1) * 24 * 60 * 60 * 1000)
);

const meta: Meta<DatePicker> = {
    title: 'PrimeNG/Form/DatePicker',
    component: DatePicker,
    decorators: [
        moduleMetadata({
            imports: [BrowserAnimationsModule, ButtonModule, DatePickerModule, ChevronLeftIcon]
        }),
        componentWrapperDecorator((story) => `<div class="h-30rem">${story}</div>`)
    ],
    args: {
        disabled: false,
        readonlyInput: true,
        showIcon: false,
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
    },
    render: (args) => ({
        props: args,
        template: `<p-datePicker ${argsToTemplate(args)} />`
    })
};

export default meta;

type Story = StoryObj<DatePicker>;

export const Default: Story = {};

export const WithIcon: Story = {
    args: {
        showIcon: true
    }
};
