import { Meta, moduleMetadata, Story } from '@storybook/angular';

import { ButtonModule } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';

import { DotMessageService } from '@dotcms/data-access';
import { DotClipboardUtil, DotCopyButtonComponent } from '@dotcms/ui';

export default {
    title: 'DotCMS/Copy Button',
    component: DotCopyButtonComponent,
    args: {
        label: 'Copy',
        tooltipText: 'Tooltip'
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
} as Meta;

export const Primary: Story = (args) => ({
    props: args
});
