import { Observable } from 'rxjs';

import {
    AsyncPipe,
    JsonPipe,
    NgClass,
    NgFor,
    NgIf,
    NgSwitch,
    NgSwitchCase,
    NgSwitchDefault,
    TitleCasePipe
} from '@angular/common';
import { ChangeDetectionStrategy, Component, Input, OnChanges, OnInit } from '@angular/core';

import { CardModule } from 'primeng/card';

import { map } from 'rxjs/operators';

import { DotPipesModule } from '@dotcms/app/view/pipes/dot-pipes.module';
import { DotMessagePipe } from '@dotcms/ui';

import {
    MetaTagsPreview,
    SeoMetaTags,
    SeoMetaTagsResult,
    SEO_MEDIA_TYPES,
    SEO_LIMITS
} from '../../../content/services/dot-edit-content-html/models/meta-tags-model';
import { DotSeoMetaTagsService } from '../../../content/services/html/dot-seo-meta-tags.service';
import { DotSelectSeoToolComponent } from '../dot-select-seo-tool/dot-select-seo-tool.component';

@Component({
    selector: 'dot-results-seo-tool',
    standalone: true,
    imports: [
        NgClass,
        CardModule,
        NgFor,
        TitleCasePipe,
        NgIf,
        JsonPipe,
        NgSwitch,
        NgSwitchCase,
        NgSwitchDefault,
        AsyncPipe,
        DotMessagePipe,
        DotPipesModule,
        DotSelectSeoToolComponent
    ],
    providers: [DotSeoMetaTagsService],
    templateUrl: './dot-results-seo-tool.component.html',
    styleUrls: ['./dot-results-seo-tool.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotResultsSeoToolComponent implements OnInit, OnChanges {
    @Input() hostName: string;
    @Input() seoMedia: string;
    @Input() seoOGTags: SeoMetaTags;
    @Input() seoOGTagsResults: Observable<SeoMetaTagsResult[]>;
    currentResults$: Observable<SeoMetaTagsResult[]>;
    readMoreValues: Record<SEO_MEDIA_TYPES, string[]>;

    constructor(private dotSeoMetaTagsService: DotSeoMetaTagsService) {}
    allPreview: MetaTagsPreview[];
    mainPreview: MetaTagsPreview;
    seoMediaTypes = SEO_MEDIA_TYPES;

    ngOnInit() {
        const title =
            this.seoOGTags['og:title']?.slice(0, SEO_LIMITS.MAX_OG_TITLE_LENGTH) ||
            this.seoOGTags['title']?.slice(0, SEO_LIMITS.MAX_TITLE_LENGTH);

        const description =
            this.seoOGTags['og:description']?.slice(0, SEO_LIMITS.MAX_OG_DESCRIPTION_LENGTH) ||
            this.seoOGTags.description?.slice(0, SEO_LIMITS.MAX_DESCRIPTION_LENGTH);

        this.allPreview = [
            {
                hostName: this.hostName,
                title,
                description,
                type: 'Desktop',
                isMobile: false,
                image: this.seoOGTags['og:image'],
                twitterTitle:
                    this.seoOGTags['twitter:title']?.slice(
                        0,
                        SEO_LIMITS.MAX_TWITTER_TITLE_LENGTH
                    ) ?? this.seoOGTags['og:title'],
                twitterCard: this.seoOGTags['twitter:card'],
                twitterDescription:
                    this.seoOGTags['twitter:description']?.slice(
                        0,
                        SEO_LIMITS.MAX_TWITTER_DESCRIPTION_LENGTH
                    ) ?? this.seoOGTags['og:description'],
                twitterImage: this.seoOGTags['twitter:image']
            },
            {
                hostName: this.hostName,
                title,
                description,
                type: 'Mobile',
                isMobile: true
            }
        ];

        const [preview] = this.allPreview;
        this.mainPreview = preview;
        this.readMoreValues = this.dotSeoMetaTagsService.getReadMore();
    }

    ngOnChanges() {
        this.currentResults$ = this.seoOGTagsResults.pipe(
            map((tags) => {
                return this.dotSeoMetaTagsService.getFilteredMetaTagsByMedia(tags, this.seoMedia);
            })
        );
    }
}
