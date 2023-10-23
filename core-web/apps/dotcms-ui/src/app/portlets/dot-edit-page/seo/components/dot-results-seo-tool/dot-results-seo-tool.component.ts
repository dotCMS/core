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
import { DotSeoMetaTagsUtilService } from '../../../content/services/html/dot-seo-meta-tags-util.service';
import { DotSeoMetaTagsService } from '../../../content/services/html/dot-seo-meta-tags.service';
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
        DotMessagePipe,
        DotPipesModule,
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
    @Input() seoOGTags: SeoMetaTags;
    @Input() seoOGTagsResults: Observable<SeoMetaTagsResult[]>;
    currentResults$: Observable<SeoMetaTagsResult[]>;
    readMoreValues: Record<SEO_MEDIA_TYPES, string[]>;

    constructor(private dotSeoMetaTagsUtilService: DotSeoMetaTagsUtilService) {}
    allPreview: MetaTagsPreview[];
    mainPreview: MetaTagsPreview;
    seoMediaTypes = SEO_MEDIA_TYPES;
    noFavicon = false;

    ngOnInit() {
        const title =
            this.seoOGTags['og:title']?.slice(0, SEO_LIMITS.MAX_OG_TITLE_LENGTH) ||
            this.seoOGTags.title?.slice(0, SEO_LIMITS.MAX_TITLE_LENGTH);

        const description =
            this.seoOGTags['og:description']?.slice(0, SEO_LIMITS.MAX_OG_DESCRIPTION_LENGTH) ||
            this.seoOGTags.description?.slice(0, SEO_LIMITS.MAX_DESCRIPTION_LENGTH);

        const twitterDescriptionProperties = [
            'twitter:description',
            'og:description',
            'description'
        ];
        const twitterTitleProperties = ['twitter:title', 'og:title', 'title'];

        const twitterDescription = twitterDescriptionProperties
            .map((property) =>
                this.seoOGTags[property]?.slice(0, SEO_LIMITS.MAX_TWITTER_DESCRIPTION_LENGTH)
            )
            .find((value) => value !== undefined);

        const twitterTitle = twitterTitleProperties
            .map((property) =>
                this.seoOGTags[property]?.slice(0, SEO_LIMITS.MAX_TWITTER_TITLE_LENGTH)
            )
            .find((value) => value !== undefined);

        this.allPreview = [
            {
                hostName: this.hostName,
                title,
                description,
                type: 'Desktop',
                isMobile: false,
                image: this.seoOGTags['og:image'],
                twitterTitle,
                twitterDescription,
                twitterCard: this.seoOGTags['twitter:card'],
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
        this.readMoreValues = this.dotSeoMetaTagsUtilService.getReadMore();
    }

    ngOnChanges() {
        this.currentResults$ = this.seoOGTagsResults.pipe(
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
