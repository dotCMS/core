import { Observable, of } from 'rxjs';

import { AsyncPipe, NgForOf } from '@angular/common';
import { ChangeDetectionStrategy, Component, Input, OnInit } from '@angular/core';

import { ChipModule } from 'primeng/chip';
import { DialogModule } from 'primeng/dialog';

import { switchMap } from 'rxjs/operators';

import { DotPageToolsService } from '@dotcms/data-access';
import { DotPageTool, DotPageToolUrlParams } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

@Component({
    selector: 'dot-page-tools-seo',
    standalone: true,
    providers: [DotPageToolsService],
    imports: [NgForOf, AsyncPipe, DialogModule, DotMessagePipe, ChipModule],
    templateUrl: './dot-page-tools-seo.component.html',
    styleUrls: ['./dot-page-tools-seo.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotPageToolsSeoComponent implements OnInit {
    @Input() visible: boolean;
    @Input() currentPageUrlParams: DotPageToolUrlParams;
    dialogHeader: string;
    tools$: Observable<DotPageTool[]>;

    constructor(private dotPageToolsService: DotPageToolsService) {}

    ngOnInit() {
        this.tools$ = this.dotPageToolsService.get().pipe(
            switchMap((tools) => {
                const updatedTools = tools.map((tool) => {
                    return {
                        ...tool,
                        runnableLink: this.getRunnableLink(tool.runnableLink)
                    };
                });

                return of(updatedTools);
            })
        );
    }

    private getQueryParameterSeparator(url: string): string {
        if (url.indexOf('?') === -1) {
            return '?';
        }

        return '&';
    }

    /**
     * This method is used to get the runnable link for the tool
     * @param url
     * @returns
     */
    private getRunnableLink(url: string): string {
        const { currentUrl, requestHostName, siteId, languageId } = this.currentPageUrlParams;

        url = url.replace('{requestHostName}', requestHostName ?? '');
        url = url.replace('{currentUrl}', currentUrl ?? '');
        url = url.replace(
            '{siteId}',
            siteId ? `${this.getQueryParameterSeparator(url)}host_id=${siteId}` : ''
        );
        url = url.replace(
            '{languageId}',
            languageId
                ? `${this.getQueryParameterSeparator(url)}language_id=${String(languageId)}`
                : ''
        );

        return url;
    }
}
