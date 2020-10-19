import { Story, Meta } from '@storybook/angular/types-6-0';
import { BrowserModule } from '@angular/platform-browser';
import { FormsModule } from '@angular/forms';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { moduleMetadata } from '@storybook/angular';
import { ProgressBarModule } from 'primeng/progressbar';
import { ToastModule } from 'primeng/toast';

export default {
  title: 'PrimeNG/Misc/ProgressBar',
  parameters: {
    docs: {
      description: {
        component:
          'ProgressBar is a process status indicator.: https://primefaces.org/primeng/showcase/#/progressbar',
      },
    },
  },
  decorators: [
    moduleMetadata({
      imports: [
        BrowserModule,
        BrowserAnimationsModule,
        ProgressBarModule,
        ToastModule,
        FormsModule,
      ],
    }),
  ],
  args: {
    value: 10,
    showValue: false,
  },
} as Meta;

const IndeterminateTemplate = `
  <h3>Indeterminate</h3>
  <p-progressBar mode="indeterminate"></p-progressBar>
`;

const StaticTemplate = `
  <h3>Static</h3>
  <p-progressBar [value]="value" [showValue]="showValue"></p-progressBar>
`;

export const Indeterminate: Story = () => {
  return {
    template: IndeterminateTemplate,
  };
};

Indeterminate.parameters = {
  docs: {
    source: {
      code: IndeterminateTemplate,
    },
  },
};

export const Static: Story = () => {
  return {
    template: StaticTemplate,
    props: {
      value: 10,
      showValue: false
    },
  };
};

Static.parameters = {
  docs: {
    source: {
      code: StaticTemplate,
    },
  },
};