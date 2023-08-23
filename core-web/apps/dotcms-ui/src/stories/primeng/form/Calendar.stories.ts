// also exported from '@storybook/angular' if you can deal with breaking changes in 6.1
import { moduleMetadata } from '@storybook/angular';
import { Meta, Story } from '@storybook/angular/types-6-0';

import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { Calendar, CalendarModule } from 'primeng/calendar';

export default {
    title: 'PrimeNG/Form/Calendar',
    component: Calendar,
    decorators: [
        moduleMetadata({
            imports: [CalendarModule, BrowserAnimationsModule]
        })
    ],
    parameters: {
        docs: {
            description: {
                component:
                    'Calendar is an input component to select a date: https://primeng.org/calendar'
            }
        }
    }
} as Meta;

const CalendarTemplate = `<p-calendar [showTime]="true" inputId="time" showButtonBar="true"></p-calendar>`;
const Template: Story<Calendar> = (props: Calendar) => {
    const template = CalendarTemplate;

    return {
        props,
        template
    };
};

export const Primary: Story = Template.bind({});

Primary.parameters = {
    docs: {
        source: {
            code: CalendarTemplate
        },
        iframeHeight: 700
    }
};
