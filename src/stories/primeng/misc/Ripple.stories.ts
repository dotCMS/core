import { Meta } from '@storybook/angular/types-6-0';
import { moduleMetadata } from '@storybook/angular';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { RippleComponent } from './Ripple.component';
import { BrowserModule } from '@angular/platform-browser';

import { RippleModule } from 'primeng/ripple';
import { ButtonModule } from 'primeng/button';
export default {
  title: 'PrimeNG/Misc/Ripple',
  parameters: {
    docs: {
      description: {
        component:
          'Ripple directive adds ripple effect to the host element.: https://primefaces.org/primeng/showcase/#/ripple',
      },
    },
  },
  decorators: [
    moduleMetadata({
      imports: [
        RippleModule,
        ButtonModule,
        BrowserModule,
        BrowserAnimationsModule,
      ],
    }),
  ],
} as Meta;


export const Default = () => ({
  component: RippleComponent,
  props: {},
});
