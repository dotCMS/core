import { Observable } from 'rxjs';

import {
    AsyncPipe,
    NgClass,
    NgFor,
    NgIf,
    NgSwitch,
    NgSwitchCase,
    NgSwitchDefault,
    TitleCasePipe
} from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    Input,
    OnChanges,
    OnInit,
    inject
} from '@angular/core';

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
import { ellipsizeText } from '@dotcms/utils';

import { DotSelectSeoToolComponent } from '../dot-select-seo-tool/dot-select-seo-tool.component';
import { DotSeoImagePreviewComponent } from '../dot-seo-image-preview/dot-seo-image-preview.component';
@Component({
    selector: 'dot-results-seo-tool',
    imports: [
        NgClass,
        CardModule,
        NgFor,
        TitleCasePipe,
        NgIf,
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
    private dotSeoMetaTagsUtilService = inject(DotSeoMetaTagsUtilService);

    @Input() hostName: string;
    @Input() seoMedia: string;
    @Input() seoOGTags?: SeoMetaTags;
    @Input() seoOGTagsResults?: Observable<SeoMetaTagsResult[]>;
    currentResults$: Observable<SeoMetaTagsResult[]>;
    readMoreValues: Record<SEO_MEDIA_TYPES, string[]>;
    allPreview: MetaTagsPreview[];
    mainPreview: MetaTagsPreview;
    seoMediaTypes = SEO_MEDIA_TYPES;
    noFavicon = false;

    ngOnInit(): void {
        const title = this.seoOGTags?.['og:title'] || this.seoOGTags?.title;
        const ellipsizedTitle = ellipsizeText(title, SEO_LIMITS.MAX_OG_TITLE_LENGTH);

        const description = this.seoOGTags?.['og:description'] || this.seoOGTags?.description;
        const ellipsizedDescription = ellipsizeText(
            description,
            SEO_LIMITS.MAX_OG_DESCRIPTION_LENGTH
        );

        const twitterDescriptionProperties = [
            'twitter:description',
            'og:description',
            'description'
        ];
        const twitterTitleProperties = ['twitter:title', 'og:title', 'title'];

        const twitterDescription = twitterDescriptionProperties
            .map((property) =>
                ellipsizeText(this.seoOGTags?.[property], SEO_LIMITS.MAX_TWITTER_DESCRIPTION_LENGTH)
            )
            .find((value) => value !== undefined && value.length > 0);

        const twitterTitle = twitterTitleProperties
            .map((property) =>
                ellipsizeText(this.seoOGTags?.[property], SEO_LIMITS.MAX_TWITTER_TITLE_LENGTH)
            )
            .find((value) => value !== undefined && value.length > 0);

        this.allPreview = [
            {
                hostName: this.hostName,
                title: ellipsizedTitle,
                description: ellipsizedDescription,
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
                title: ellipsizedTitle,
                description: ellipsizedDescription,
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
