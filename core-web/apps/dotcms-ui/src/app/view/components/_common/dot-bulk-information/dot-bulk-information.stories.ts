import {
    Meta,
    moduleMetadata,
    componentWrapperDecorator,
    StoryObj,
    argsToTemplate
} from '@storybook/angular';

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
            (story) => `<div class="w-30rem border-1 mx-auto p-2">${story}</div>`
        )
    ],
    render: (args) => ({
        props: args,
        template: `<dot-bulk-information ${argsToTemplate(args)} />`
    })
};
export default meta;

type Story = StoryObj<DotBulkInformationComponent>;

export const Default: Story = {};
