import { moduleMetadata, Story, Meta } from '@storybook/angular';

import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ScrollPanelModule } from 'primeng/scrollpanel';

import { TemplateBuilderBoxComponent } from './template-builder-box.component';

import { RemoveConfirmDialogComponent } from '../remove-confirm-dialog/remove-confirm-dialog.component';

export default {
    title: 'TemplateBuilderBoxComponent',
    component: TemplateBuilderBoxComponent,
    decorators: [
        moduleMetadata({
            imports: [
                ButtonModule,
                ScrollPanelModule,
                RemoveConfirmDialogComponent,
                BrowserAnimationsModule
            ],
            providers: [ConfirmationService]
        })
    ]
} as Meta<TemplateBuilderBoxComponent>;

const Template: Story<TemplateBuilderBoxComponent> = (args: TemplateBuilderBoxComponent) => ({
    props: args
});

const items = [
    { identifier: 'demo.dotcms.com' },
    { identifier: 'System Container' },
    { identifier: 'demo.dotcms.com' },
    { identifier: 'demo.dotcms.com' },
    { identifier: 'demo.dotcms.com' },
    { identifier: 'demo.dotcms.com' }
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
