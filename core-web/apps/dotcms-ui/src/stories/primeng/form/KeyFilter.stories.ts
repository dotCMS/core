import { Meta, moduleMetadata, StoryObj } from '@storybook/angular';

import { FormsModule } from '@angular/forms';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { InputTextModule } from 'primeng/inputtext';
import { KeyFilterModule } from 'primeng/keyfilter';
import { MessageModule } from 'primeng/message';

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

const meta: Meta = {
    title: 'PrimeNG/Form/KeyFilter',
    parameters: {
        docs: {
            description: {
                component:
                    'KeyFilter directive restricts user input based on a regular expression.: https://primeng.org/keyfilter'
            },
            source: {
                code: KeyFilterTemplate
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
    },
    render: (args) => ({
        props: args,
        template: KeyFilterTemplate
    })
};
export default meta;

type Story = StoryObj;

export const Basic: Story = {};
