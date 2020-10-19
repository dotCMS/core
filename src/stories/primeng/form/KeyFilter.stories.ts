import { Story, Meta } from '@storybook/angular/types-6-0';
import { moduleMetadata } from '@storybook/angular';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { KeyFilterModule } from 'primeng/keyfilter';
import { BrowserModule } from '@angular/platform-browser';
import { FormsModule } from '@angular/forms';

export default {
  title: 'PrimeNG/Form/KeyFilter',
  parameters: {
    docs: {
      description: {
        component:
          'KeyFilter directive restricts user input based on a regular expression.: https://primefaces.org/primeng/showcase/#/keyfilter',
      },
    },
  },
  decorators: [
    moduleMetadata({
      imports: [
        BrowserModule,
        BrowserAnimationsModule,
        KeyFilterModule,
        InputTextModule,
        MessageModule,
        FormsModule,
      ],
    }),
  ],
  args: {
    ccRegex: /^[^<>*!]+$/,
    inputValue: '',
  },
} as Meta;

const KeyFilterTemplate = `
  <form #form="ngForm">
    <label for="cc" style="display:block;margin-bottom:4px"
      >Credit Card</label
    >
    <input
      id="cc"
      type="text"
      name="cc"
      [(ngModel)]="inputValue"
      pInputText
      [pKeyFilter]="ccRegex"
      [pValidateOnly]="true"
      placeholder="1234-1234-1234-1234"
      style="margin-right: .5em"
    />
  </form>
`;

const Template: Story<any> = (props: any) => {
  const template = KeyFilterTemplate;
  return {
    props,
    template,
  };
};

export const Basic: Story = Template.bind({});

Basic.parameters = {
  docs: {
    source: {
      code: KeyFilterTemplate,
    },
  },
};
