import { Observable } from 'rxjs';

import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, Input, OnInit } from '@angular/core';

import { DialogModule } from 'primeng/dialog';

import { DotPageToolsService } from '@dotcms/data-access';
import { DotPageTool } from '@dotcms/dotcms-models';

@Component({
    selector: 'dot-dot-page-tools-seo',
    standalone: true,
    imports: [CommonModule, DialogModule],
    providers: [DotPageToolsService],

    templateUrl: './dot-page-tools-seo.component.html',
    styleUrls: ['./dot-page-tools-seo.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotPageToolsSeoComponent implements OnInit {
    @Input() visible: boolean;
    @Input() currentPageUrl: string;
    tools$: Observable<DotPageTool[]>;

    constructor(private dotPageToolsService: DotPageToolsService) {}

    ngOnInit() {
        this.tools$ = this.dotPageToolsService.get();
    }

    getRunnableLink(url: string): string {
        return url.replace('{currentPageUrl}', this.currentPageUrl);
    }
}
