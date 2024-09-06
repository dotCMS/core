/* eslint-disable no-console */
import { action } from '@storybook/addon-actions';
import { Meta, moduleMetadata, StoryObj, argsToTemplate } from '@storybook/angular';

import { DotMessageService } from '@dotcms/data-access';
import { DotSafeHtmlPipe } from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotEmptyStateComponent } from './dot-empty-state.component';
import { DotEmptyStateModule } from './dot-empty-state.module';

const messageServiceMock = new MockDotMessageService({
    'message.template.empty.title': 'Your template list is empty',
    'message.template.empty.content':
        "You haven't added anything yet, start by clicking the button below",
    'message.template.empty.button.label': 'Add New Template'
});

const meta: Meta<DotEmptyStateComponent> = {
    title: 'DotCMS/Structure/Empty State',
    decorators: [
        moduleMetadata({
            imports: [DotEmptyStateModule, DotSafeHtmlPipe],
            providers: [{ provide: DotMessageService, useValue: messageServiceMock }]
        })
    ],
    parameters: {
        docs: {}
    },
    render: (args) => ({
        props: {
            ...args,
            buttonClick: action('buttonClick')
        },
        template: `<dot-empty-state ${argsToTemplate(args)} />`
    })
};
export default meta;

type Story = StoryObj<DotEmptyStateComponent>;

export const Default: Story = {
    args: {
        rows: 10,
        colsTextWidth: [60, 50, 60, 80],
        icon: 'web',
        title: 'Your template list is empty',
        content: "You haven't added anything yet, start by clicking the button below",
        buttonLabel: 'Add New Template'
    }
};
