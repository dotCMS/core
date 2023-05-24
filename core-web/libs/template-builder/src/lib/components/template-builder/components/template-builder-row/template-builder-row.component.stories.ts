import { moduleMetadata, Story, Meta } from '@storybook/angular';

import { ButtonModule } from 'primeng/button';

import { DotIconModule } from '@dotcms/ui';

import { TemplateBuilderRowComponent } from './template-builder-row.component';

export default {
    title: 'TemplateBuilderRowComponent',
    component: TemplateBuilderRowComponent,
    decorators: [
        moduleMetadata({
            imports: [DotIconModule, ButtonModule],
            providers: []
        })
    ]
} as Meta<TemplateBuilderRowComponent>;

const Template: Story<TemplateBuilderRowComponent> = (args: TemplateBuilderRowComponent) => ({
    props: args
});

export const Primary = Template.bind({});

Primary.args = {};
