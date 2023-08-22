import { NgClass, NgFor, NgIf, TitleCasePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, Input, OnInit } from '@angular/core';

import { CardModule } from 'primeng/card';

import { DotPageRenderState } from '@dotcms/dotcms-models';

import { DotEditContentHtmlService } from '../../../content/services/dot-edit-content-html/dot-edit-content-html.service';

@Component({
    selector: 'dot-results-seo-tool',
    standalone: true,
    imports: [NgClass, CardModule, NgFor, TitleCasePipe, NgIf],
    providers: [DotEditContentHtmlService],
    templateUrl: './dot-results-seo-tool.component.html',
    styleUrls: ['./dot-results-seo-tool.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotResultsSeoToolComponent implements OnInit {
    @Input() pageState: DotPageRenderState;
    @Input() seoOGTags;
    @Input() seoOGTagsResults;

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

    constructor(public dotEditContentHtmlService: DotEditContentHtmlService) {}

    ngOnInit() {
        this.mainPreview = [
            {
                hostName: this.pageState.page.hostName,
                title: this.seoOGTags['og:title'],
                description: this.seoOGTags.description,
                type: 'Desktop'
            },
            {
                hostName: this.pageState.page.hostName,
                title: this.seoOGTags['og:title'],
                description: this.seoOGTags.description,
                type: 'Mobile'
            }
        ];
    }
}
