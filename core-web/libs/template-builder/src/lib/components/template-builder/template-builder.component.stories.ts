import { moduleMetadata, Story, Meta } from '@storybook/angular';

import { TemplateBuilderComponent } from './template-builder.component';

export default {
    title: 'TemplateBuilderComponent',
    component: TemplateBuilderComponent,
    decorators: [
        moduleMetadata({
            imports: []
        })
    ]
} as Meta<TemplateBuilderComponent>;

const Template: Story<TemplateBuilderComponent> = (args: TemplateBuilderComponent) => ({
    props: args
});

export const Primary = Template.bind({});

Primary.args = {};
