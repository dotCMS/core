import { NgClass, NgFor, NgIf, TitleCasePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, Input, OnInit } from '@angular/core';

import { CardModule } from 'primeng/card';

import {
    SeoMetaTags,
    SeoMetaTagsResult
} from '../../../content/services/dot-edit-content-html/models/meta-tags-model';

@Component({
    selector: 'dot-results-seo-tool',
    standalone: true,
    imports: [NgClass, CardModule, NgFor, TitleCasePipe, NgIf],
    templateUrl: './dot-results-seo-tool.component.html',
    styleUrls: ['./dot-results-seo-tool.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotResultsSeoToolComponent implements OnInit {
    @Input() hostName: string;
    @Input() seoOGTags: SeoMetaTags;
    @Input() seoOGTagsResults: SeoMetaTagsResult[];

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
                isMobile: false
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
}
