import { moduleMetadata, Story, Meta } from '@storybook/angular';

import { BinaryCodeFieldComponent } from './binary-code-field.component';

export default {
    title: 'Dotcms fields/components/Binary Code Field Component',
    component: BinaryCodeFieldComponent,
    decorators: [
        moduleMetadata({
            imports: []
        })
    ]
} as Meta<BinaryCodeFieldComponent>;

const Template: Story<BinaryCodeFieldComponent> = (args: BinaryCodeFieldComponent) => ({
    props: args
});

export const Primary = Template.bind({});

Primary.args = {};
