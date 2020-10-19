import { Story, Meta } from '@storybook/angular/types-6-0';
import { moduleMetadata } from '@storybook/angular';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { MessagesModule } from 'primeng/messages';
import { MessageModule } from 'primeng/message';

export default {
  title: 'PrimeNG/Messages/Message',
  parameters: {
    docs: {
      description: {
        component:
          'Messages is used to display alerts inline.: https://primefaces.org/primeng/showcase/#/messages',
      },
    },
  },
  decorators: [
    moduleMetadata({
      imports: [MessagesModule, MessageModule, BrowserAnimationsModule],
    }),
  ],
  args: {
    messages: [
      { severity: 'success', summary: 'Success', detail: 'Message Content' },
      { severity: 'info', summary: 'Info', detail: 'Message Content' },
      { severity: 'warn', summary: 'Warning', detail: 'Message Content' },
      { severity: 'error', summary: 'Error', detail: 'Message Content' },
    ],
  },
} as Meta;

const MessageTemplate = `<p-messages [(value)]="messages"></p-messages>`;

const Template: Story<any> = () => {
  const template = MessageTemplate;
  return {
    props: {
      messages: [
        { severity: 'success', summary: 'Success', detail: 'Message Content' },
        { severity: 'info', summary: 'Info', detail: 'Message Content' },
        { severity: 'warn', summary: 'Warning', detail: 'Message Content' },
        { severity: 'error', summary: 'Error', detail: 'Message Content' },
      ],
    },
    template,
  };
};

export const Basic: Story = Template.bind({});

Basic.parameters = {
  docs: {
    source: {
      code: MessageTemplate,
    },
  },
};
