import { Meta, StoryObj, moduleMetadata } from '@storybook/angular';
import { of } from 'rxjs';

import { NgClass, NgFor, NgIf, TitleCasePipe } from '@angular/common';

import { CardModule } from 'primeng/card';

import { DotMessageService } from '@dotcms/data-access';
import { seoOGTagsMock, seoOGTagsResultMock } from '@dotcms/utils-testing';

import { DotResultsSeoToolComponent } from './dot-results-seo-tool.component';

const meta: Meta<DotResultsSeoToolComponent> = {
    title: 'DotCMS/Results Seo Tool',
    component: DotResultsSeoToolComponent,
    args: {
        hostName: 'A title',
        seoOGTags: seoOGTagsMock,
        seoOGTagsResults: of(seoOGTagsResultMock)
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
};
export default meta;

type Story = StoryObj<DotResultsSeoToolComponent>;

export const Primary: Story = {};
