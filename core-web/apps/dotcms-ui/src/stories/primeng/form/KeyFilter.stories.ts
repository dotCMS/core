import { Meta, moduleMetadata, Story } from '@storybook/angular';

import { FormsModule } from '@angular/forms';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { InputTextModule } from 'primeng/inputtext';
import { KeyFilterModule } from 'primeng/keyfilter';
import { MessageModule } from 'primeng/message';

export default {
    title: 'PrimeNG/Form/KeyFilter',
    parameters: {
        docs: {
            description: {
                component:
                    'KeyFilter directive restricts user input based on a regular expression.: https://primeng.org/keyfilter'
            }
        }
    },
    decorators: [
        moduleMetadata({
            imports: [
                BrowserModule,
                BrowserAnimationsModule,
                KeyFilterModule,
                InputTextModule,
                MessageModule,
                FormsModule
            ]
        })
    ],
    args: {
        ccRegex: /^[^<>*!]+$/,
        inputValue: ''
    }
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

const Template: Story<{
    ccRegex: RegExp;
    inputValue: string;
}> = (props: { ccRegex: RegExp; inputValue: string }) => {
    const template = KeyFilterTemplate;

    return {
        props,
        template
    };
};

export const Basic: Story = Template.bind({});

Basic.parameters = {
    docs: {
        source: {
            code: KeyFilterTemplate
        }
    }
};
