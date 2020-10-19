import { Meta } from '@storybook/angular/types-6-0';
import { moduleMetadata } from '@storybook/angular';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { ToastModule } from 'primeng/toast';
import { ButtonModule } from 'primeng/button';
import { MessageService } from 'primeng/api';
import { ToastComponent } from './Toast.component'

export default {
  title: 'PrimeNG/Messages/Toast',
  parameters: {
    docs: {
      description: {
        component:
          'Tooltip directive provides advisory information for a component.: https://primefaces.org/primeng/showcase/#/tooltip',
      },
    },
  },
  decorators: [
    moduleMetadata({
      imports: [ToastModule, ButtonModule, BrowserAnimationsModule],
      providers: [MessageService],
    }),
  ],
} as Meta;

// inbox screen default state
export const Default = () => ({
  component: ToastComponent,
});
