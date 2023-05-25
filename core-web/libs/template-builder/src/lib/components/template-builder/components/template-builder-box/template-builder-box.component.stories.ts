import { moduleMetadata, Story, Meta } from '@storybook/angular';

import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { ScrollPanelModule } from 'primeng/scrollpanel';

import { TemplateBuilderBoxComponent } from './template-builder-box.component';

export default {
    title: 'TemplateBuilderBoxComponent',
    component: TemplateBuilderBoxComponent,
    decorators: [
        moduleMetadata({
            imports: [ButtonModule, CardModule, ScrollPanelModule],
            providers: []
        })
    ]
} as Meta<TemplateBuilderBoxComponent>;

const Template: Story<TemplateBuilderBoxComponent> = (args: TemplateBuilderBoxComponent) => ({
    props: args
});

export const Primary = Template.bind({});

Primary.args = {
    size: 'large'
};
