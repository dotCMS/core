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

import {
    SeoMetaTags,
    SeoMetaTagsResult
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
        AsyncPipe
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
    currentResults: Observable<SeoMetaTagsResult[]>;

    constructor(private dotSeoMetaTagsService: DotSeoMetaTagsService) {}

    mainPreview = [];
    readMore = [
        {
            label: 'The Open Graph protocol',
            url: 'https://ogp.me/'
        },
        {
            label: 'Sharing Debugger - Meta for Developers',
            url: 'https://developers.facebook.com/tools/debug/'
        }
    ];

    ngOnInit() {
        this.mainPreview = [
            {
                hostName: this.hostName,
                title: this.seoOGTags['og:title'],
                description: this.seoOGTags.description,
                type: 'Desktop',
                isMobile: false,
                image: this.seoOGTags['og:image']
            },
            {
                hostName: this.hostName,
                title: this.seoOGTags['og:title'],
                description: this.seoOGTags.description,
                type: 'Mobile',
                isMobile: true
            }
        ];
    }

    ngOnChanges() {
        this.currentResults = this.dotSeoMetaTagsService.getFilteredMetaTagsByMedia(
            this.seoOGTagsResults,
            this.seoMedia
        );
    }
}
