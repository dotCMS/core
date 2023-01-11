import { moduleMetadata } from '@storybook/angular';
import { Meta, Story } from '@storybook/angular/types-6-0';

import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { MessageModule } from 'primeng/message';
import { MessagesModule } from 'primeng/messages';

export default {
    title: 'PrimeNG/Messages/Message',
    parameters: {
        docs: {
            description: {
                component:
                    'Messages is used to display alerts inline.: https://primefaces.org/primeng/showcase/#/messages'
            }
        }
    },
    decorators: [
        moduleMetadata({
            imports: [MessagesModule, MessageModule, BrowserAnimationsModule]
        })
    ],
    args: {
        messages: [
            { severity: 'success', summary: 'Success', detail: 'Message Content' },
            { severity: 'info', summary: 'Info', detail: 'Message Content' },
            { severity: 'warn', summary: 'Warning', detail: 'Message Content' },
            { severity: 'error', summary: 'Error', detail: 'Message Content' }
        ]
    }
} as Meta;

type Message = {
    severity: string;
    summary: string;
    detail: string;
};

const MessageTemplate = `<p-messages [(value)]="messages"></p-messages>`;

const Template: Story<{ messages: Message[] }> = () => {
    const template = MessageTemplate;

    return {
        props: {
            messages: [
                { severity: 'success', summary: 'Success', detail: 'Message Content' },
                { severity: 'info', summary: 'Info', detail: 'Message Content' },
                { severity: 'warn', summary: 'Warning', detail: 'Message Content' },
                { severity: 'error', summary: 'Error', detail: 'Message Content' }
            ]
        },
        template
    };
};

export const Basic: Story = Template.bind({});

Basic.parameters = {
    docs: {
        source: {
            code: MessageTemplate
        }
    }
};
