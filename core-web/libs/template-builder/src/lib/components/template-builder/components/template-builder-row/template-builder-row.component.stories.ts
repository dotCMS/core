import { moduleMetadata, Story, Meta } from '@storybook/angular';

import { CommonModule } from '@angular/common';

import { TemplateBuilderRowComponent } from './template-builder-row.component';

export default {
    title: 'TemplateBuilderRowComponent',
    component: TemplateBuilderRowComponent,
    decorators: [
        moduleMetadata({
            imports: [CommonModule],
            providers: []
        })
    ]
} as Meta<TemplateBuilderRowComponent>;

const Template: Story<TemplateBuilderRowComponent> = (args: TemplateBuilderRowComponent) => ({
    props: args
});

export const Primary = Template.bind({});

Primary.args = {};
