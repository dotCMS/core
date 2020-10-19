// also exported from '@storybook/angular' if you can deal with breaking changes in 6.1
import { Story, Meta } from '@storybook/angular/types-6-0';
import { CheckboxModule, Checkbox } from 'primeng/checkbox';
import { moduleMetadata } from '@storybook/angular';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

export default {
  title: 'PrimeNG/Form/Checkbox',
  parameters: {
    docs: {
      description: {
        component: 'Basic checkbox with label, more information: https://primefaces.org/primeng/showcase/#/checkbox',
      },
    },
  },
  component: Checkbox,
  decorators: [
    moduleMetadata({
      imports: [CheckboxModule, BrowserAnimationsModule],
    }),
  ],
  args: {},
} as Meta;

const CheckboxTemplate = `
<div class="p-field-checkbox">
  <p-checkbox name="group1" value="New York" inputId="ny"></p-checkbox>
  <label for="ny">New York</label>
</div>
<div class="p-field-checkbox">
  <p-checkbox name="group1" value="San Francisco" inputId="sf"></p-checkbox>
  <label for="sf">San Francisco</label>
</div>
<div class="p-field-checkbox">
  <p-checkbox name="group1" value="Los Angeles" inputId="la"></p-checkbox>
  <label for="la">Los Angeles</label>
</div>
<div class="p-field-checkbox">
  <p-checkbox name="group1" value="Chicago" inputId="ch"></p-checkbox>
  <label for="ch">Chicago</label>
</div>
`
const Template: Story<Checkbox> = (props: Checkbox) => {
  const template = CheckboxTemplate;
  return {
    props,
    template,
  };
};

export const Primary: Story = Template.bind({});
Primary.parameters = {
  docs: {
    source: {
      code: CheckboxTemplate,
    },
  },
};
