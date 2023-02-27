import { fromEvent, Observable, Subject } from 'rxjs';

import { CommonModule } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    ElementRef,
    OnDestroy,
    OnInit,
    ViewChild
} from '@angular/core';

import { DynamicDialogRef } from 'primeng/dynamicdialog';
import { InputTextModule } from 'primeng/inputtext';

import { debounceTime, takeUntil } from 'rxjs/operators';

import { DotAutofocusModule } from '@directives/dot-autofocus/dot-autofocus.module';
import { DotRouterService } from '@dotcms/app/api/services/dot-router/dot-router.service';
import { DotMessagePipeModule } from '@dotcms/app/view/pipes/dot-message/dot-message-pipe.module';
import {
    DotESContentService,
    DotLanguagesService,
    DotPageTypesService,
    DotWorkflowsActionsService
} from '@dotcms/data-access';
import { DotIconModule } from '@dotcms/ui';

import { DotPagesState, DotPageStore } from '../dot-pages-store/dot-pages.store';

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
    styleUrls: ['./dot-pages-create-page-dialog.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotPagesCreatePageDialogComponent implements OnInit, OnDestroy {
    @ViewChild('searchInput', { static: true }) searchInput: ElementRef;

    vm$: Observable<DotPagesState> = this.store.vm$;

    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor(
        private store: DotPageStore,
        private dotRouterService: DotRouterService,
        private ref: DynamicDialogRef
    ) {
        this.store.getPageTypes('');
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
            .pipe(debounceTime(400), takeUntil(this.destroy$))
            .subscribe(({ target }: Event) => {
                this.store.getPageTypes(target['value']);
            });
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }
}
