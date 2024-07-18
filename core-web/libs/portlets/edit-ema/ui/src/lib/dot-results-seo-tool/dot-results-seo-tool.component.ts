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

import { DotSeoMetaTagsService, DotSeoMetaTagsUtilService } from '@dotcms/data-access';
import {
    SeoMetaTags,
    SeoMetaTagsResult,
    SEO_MEDIA_TYPES,
    MetaTagsPreview,
    SEO_LIMITS
} from '@dotcms/dotcms-models';
import { DotMessagePipe, DotSafeHtmlPipe } from '@dotcms/ui';

import { DotSelectSeoToolComponent } from '../dot-select-seo-tool/dot-select-seo-tool.component';
import { DotSeoImagePreviewComponent } from '../dot-seo-image-preview/dot-seo-image-preview.component';

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
        DotSafeHtmlPipe,
        DotMessagePipe,
        DotMessagePipe,
        DotSelectSeoToolComponent,
        DotSeoImagePreviewComponent
    ],
    providers: [DotSeoMetaTagsService],
    templateUrl: './dot-results-seo-tool.component.html',
    styleUrls: ['./dot-results-seo-tool.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotResultsSeoToolComponent implements OnInit, OnChanges {
    @Input() hostName: string;
    @Input() seoMedia: string;
    @Input() seoOGTags?: SeoMetaTags;
    @Input() seoOGTagsResults?: Observable<SeoMetaTagsResult[]>;
    currentResults$: Observable<SeoMetaTagsResult[]>;
    readMoreValues: Record<SEO_MEDIA_TYPES, string[]>;

    constructor(private dotSeoMetaTagsUtilService: DotSeoMetaTagsUtilService) {}
    allPreview: MetaTagsPreview[];
    mainPreview: MetaTagsPreview;
    seoMediaTypes = SEO_MEDIA_TYPES;
    noFavicon = false;

    ngOnInit(): void {
        const truncateText = (text: string, limit: number) => {
            if (!text) {
                return '';
            }

            if (text.length <= limit) {
                return text;
            }

            const truncated = text.slice(0, limit);

            return truncated.slice(0, truncated.lastIndexOf(' ')) + '...';
        };

        const title =
            truncateText(this.seoOGTags?.['og:title'], SEO_LIMITS.MAX_OG_TITLE_LENGTH) ||
            truncateText(this.seoOGTags?.title, SEO_LIMITS.MAX_OG_TITLE_LENGTH);

        const description =
            truncateText(
                this.seoOGTags?.['og:description'],
                SEO_LIMITS.MAX_OG_DESCRIPTION_LENGTH
            ) || truncateText(this.seoOGTags?.description, SEO_LIMITS.MAX_OG_DESCRIPTION_LENGTH);

        const twitterDescriptionProperties = [
            'twitter:description',
            'og:description',
            'description'
        ];
        const twitterTitleProperties = ['twitter:title', 'og:title', 'title'];

        const twitterDescription = twitterDescriptionProperties
            .map((property) =>
                truncateText(this.seoOGTags?.[property], SEO_LIMITS.MAX_TWITTER_DESCRIPTION_LENGTH)
            )
            .find((value) => value !== undefined && value.length > 0);

        const twitterTitle = twitterTitleProperties
            .map((property) =>
                truncateText(this.seoOGTags?.[property], SEO_LIMITS.MAX_TWITTER_TITLE_LENGTH)
            )
            .find((value) => value !== undefined && value.length > 0);

        this.allPreview = [
            {
                hostName: this.hostName,
                title,
                description,
                type: 'Desktop',
                isMobile: false,
                image: this.seoOGTags?.['og:image'],
                twitterTitle,
                twitterDescription,
                twitterCard: this.seoOGTags?.['twitter:card'],
                twitterImage: this.seoOGTags?.['twitter:image']
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
        this.readMoreValues = this.dotSeoMetaTagsUtilService.getReadMore();
    }

    ngOnChanges() {
        this.currentResults$ = this.seoOGTagsResults?.pipe(
            map((tags) => {
                return this.dotSeoMetaTagsUtilService.getFilteredMetaTagsByMedia(
                    tags,
                    this.seoMedia
                );
            })
        );
    }

    onFaviconError(): void {
        this.noFavicon = true;
    }
}
