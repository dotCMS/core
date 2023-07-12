import { fromEvent, Observable, of, Subject } from 'rxjs';

import { CommonModule } from '@angular/common';
import { Component, ElementRef, OnDestroy, OnInit, ViewChild } from '@angular/core';

import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { InputTextModule } from 'primeng/inputtext';

import { distinctUntilChanged, map, startWith, switchMap, takeUntil } from 'rxjs/operators';

import { DotAutofocusModule } from '@directives/dot-autofocus/dot-autofocus.module';
import { DotRouterService } from '@dotcms/app/api/services/dot-router/dot-router.service';
import {
    DotESContentService,
    DotLanguagesService,
    DotPageTypesService,
    DotWorkflowsActionsService
} from '@dotcms/data-access';
import { DotCMSContentType } from '@dotcms/dotcms-models';
import { DotIconModule, DotMessagePipeModule } from '@dotcms/ui';

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

    pageTypes$: Observable<DotCMSContentType[]>;

    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor(
        private dotRouterService: DotRouterService,
        private ref: DynamicDialogRef,
        public config: DynamicDialogConfig
    ) {}

    /**
     * Redirect to Create content page
     * @param {string} variableName
     *
     * @memberof DotPagesCreatePageDialogComponent
     */
    goToCreatePage(variableName: string): void {
        this.ref.close();
        this.dotRouterService.goToURL(`/pages/new/${variableName}`);
    }

    ngOnInit(): void {
        this.pageTypes$ = fromEvent(this.searchInput.nativeElement, 'keyup').pipe(
            takeUntil(this.destroy$),
            map(({ target }: Event) => target['value']),
            distinctUntilChanged(),
            switchMap((searchValue: string) => {
                if (searchValue.length) {
                    return of(
                        this.config.data.filter((pageType: DotCMSContentType) =>
                            pageType.name
                                .toLocaleLowerCase()
                                .includes(searchValue.toLocaleLowerCase())
                        )
                    );
                } else {
                    return of(this.config.data);
                }
            }),
            startWith(this.config.data)
        );
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }
}
