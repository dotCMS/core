import { Meta, moduleMetadata, StoryObj } from '@storybook/angular';

import { ButtonModule } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';

import { DotMessageService } from '@dotcms/data-access';
import { DotClipboardUtil, DotCopyButtonComponent } from '@dotcms/ui';

const meta: Meta<DotCopyButtonComponent> = {
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

type Story = StoryObj<DotCopyButtonComponent>;

export const Primary: Story = {};
