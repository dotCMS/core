import { Meta, moduleMetadata, componentWrapperDecorator, StoryObj } from '@storybook/angular';

import { DialogService, DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotMessageService, DotFormatDateService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotBulkInformationComponent } from './dot-bulk-information.component';

const messageServiceMock = new MockDotMessageService({
    'message.template.archived': 'archived',
    'message.template.failed': 'failed',
    'message.template.success': 'has been successfully',
    'message.template.singular': 'template',
    'message.template.plural': 'templates'
});

const meta: Meta<DotBulkInformationComponent> = {
    title: 'DotCMS/Misc/BulkDialog',
    component: DotBulkInformationComponent,
    decorators: [
        moduleMetadata({
            imports: [DotMessagePipe],
            providers: [
                DynamicDialogRef,
                DotFormatDateService,
                DialogService,
                { provide: DotMessageService, useValue: messageServiceMock },
                {
                    provide: DynamicDialogConfig,
                    useValue: {
                        data: {
                            fails: [
                                {
                                    description: 'Blank - 1',
                                    errorMessage:
                                        'Template cannot be published because it is archived'
                                },
                                {
                                    description: 'Blank - 2',
                                    errorMessage:
                                        'Template cannot be published because it is archived'
                                },
                                {
                                    description: 'Blank - 3',
                                    errorMessage:
                                        'Template cannot be published because it is archived'
                                },
                                {
                                    description: 'Blank - 4',
                                    errorMessage:
                                        'Template cannot be published because it is archived'
                                }
                            ],
                            successCount: 1,
                            action: messageServiceMock.get('archived')
                        }
                    }
                }
            ],
            declarations: [DotBulkInformationComponent]
        }),
        componentWrapperDecorator(
            (story) => `<div style="
      background: white;
      padding: 2rem;
      width: 500px; 
      margin: 0 auto; 
      height: 420px; 
      overflow: auto;
      border: 1px solid #eee;
      " 
    >
      ${story}
    </div>`
        )
    ],
    args: {}
};
export default meta;

type Story = StoryObj<DotBulkInformationComponent>;

export const Basic: Story = {
    args: {}
};
