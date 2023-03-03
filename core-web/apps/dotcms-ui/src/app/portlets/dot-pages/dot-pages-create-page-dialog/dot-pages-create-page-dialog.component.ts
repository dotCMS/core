import { fromEvent, Subject } from 'rxjs';

import { CommonModule } from '@angular/common';
import { Component, ElementRef, OnDestroy, OnInit, ViewChild } from '@angular/core';

import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { InputTextModule } from 'primeng/inputtext';

import { takeUntil } from 'rxjs/operators';

import { DotAutofocusModule } from '@directives/dot-autofocus/dot-autofocus.module';
import { DotRouterService } from '@dotcms/app/api/services/dot-router/dot-router.service';
import { DotMessagePipeModule } from '@dotcms/app/view/pipes/dot-message/dot-message-pipe.module';
import {
    DotESContentService,
    DotLanguagesService,
    DotPageTypesService,
    DotWorkflowsActionsService
} from '@dotcms/data-access';
import { DotCMSContentType } from '@dotcms/dotcms-models';
import { DotIconModule } from '@dotcms/ui';

@Component({
    selector: 'dot-pages-create-page-dialog',
    standalone: true,
    imports: [
        CommonModule,
        DotAutofocusModule,
        DotIconModule,
        DotMessagePipeModule,
        InputTextModule
    ],
    providers: [
        DotESContentService,
        DotLanguagesService,
        DotPageTypesService,
        DotWorkflowsActionsService
    ],
    templateUrl: './dot-pages-create-page-dialog.component.html',
    styleUrls: ['./dot-pages-create-page-dialog.component.scss']
})
export class DotPagesCreatePageDialogComponent implements OnInit, OnDestroy {
    @ViewChild('searchInput', { static: true }) searchInput: ElementRef;

    pageTypes: DotCMSContentType[];
    pageTypesBackup: DotCMSContentType[];

    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor(
        private dotRouterService: DotRouterService,
        private ref: DynamicDialogRef,
        public config: DynamicDialogConfig
    ) {
        this.pageTypes = this.config.data;
        this.pageTypesBackup = [...this.pageTypes];
    }

    /**
     * Redirect to Create content page
     * @param {string} variableName
     *
     * @memberof DotPagesCreatePageDialogComponent
     */
    goToCreatePage(variableName: string): void {
        this.ref.close();
        this.dotRouterService.goToCreateContent(variableName);
    }

    ngOnInit(): void {
        fromEvent(this.searchInput.nativeElement, 'keyup')
            .pipe(takeUntil(this.destroy$))
            .subscribe(({ target }: Event) => {
                if (target['value'].length) {
                    this.pageTypes = this.pageTypesBackup.filter((pageType: DotCMSContentType) =>
                        pageType.name
                            .toLocaleLowerCase()
                            .includes(target['value'].toLocaleLowerCase())
                    );
                } else {
                    this.pageTypes = [...this.pageTypesBackup];
                }
            });
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }
}
