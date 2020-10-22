// also exported from '@storybook/angular' if you can deal with breaking changes in 6.1
import { Meta } from '@storybook/angular/types-6-0';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ButtonModule } from 'primeng/button';
import { MessagesModule } from 'primeng/messages';
import { ToastModule } from 'primeng/toast';
import { ConfirmationService } from 'primeng/api';
import { ConfirmDialogComponent, ConfirmDialogTemplate } from './ConfirmDialog.component';
import { ToastComponent } from '../messages/Toast.component';

export default {
    title: 'PrimeNG/Overlay/ConfirmDialog',
    component: ConfirmDialogComponent,
    parameters: {
        docs: {
            description: {
                component:
                    'ConfirmDialog is backed by a service utilizing Observables to display confirmation windows easily that can be shared by multiple actions on the same component: https://primefaces.org/primeng/showcase/#/confirmdialog'
            }
        }
    }
} as Meta;

export const Basic = () => ({
    component: ConfirmDialogComponent,
    moduleMetadata: {
        imports: [
            ConfirmDialogModule,
            ButtonModule,
            MessagesModule,
            BrowserAnimationsModule,
            ToastModule
        ],
        providers: [ConfirmationService],
        declarations: [ToastComponent],
        entryComponents: [ToastComponent]
    }
});

Basic.parameters = {
    docs: {
        source: {
            code: ConfirmDialogTemplate
        },
        iframeHeight: 300
    }
};

Basic.args = {
    confirm: () => {}
};
