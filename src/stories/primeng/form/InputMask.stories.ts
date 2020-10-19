import { Story, Meta } from '@storybook/angular/types-6-0';
import { InputMaskModule, InputMask} from 'primeng/inputmask';
import { moduleMetadata } from '@storybook/angular';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

export default {
  title: 'PrimeNG/Form/InputMask',
  component: InputMask,
  parameters: {
    docs: {
      description: {
        component:
          'InputMask component is used to enter input in a certain format such as numeric, date, currency, email and phone.: https://primefaces.org/primeng/showcase/#/inputmask',
      },
    },
  },
  decorators: [
    moduleMetadata({
      imports: [InputMaskModule, BrowserAnimationsModule],
    }),
  ],
  args: {
    val: '',
  },
} as Meta;

const InputMaskTemplate = `<p-inputMask [(ngModel)]="val" mask="99-9999" placeholder="99-9999"></p-inputMask>`;

const Template: Story<InputMask> = (props: InputMask) => {
  const template = InputMaskTemplate;
  return {
    props,
    template,
  };
};

export const Basic: Story = Template.bind({});

Basic.argTypes = {
  val: {
    name: 'val',
    description: 'Input text',
  }
}

Basic.parameters = {
  docs: {
    source: {
      code: InputMaskTemplate,
    },
  },
};
