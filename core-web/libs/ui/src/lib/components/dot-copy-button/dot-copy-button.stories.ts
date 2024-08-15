import { Meta, moduleMetadata, StoryObj } from '@storybook/angular';

import { ButtonModule } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';

import { DotMessageService } from '@dotcms/data-access';
import { DotClipboardUtil, DotCopyButtonComponent } from '@dotcms/ui';

type Args = DotCopyButtonComponent & {
    label: string;
    originalTooltipText: string;
};

const meta: Meta = {
    title: 'DotCMS/Copy Button',
    component: DotCopyButtonComponent,
    args: {
        label: 'Copy',
        originalTooltipText: 'Tooltip'
    },
    decorators: [
        moduleMetadata({
            imports: [TooltipModule, ButtonModule],
            providers: [
                {
                    provide: DotClipboardUtil,
                    useValue: {
                        copy: () => Promise.resolve()
                    }
                },
                {
                    provide: DotMessageService,
                    useValue: {
                        get: () => 'Copy'
                    }
                }
            ]
        })
    ]
};
export default meta;

type Story = StoryObj<Args>;

export const Primary: Story = {};
