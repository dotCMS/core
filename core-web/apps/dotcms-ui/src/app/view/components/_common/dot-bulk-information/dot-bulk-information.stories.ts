import { Meta, moduleMetadata, Story } from '@storybook/angular';

import { DialogService, DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotFormatDateService } from '@dotcms/app/api/services/dot-format-date-service';
import { DotMessageService } from '@dotcms/data-access';
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

export default {
    title: 'DotCMS/Misc/BulkDialog',
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
        })
    ],
    component: DotBulkInformationComponent,
    args: {}
} as Meta;

const container = (child) => {
    return `
    <div style="
      background: white;
      padding: 2rem;
      width: 500px; 
      margin: 0 auto; 
      height: 420px; 
      overflow: auto;
      border: 1px solid #eee;
      " 
    >
      ${child}
    </div>`;
};

export const Basic: Story = (props) => {
    return {
        template: `
          ${container(`<dot-bulk-information></dot-bulk-information>`)}
        `,
        props
    };
};
