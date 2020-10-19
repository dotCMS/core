import { Story, Meta } from '@storybook/angular/types-6-0';
import { moduleMetadata } from '@storybook/angular';
import { ButtonModule } from 'primeng/button';

export default {
  title: 'PrimeNG/Misc/Badge',
  parameters: {
    docs: {
      description: {
        component:
          'Badge is a small status indicator for another element.: https://primefaces.org/primeng/showcase/#/badge',
      },
    },
  },
  decorators: [
    moduleMetadata({
      imports: [ButtonModule],
    }),
  ],
} as Meta;

const TooltipPrimaryTemplate = `
  <p><span class="p-badge">2</span></p>
  <p><span class="p-badge p-badge-lg">4</span></p>
  <p><span class="p-badge p-badge-xl">6</span></p>
`;

const TooltipSecondaryTemplate = `
  <p><span class="p-badge p-badge-secondary p-badge-secondary">2</span></p>
  <p><span class="p-badge p-badge-secondary p-badge-lg p-badge-sucess">4</span></p>
  <p><span class="p-badge p-badge-secondary p-badge-xl">6</span></p>
`;


export const Primary: Story = (props: any) => {
  return {
      props,
      template: TooltipPrimaryTemplate
  }
}

Primary.parameters = {
  docs: {
    source: {
      code: TooltipPrimaryTemplate,
    },
  },
};

export const Secondary: Story = (props: any) => {
  return {
      props,
      template: TooltipSecondaryTemplate
  }
}

Secondary.parameters = {
  docs: {
    source: {
      code: TooltipSecondaryTemplate,
    },
  },
};
