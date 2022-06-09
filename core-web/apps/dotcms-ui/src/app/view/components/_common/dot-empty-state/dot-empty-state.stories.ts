import { Meta, Story } from '@storybook/angular/types-6-0';
import { moduleMetadata } from '@storybook/angular';
import { DotEmptyStateModule } from './dot-empty-state.module';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { MockDotMessageService } from '@tests/dot-message-service.mock';
import { DotPipesModule } from '@pipes/dot-pipes.module';

const messageServiceMock = new MockDotMessageService({
    'message.template.empty.title': 'Your template list is empty',
    'message.template.empty.content':
        "You haven't added anything yet, start by clicking the button below",
    'message.template.empty.button.label': 'Add New Template'
});

export default {
    title: 'DotCMS/Structure/Empty State',
    decorators: [
        moduleMetadata({
            imports: [DotEmptyStateModule, DotPipesModule],
            providers: [{ provide: DotMessageService, useValue: messageServiceMock }]
        })
    ],
    parameters: {
        docs: {}
    },
    args: {
        handleButtonClick: () => {
            console.log('button click');
        }
    }
} as Meta;

const PrimaryTemplate = `
    <dot-empty-state
        [rows]="10"
        [colsTextWidth]="[60, 50, 60, 80]"
        icon="web"
        [title]="'message.template.empty.title' | dm"
        [content]="'message.template.empty.content' | dm"
        [buttonLabel]="'message.template.empty.button.label' | dm"
        (buttonClick)="handleButtonClick()"
    >
    </dot-empty-state>
`;

export const Primary: Story = () => {
    return {
        template: PrimaryTemplate
    };
};

Primary.parameters = {
    docs: {
        source: {
            code: PrimaryTemplate
        }
    }
};
