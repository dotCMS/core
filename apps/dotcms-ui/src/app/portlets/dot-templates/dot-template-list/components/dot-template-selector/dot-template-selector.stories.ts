import { DotIconModule } from '@dotcms/dot-icon';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { Meta, moduleMetadata } from '@storybook/angular';
import { MockDotMessageService } from '@tests/dot-message-service.mock';
import { ButtonModule } from 'primeng/button';
import { DynamicDialogRef } from 'primeng/dynamicdialog';
import { DotTemplateSelectorComponent } from './dot-template-selector.component';

const messageServiceMock = new MockDotMessageService({
    'templates.template.selector.label.designer': 'Designer',
    'templates.template.selector.label.advanced': 'Advanced',
    'templates.template.selector.design':
        '<b>Template Designer</b> allows you to create templates seamlessly with a set of tools lorem ipsum.',
    'templates.template.selector.advanced':
        '<b>Template Advanced</b> allows you to create templates using HTML code'
});

export default {
    title: 'DotCMS/Template Selector',
    component: DotTemplateSelectorComponent,
    decorators: [
        moduleMetadata({
            imports: [DotIconModule, ButtonModule, DotMessagePipeModule],
            providers: [
                {
                    provide: DotMessageService,
                    useValue: messageServiceMock
                },
                {
                    provide: DynamicDialogRef,
                    useValue: {
                        close: () => {}
                    }
                }
            ]
        })
    ],
    parameters: {
        docs: {
            description: {
                component:
                    'Allows you to select the type of template to create from the template portlet'
            },
            iframeHeight: 400
        },
        layout: 'centered'
    }
} as Meta;

export const Primary = () => ({
    component: DotTemplateSelectorComponent
});
