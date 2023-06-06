import { moduleMetadata, Story, Meta } from '@storybook/angular';

import { ButtonModule } from 'primeng/button';

import { DotIconModule } from '@dotcms/ui';

import { TemplateBuilderRowComponent } from './template-builder-row.component';

import { RemoveConfirmDialogComponent } from '../remove-confirm-dialog/remove-confirm-dialog.component';

export default {
    title: 'TemplateBuilderRowComponent',
    component: TemplateBuilderRowComponent,
    decorators: [
        moduleMetadata({
            imports: [DotIconModule, ButtonModule, RemoveConfirmDialogComponent],
            providers: []
        })
    ]
} as Meta<TemplateBuilderRowComponent>;

const Template: Story<TemplateBuilderRowComponent> = (args: TemplateBuilderRowComponent) => ({
    props: args
});

export const Primary = Template.bind({});

Primary.args = {};
