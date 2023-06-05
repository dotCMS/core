import { moduleMetadata, Story, Meta } from '@storybook/angular';

import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { ButtonModule } from 'primeng/button';

import { DotMessageService } from '@dotcms/data-access';
import { DotIconModule } from '@dotcms/ui';

import { TemplateBuilderRowComponent } from './template-builder-row.component';

import { DOT_MESSAGE_SERVICE_TB_MOCK } from '../../utils/mocks';

export default {
    title: 'TemplateBuilderRowComponent',
    component: TemplateBuilderRowComponent,
    decorators: [
        moduleMetadata({
            imports: [BrowserAnimationsModule, DotIconModule, ButtonModule],
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
