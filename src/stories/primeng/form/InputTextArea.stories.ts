import { Story, Meta } from '@storybook/angular/types-6-0';
import { BrowserModule } from '@angular/platform-browser';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { FormsModule } from '@angular/forms';
import { moduleMetadata } from '@storybook/angular';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

export default {
  title: 'PrimeNG/Form/InputTextArea',
  component: InputTextareaModule,
  parameters: {
    docs: {
      description: {
        component:
          'Textarea is a multi-line text input element.: https://primefaces.org/primeng/showcase/#/inputtextarea',
      },
    },
  },
  decorators: [
    moduleMetadata({
      imports: [
        BrowserModule,
        BrowserAnimationsModule,
        InputTextareaModule,
        FormsModule,
      ],
    }),
  ],
} as Meta;

const InputTextAreaTemplate = `
  <textarea pInputTextarea [rows]="5" [cols]="30" placeholder="Some placeholder"></textarea>
`;

const InputTextAreaTemplateAutoRezise = `
  <textarea [rows]="5" [cols]="30" pInputTextarea autoResize="autoResize"></textarea>
`;

const Template: Story<any> = (props: any) => {
  const template = InputTextAreaTemplate;
  return {
    props,
    template,
  };
};

export const Basic: Story = Template.bind({});
export const AutoRezise: Story = Template.bind({});

Basic.parameters = {
  docs: {
    source: {
      code: InputTextAreaTemplate,
    },
  },

};

AutoRezise.parameters = {
  docs: {
    source: {
      code: InputTextAreaTemplateAutoRezise,
    },
  },
};
