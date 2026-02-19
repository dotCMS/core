import { fromEvent, Observable, of, Subject } from 'rxjs';

import { CommonModule } from '@angular/common';
import { Component, ElementRef, OnDestroy, OnInit, ViewChild, inject } from '@angular/core';

import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { InputTextModule } from 'primeng/inputtext';

import { distinctUntilChanged, map, startWith, switchMap, takeUntil } from 'rxjs/operators';

import {
    DotESContentService,
    DotLanguagesService,
    DotPageTypesService,
    DotRouterService,
    DotWorkflowsActionsService
} from '@dotcms/data-access';
import { DotCMSContentType } from '@dotcms/dotcms-models';
import { DotAutofocusDirective, DotIconComponent, DotMessagePipe } from '@dotcms/ui';

@Component({
    selector: 'dot-pages-create-page-dialog',
    imports: [
        CommonModule,
        DotAutofocusDirective,
        DotIconComponent,
        DotMessagePipe,
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
    private dotRouterService = inject(DotRouterService);
    private ref = inject(DynamicDialogRef);
    config = inject(DynamicDialogConfig);

    @ViewChild('searchInput', { static: true }) searchInput: ElementRef;

    pageTypes$: Observable<DotCMSContentType[]>;

    private destroy$: Subject<boolean> = new Subject<boolean>();

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
                        this.config.data.pageTypes.filter((pageType: DotCMSContentType) =>
                            pageType.name
                                .toLocaleLowerCase()
                                .includes(searchValue.toLocaleLowerCase())
                        )
                    );
                } else {
                    return of(this.config.data.pageTypes);
                }
            }),
            startWith(this.config.data.pageTypes)
        );
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }
}
