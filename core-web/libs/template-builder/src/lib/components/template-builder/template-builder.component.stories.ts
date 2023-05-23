import { moduleMetadata, Story, Meta } from '@storybook/angular';

import { DotTemplateBuilderStore } from './store/template-builder.store';
import { TemplateBuilderComponent } from './template-builder.component';

export default {
    title: 'TemplateBuilderComponent',
    component: TemplateBuilderComponent,
    decorators: [
        moduleMetadata({
            imports: [],
            providers: [DotTemplateBuilderStore]
        })
    ]
} as Meta<TemplateBuilderComponent>;

const Template: Story<TemplateBuilderComponent> = (args: TemplateBuilderComponent) => ({
    props: args
});

export const Primary = Template.bind({});

Primary.args = {};
