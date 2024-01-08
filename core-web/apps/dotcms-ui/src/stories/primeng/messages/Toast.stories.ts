import { Meta, moduleMetadata } from '@storybook/angular';

import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ToastModule } from 'primeng/toast';

import { ToastComponent } from './Toast.component';

export default {
    title: 'PrimeNG/Messages/Toast',
    parameters: {
        docs: {
            description: {
                component:
                    'Tooltip directive provides advisory information for a component.: https://primefaces.org/primeng/showcase/#/tooltip'
            }
        }
    },
    decorators: [
        moduleMetadata({
            imports: [ToastModule, ButtonModule, BrowserAnimationsModule],
            providers: [MessageService]
        })
    ]
} as Meta;

// inbox screen default state
export const Default = () => ({
    component: ToastComponent
});
