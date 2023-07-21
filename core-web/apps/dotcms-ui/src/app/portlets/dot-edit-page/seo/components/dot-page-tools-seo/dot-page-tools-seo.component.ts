import { Observable } from 'rxjs';

import { AsyncPipe, NgForOf } from '@angular/common';
import { ChangeDetectionStrategy, Component, Input, OnInit } from '@angular/core';

import { DialogModule } from 'primeng/dialog';

import { DotPageToolsService } from '@dotcms/data-access';
import { DotPageTool } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

@Component({
    selector: 'dot-page-tools-seo',
    standalone: true,
    providers: [DotPageToolsService],
    imports: [NgForOf, AsyncPipe, DialogModule, DotMessagePipe],
    templateUrl: './dot-page-tools-seo.component.html',
    styleUrls: ['./dot-page-tools-seo.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotPageToolsSeoComponent implements OnInit {
    @Input() visible: boolean;
    @Input() currentPageUrl: string;
    dialogHeader: string;
    tools$: Observable<DotPageTool[]>;

    constructor(private dotPageToolsService: DotPageToolsService) {}

    ngOnInit() {
        this.tools$ = this.dotPageToolsService.get();
    }

    /**
     * This method is used to get the runnable link for the tool
     * @param url
     * @returns
     */

    getRunnableLink(url: string): string {
        return url.replace('{currentPageUrl}', this.currentPageUrl);
    }
}
