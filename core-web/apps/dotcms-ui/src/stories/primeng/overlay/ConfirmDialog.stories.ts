import { Meta, StoryObj, moduleMetadata } from '@storybook/angular';

import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { MessagesModule } from 'primeng/messages';
import { ToastModule } from 'primeng/toast';

import { ConfirmDialogComponent, ConfirmDialogTemplate } from './ConfirmDialog.component';

import { ToastComponent } from '../messages/Toast.component';

const meta: Meta<ConfirmDialogComponent> = {
    title: 'PrimeNG/Overlay/ConfirmDialog',
    component: ConfirmDialogComponent,
    parameters: {
        docs: {
            description: {
                component:
                    'ConfirmDialog is backed by a service utilizing Observables to display confirmation windows easily that can be shared by multiple actions on the same component: https://primefaces.org/primeng/showcase/#/confirmdialog'
            },
            source: {
                code: ConfirmDialogTemplate
            },
            iframeHeight: 300
        }
    },
    decorators: [
        moduleMetadata({
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
        })
    ],
    render: (args) => ({
        props: args,
        template: ConfirmDialogTemplate
    })
};
export default meta;

type Story = StoryObj<ConfirmDialogComponent>;

export const Basic: Story = {};
