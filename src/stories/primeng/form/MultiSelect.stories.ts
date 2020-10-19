// also exported from '@storybook/angular' if you can deal with breaking changes in 6.1
import { Story, Meta } from '@storybook/angular/types-6-0';
import { MultiSelectModule, MultiSelect } from 'primeng/multiselect';
import { moduleMetadata } from '@storybook/angular';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

export default {
  title: 'PrimeNG/Form/MultiSelect',
  component: MultiSelect,
  decorators: [
    moduleMetadata({
      imports: [MultiSelectModule, BrowserAnimationsModule],
    }),
  ],
  parameters: {
    docs: {
      description: {
        component:
          'MultiSelect is used to multiple values from a list of options: https://primefaces.org/primeng/showcase/#/multiselect',
      },
    },
  },
  args: {
    cities: [
      { name: 'New York', code: 'NY' },
      { name: 'Rome', code: 'RM' },
      { name: 'London', code: 'LDN' },
      { name: 'Istanbul', code: 'IST' },
      { name: 'Paris', code: 'PRS' },
    ],
    selectedCities: [{ name: 'Paris', code: 'PRS' }],
  },
} as Meta;

const MultiSelectTemplate = `
<p-multiSelect [options]="cities" defaultLabel="Select a City" optionLabel="name"></p-multiSelect>`;
const Template: Story<MultiSelect> = (props: MultiSelect) => {
  const template = MultiSelectTemplate;
  return {
    props,
    template,
  };
};

export const Primary: Story = Template.bind({});
Primary.parameters = {
  docs: {
    source: {
      code: MultiSelectTemplate,
    },
    iframeHeight: 300,
  },
};
