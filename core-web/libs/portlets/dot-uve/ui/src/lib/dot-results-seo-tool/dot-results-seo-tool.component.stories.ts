import { Meta, StoryObj, moduleMetadata } from '@storybook/angular';
import { of } from 'rxjs';

import { NgClass, NgFor, NgIf, TitleCasePipe } from '@angular/common';

import { CardModule } from 'primeng/card';

import { DotMessageService, DotSeoMetaTagsUtilService } from '@dotcms/data-access';
import { seoOGTagsMock, seoOGTagsResultMock } from '@dotcms/utils-testing';

import { DotResultsSeoToolComponent } from './dot-results-seo-tool.component';

const meta: Meta<DotResultsSeoToolComponent> = {
    title: 'DotCMS/Results Seo Tool',
    component: DotResultsSeoToolComponent,
    decorators: [
        moduleMetadata({
            imports: [NgClass, CardModule, NgFor, TitleCasePipe, NgIf],
            providers: [
                {
                    provide: DotMessageService,
                    useValue: {
                        get: () => 'Read More'
                    }
                },
                {
                    provide: DotSeoMetaTagsUtilService
                }
            ]
        })
    ]
};
export default meta;

type Story = StoryObj<DotResultsSeoToolComponent>;

export const Primary: Story = {
    args: {
        hostName: 'A title',
        seoMedia: 'all',
        seoOGTags: seoOGTagsMock,
        seoOGTagsResults: of(seoOGTagsResultMock)
    }
};
