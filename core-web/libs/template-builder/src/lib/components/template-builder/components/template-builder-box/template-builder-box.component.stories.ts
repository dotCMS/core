import { moduleMetadata, Story, Meta } from '@storybook/angular';

import { ButtonModule } from 'primeng/button';
import { ScrollPanelModule } from 'primeng/scrollpanel';

import { TemplateBuilderBoxComponent } from './template-builder-box.component';

export default {
    title: 'TemplateBuilderBoxComponent',
    component: TemplateBuilderBoxComponent,
    decorators: [
        moduleMetadata({
            imports: [ButtonModule, ScrollPanelModule],
            providers: []
        })
    ]
} as Meta<TemplateBuilderBoxComponent>;

const Template: Story<TemplateBuilderBoxComponent> = (args: TemplateBuilderBoxComponent) => ({
    props: args
});

const items = [
    { label: 'demo.dotcms.com' },
    { label: 'System Container' },
    { label: 'demo.dotcms.com' },
    { label: 'demo.dotcms.com' },
    { label: 'demo.dotcms.com' },
    { label: 'demo.dotcms.com' }
];

export const Small = Template.bind({});

export const Medium = Template.bind({});

export const Large = Template.bind({});

Small.args = {
    size: 'small',
    items
};
Medium.args = {
    size: 'medium',
    items
};
Large.args = {
    size: 'large',
    items
};
