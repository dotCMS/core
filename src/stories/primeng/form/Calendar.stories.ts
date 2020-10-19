// also exported from '@storybook/angular' if you can deal with breaking changes in 6.1
import { Story, Meta } from '@storybook/angular/types-6-0';
import { CalendarModule, Calendar } from 'primeng/calendar';
import { moduleMetadata } from '@storybook/angular';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

export default {
  title: 'PrimeNG/Form/Calendar',
  component: Calendar,
  decorators: [
    moduleMetadata({
      imports: [CalendarModule, BrowserAnimationsModule],
    }),
  ],
  parameters: {
    docs: {
      description: {
        component: 'Calendar is an input component to select a date: https://primefaces.org/primeng/showcase/#/calendar',
      },
    },
  }
} as Meta;

const CalendarTemplate = `<p-calendar [showTime]="true" inputId="time" showButtonBar="true"></p-calendar>`;
const Template: Story<Calendar> = (props: Calendar) => {
  const template = CalendarTemplate;
  return {
    props,
    template,
  };
};

export const Primary: Story = Template.bind({});
Primary.parameters = {
  docs: {
    source: {
      code: CalendarTemplate,
    },
    iframeHeight: 700,
  },
};
