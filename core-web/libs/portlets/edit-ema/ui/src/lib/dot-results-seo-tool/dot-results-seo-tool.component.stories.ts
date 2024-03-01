import { Meta, Story, moduleMetadata } from '@storybook/angular';

import { NgClass, NgFor, NgIf, TitleCasePipe } from '@angular/common';

import { CardModule } from 'primeng/card';

import { DotMessageService } from '@dotcms/data-access';
import { seoOGTagsMock, seoOGTagsResultMock } from '@dotcms/utils-testing';

import { DotResultsSeoToolComponent } from './dot-results-seo-tool.component';

export default {
    title: 'DotCMS/Results Seo Tool',
    component: DotResultsSeoToolComponent,
    args: {
        hostName: 'A title',
        seoOGTags: seoOGTagsMock,
        seoOGTagsResults: seoOGTagsResultMock
    },
    decorators: [
        moduleMetadata({
            imports: [NgClass, CardModule, NgFor, TitleCasePipe, NgIf],
            providers: [
                {
                    provide: DotMessageService,
                    useValue: {
                        get: () => 'Read More'
                    }
                }
            ]
        })
    ]
} as Meta;

export const Primary: Story = (args) => ({
    props: args
});
