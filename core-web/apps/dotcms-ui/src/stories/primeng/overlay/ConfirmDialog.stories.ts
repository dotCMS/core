import { Meta, StoryObj, moduleMetadata, componentWrapperDecorator } from '@storybook/angular';

import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { ConfirmationService } from 'primeng/api';
import { MessagesModule } from 'primeng/messages';
import { ToastModule } from 'primeng/toast';

import { ConfirmDialogComponent } from './ConfirmDialog.component';

const meta: Meta<ConfirmDialogComponent> = {
    title: 'PrimeNG/Overlay/ConfirmDialog',
    component: ConfirmDialogComponent,
    parameters: {
        docs: {
            description: {
                component:
                    'ConfirmDialog is backed by a service utilizing Observables to display confirmation windows easily that can be shared by multiple actions on the same component: https://primefaces.org/primeng/showcase/#/confirmdialog'
            },
            iframeHeight: 300
        }
    },
    decorators: [
        moduleMetadata({
            imports: [MessagesModule, BrowserAnimationsModule, ToastModule],
            providers: [ConfirmationService],
            declarations: []
        }),
        componentWrapperDecorator((story) => `<div class="w-full h-20rem">${story}</div>`)
    ],
    render: (args) => ({
        props: args,
        template: '<dot-p-confirm-dialog />'
    })
};
export default meta;

type Story = StoryObj<ConfirmDialogComponent>;

export const Basic: Story = {};
