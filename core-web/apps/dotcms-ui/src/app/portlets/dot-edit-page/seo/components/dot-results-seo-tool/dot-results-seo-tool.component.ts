import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, Input } from '@angular/core';

import { CardModule } from 'primeng/card';

import { DotPageRenderState } from '@dotcms/dotcms-models';

import { DotEditContentHtmlService } from '../../../content/services/dot-edit-content-html/dot-edit-content-html.service';

@Component({
    selector: 'dot-results-seo-tool',
    standalone: true,
    imports: [CommonModule, CardModule],
    providers: [DotEditContentHtmlService],
    templateUrl: './dot-results-seo-tool.component.html',
    styleUrls: ['./dot-results-seo-tool.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotResultsSeoToolComponent {
    @Input() pageState: DotPageRenderState;
    @Input() seoOGTags;
    @Input() seoOGTagsResults;

    constructor(public dotEditContentHtmlService: DotEditContentHtmlService) {}
}
