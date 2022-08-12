import { Observable, Subject } from 'rxjs';
import { Component, OnInit, OnDestroy, AfterViewInit } from '@angular/core';
import { FormGroup, Validators, UntypedFormBuilder } from '@angular/forms';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { map, startWith, takeUntil } from 'rxjs/operators';
import { DotRole } from '@dotcms/app/shared/models/dot-role/dot-role.model';
import { DotPageRenderState } from '../../shared/models';
import { DotFavoritePageState, DotFavoritePageStore } from './store/dot-favorite-page.store';

export interface DotFavoritePageProps {
    pageRenderedHtml?: string;
    pageState: DotPageRenderState;
}

export interface DotFavoritePageFormData {
    currentUserRoleId: string;
    thumbnail?: string;
    title: string;
    url: string;
    order: number;
    permissions?: DotRole[];
}

@Component({
    selector: 'dot-favorite-page',
    templateUrl: 'dot-favorite-page.component.html',
    providers: [DotFavoritePageStore]
})
export class DotFavoritePageComponent implements OnInit, AfterViewInit, OnDestroy {
    form: FormGroup;
    isFormValid$: Observable<boolean>;

    vm$: Observable<DotFavoritePageState> = this.store.vm$;

    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor(
        private ref: DynamicDialogRef,
        private config: DynamicDialogConfig,
        private fb: UntypedFormBuilder,
        private store: DotFavoritePageStore
    ) {
        this.store.setInitialStateData({
            isAdmin: this.config.data.page.pageState.user.admin,
            imgWidth: this.config.data.page.pageState.viewAs.device?.cssWidth,
            imgHeight: this.config.data.page.pageState.viewAs.device?.cssHeight,
            pageRenderedHtml: this.config.data.page.pageRenderedHtml
        });
    }

    ngOnInit(): void {
        const { page }: { page: DotFavoritePageProps } = this.config.data;

        this.form = this.fb.group(this.setupForm(page));
        this.isFormValid$ = this.form.valueChanges.pipe(
            takeUntil(this.destroy$),
            map(() => {
                return this.form.valid;
            }),
            startWith(false)
        );

        this.store.currentUserRoleId$
            .pipe(takeUntil(this.destroy$))
            .subscribe((currentUserRoleId: string) => {
                this.form.get('currentUserRoleId').setValue(currentUserRoleId);
            });

        this.store.closeDialog$.pipe(takeUntil(this.destroy$)).subscribe((closeDialog: boolean) => {
            if (closeDialog) {
                this.ref.close(true);
            }
        });
    }

    ngAfterViewInit(): void {
        const dotHtmlToImageElement = document.querySelector('dot-html-to-image');
        dotHtmlToImageElement.addEventListener('pageThumbnail', (event: CustomEvent) => {
            this.form.get('thumbnail').setValue(event.detail.file);
        });
    }

    /**
     * Handle save button
     *
     * @memberof DotFavoritePageComponent
     */
    onSave(): void {
        this.store.saveFavoritePage(this.form.value);
    }

    /**
     * Handle cancel button
     *
     * @memberof DotTemplatePropsComponent
     */
    onCancel(): void {
        this.ref.close(true);
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    private setupForm(page: DotFavoritePageProps) {
        const url =
            `${page.pageState.params.page?.pageURI}?language_id=${page.pageState.params.viewAs.language.id}` +
            (page.pageState.params.viewAs.device?.identifier
                ? `&device_id=${page.pageState.params.viewAs.device?.identifier}`
                : '') +
            (page.pageState.params.site?.identifier
                ? `&host_id=${page.pageState.params.site?.identifier}`
                : '');

        return {
            currentUserRoleId: ['', Validators.required],
            thumbnail: [null, Validators.required],
            title: [page.pageState.params.page?.title, Validators.required],
            url: [url, Validators.required],
            order: [1, Validators.required],
            permissions: []
        };
    }
}
