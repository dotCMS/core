import { Meta, moduleMetadata, StoryObj } from '@storybook/angular';

import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { MessageModule, Message } from 'primeng/message';

const MessageTemplate = `<p-message [(value)]="messages" />`;

const meta: Meta<Message> = {
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
            imports: [MessageModule, BrowserAnimationsModule]
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
