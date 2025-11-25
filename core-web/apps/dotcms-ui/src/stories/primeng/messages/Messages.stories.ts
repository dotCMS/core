import { Meta, moduleMetadata, StoryObj } from '@storybook/angular';

import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { MessagesModule, Messages } from 'primeng/messages';

const MessageTemplate = `<p-messages [(value)]="messages" />`;

const meta: Meta<Messages> = {
    title: 'PrimeNG/Messages/Message',
    parameters: {
        docs: {
            description: {
                component:
                    'Messages is used to display alerts inline.: https://primefaces.org/primeng/showcase/#/messages'
            },
            source: {
                code: MessageTemplate
            }
        }
    },
    decorators: [
        moduleMetadata({
            imports: [MessagesModule, BrowserAnimationsModule]
        })
    ],
    args: {
        messages: [
            { severity: 'success', summary: 'Success', detail: 'Message Content' },
            { severity: 'info', summary: 'Info', detail: 'Message Content' },
            { severity: 'warn', summary: 'Warning', detail: 'Message Content' },
            { severity: 'error', summary: 'Error', detail: 'Message Content' }
        ]
    },
    render: (args) => ({
        props: args,
        template: MessageTemplate
    })
};
export default meta;

type Story = StoryObj;

export const Basic: Story = {};
