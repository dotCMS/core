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
    SEO_MEDIA_TYPES
} from '../../../content/services/dot-edit-content-html/models/meta-tags-model';
import { DotSeoMetaTagsService } from '../../../content/services/html/dot-seo-meta-tags.service';

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
        DotPipesModule
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

    constructor(private dotSeoMetaTagsService: DotSeoMetaTagsService) {}

    allPreview: MetaTagsPreview[];
    mainPreview: MetaTagsPreview;
    seoMediaTypes = SEO_MEDIA_TYPES;
    readMore = {
        [SEO_MEDIA_TYPES.FACEBOOK]: [
            {
                label: 'Learn more about <a target="_blank" href="https://ogp.me/">The Open Graph Protocol.</a>',
                link: true
            },
            {
                label: '<a target="_blank" href="https://developers.facebook.com/tools/debug/">Sharing Debugger - Meta for Developers</a>',
                link: true
            },
            {
                label: 'og:title content should be between 55 and 150 characters.'
            },

            {
                label: 'og:title content should be unique across your site.'
            },
            {
                label: 'og:image sizes should be under 1200 x 630 pixels.'
            },
            {
                label: 'og:image file sizes should be under 8 MB.'
            },
            {
                label: 'Read more about social media tile  <a target="_blank" href="https://blog.hootsuite.com/social-media-image-sizes-guide/">image sizes</a>',
                link: true
            }
        ],
        [SEO_MEDIA_TYPES.TWITTER]: [
            {
                label: 'The Open Graph protocol',
                url: 'https://ogp.me/'
            },
            {
                label: 'Sharing Debugger - Meta for Developers',
                url: 'https://developers.facebook.com/tools/debug/'
            }
        ],
        [SEO_MEDIA_TYPES.LINKEDIN]: [
            {
                label: 'LinkedIn’s <a target="_blank"  href="https://www.linkedin.com/post-inspector/">Post Inspector</a> Tool',
                link: true
            },
            {
                label: '<a target="_blank" href="https://www.linkedin.com/pulse/meta-tags-getting-them-right-linkedin-evelyn-pei/">Meta Tags: Getting Them Right for LinkedIn.</a>',
                link: true
            },
            {
                label: 'Read more about social media tile <a target="_blank" href="https://blog.hootsuite.com/social-media-image-sizes-guide/">image sizes.</a>',
                link: true
            }
        ],
        [SEO_MEDIA_TYPES.GOOGLE]: [
            {
                label: 'Favicons should be <a target="_blank" href="https://favicon.io/">.ico</a> files.',
                link: true
            },
            {
                label: 'HTML Title content should be between 30 and 60 characters.'
            },
            {
                label: 'HTML Title content should be unique per page across your site.'
            },
            { label: 'Meta Description tags should be under 160 characters.' },
            {
                label: 'The length of the Description allowed will depend on the reader/\'/s device size; on the smallest size only about 110 characters are allowed. Longer descriptions will show up with some sort of "read more" or "expand" option.'
            },
            {
                label: '<a target="_blank" href="https://ahrefs.com/blog/seo-meta-tags/">Meta Tags for SEO: A Simple Guide for Beginners</a>',
                link: true
            },
            {
                label: '<a target="_blank" href="https://moz.com/learn/seo/meta-description">What Are Meta Descriptions And How to Write Them</a>',
                link: true
            },
            {
                label: 'Read more about social media tile <a target="_blank" href="https://blog.hootsuite.com/social-media-image-sizes-guide/">image sizes.</a>',
                link: true
            }
        ]
    };

    ngOnInit() {
        this.allPreview = [
            {
                hostName: this.hostName,
                title: this.seoOGTags['og:title'],
                description: this.seoOGTags.description,
                type: 'Desktop',
                isMobile: false,
                image: this.seoOGTags['og:image'],
                twitterTitle: this.seoOGTags['twitter:title'] ?? this.seoOGTags['og:title'],
                twitterCard: this.seoOGTags['twitter:card'],
                twitterDescription:
                    this.seoOGTags['twitter:description'] ?? this.seoOGTags['og:description'],
                twitterImage: this.seoOGTags['twitter:image']
            },
            {
                hostName: this.hostName,
                title: this.seoOGTags['og:title'],
                description: this.seoOGTags.description,
                type: 'Mobile',
                isMobile: true
            }
        ];

        const [preview] = this.allPreview;
        this.mainPreview = preview;
    }

    ngOnChanges() {
        this.currentResults$ = this.seoOGTagsResults.pipe(
            map((tags) => {
                return this.dotSeoMetaTagsService.getFilteredMetaTagsByMedia(tags, this.seoMedia);
            })
        );
    }
}
