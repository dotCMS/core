import { moduleMetadata, Story, Meta } from '@storybook/angular';

import { NgIf, NgStyle } from '@angular/common';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { ButtonModule } from 'primeng/button';

import { DotMessageService } from '@dotcms/data-access';
import { DotIconModule } from '@dotcms/ui';

import { TemplateBuilderRowComponent } from './template-builder-row.component';

import { DOT_MESSAGE_SERVICE_TB_MOCK } from '../../utils/mocks';
import { RemoveConfirmDialogComponent } from '../remove-confirm-dialog/remove-confirm-dialog.component';
import { TemplateBuilderBackgroundColumnsComponent } from '../template-builder-background-columns/template-builder-background-columns.component';

export default {
    title: 'Library/Template Builder/Components/Row',
    component: TemplateBuilderRowComponent,
    decorators: [
        moduleMetadata({
            imports: [
                NgStyle,
                NgIf,
                DotIconModule,
                ButtonModule,
                RemoveConfirmDialogComponent,
                BrowserAnimationsModule,
                TemplateBuilderBackgroundColumnsComponent
            ],
            providers: [
                {
                    provide: DotMessageService,
                    useValue: DOT_MESSAGE_SERVICE_TB_MOCK
                }
            ]
        })
    ]
} as Meta<TemplateBuilderRowComponent>;

const Template: Story<TemplateBuilderRowComponent> = (args: TemplateBuilderRowComponent) => ({
    props: args
});

export const Primary = Template.bind({});

Primary.args = {};
