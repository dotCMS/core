import { Observable, of } from 'rxjs';

import { AsyncPipe, NgForOf } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    Input,
    OnChanges,
    OnInit,
    SimpleChanges
} from '@angular/core';

import { ChipModule } from 'primeng/chip';
import { DialogModule } from 'primeng/dialog';

import { switchMap } from 'rxjs/operators';

import { DotPageToolsService } from '@dotcms/data-access';
import { DotPageTool, DotPageToolUrlParams } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';
import { getRunnableLink } from '@dotcms/utils';

@Component({
    selector: 'dot-page-tools-seo',
    standalone: true,
    providers: [DotPageToolsService],
    imports: [NgForOf, AsyncPipe, DialogModule, DotMessagePipe, ChipModule],
    templateUrl: './dot-page-tools-seo.component.html',
    styleUrls: ['./dot-page-tools-seo.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotPageToolsSeoComponent implements OnInit, OnChanges {
    @Input() visible: boolean;
    @Input() currentPageUrlParams: DotPageToolUrlParams;
    dialogHeader: string;
    tools$: Observable<DotPageTool[]>;

    constructor(private dotPageToolsService: DotPageToolsService) {}

    ngOnInit() {
        this.tools$ = this.getTools();
    }

    ngOnChanges(changes: SimpleChanges) {
        if (changes.currentPageUrlParams && !changes.currentPageUrlParams.firstChange) {
            const prevParams: DotPageToolUrlParams = changes.currentPageUrlParams.previousValue;
            const currParams: DotPageToolUrlParams = changes.currentPageUrlParams.currentValue;
            if (prevParams.currentUrl !== currParams.currentUrl) {
                this.tools$ = this.getTools();
            }
        }
    }

    private getTools(): Observable<DotPageTool[]> {
        return this.dotPageToolsService.get().pipe(
            switchMap((tools) => {
                const updatedTools = tools.map((tool) => {
                    return {
                        ...tool,
                        runnableLink: getRunnableLink(tool.runnableLink, this.currentPageUrlParams)
                    };
                });

                return of(updatedTools);
            })
        );
    }

    public toggleDialog(): void {
        this.visible = !this.visible;
    }
}
