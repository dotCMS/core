import { Observable, Subject } from 'rxjs';
import { Component, OnInit, OnDestroy, AfterViewInit } from '@angular/core';
import { FormGroup, Validators, UntypedFormBuilder } from '@angular/forms';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { map, startWith, takeUntil } from 'rxjs/operators';
import {
    DotFavoritePageState,
    DotFavoritePageStore,
    DotFavoritePageActionState
} from './store/dot-favorite-page.store';
import { DotPageRenderState, DotRole, DotPageState } from '@dotcms/dotcms-models';
import { generateDotFavoritePageUrl } from '@dotcms/utils';

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
    styleUrls: ['./dot-favorite-page.component.scss'],
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

        this.form = this.fb.group(this.setupForm(page.pageState));
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

        this.store.actionState$
            .pipe(takeUntil(this.destroy$))
            .subscribe((actionState: DotFavoritePageActionState) => {
                if (actionState === DotFavoritePageActionState.SAVED) {
                    this.config.data?.onSave?.(this.form.get('url').value);
                } else if (actionState === DotFavoritePageActionState.DELETED) {
                    this.config.data?.onDelete?.(this.form.get('url').value);
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

    /**
     * Handle Delete button
     *
     * @param {string} inode
     * @memberof DotFavoritePageComponent
     */
    onDelete(inode: string): void {
        this.store.deleteFavoritePage(inode);
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    private setupForm(pageState: DotPageRenderState) {
        const params: DotFavoritePageFormData = {
            currentUserRoleId: '',
            thumbnail: '',
            title: '',
            url: '',
            order: 1,
            permissions: []
        };

        if (pageState.state?.favoritePage) {
            const { state }: { state: DotPageState } = pageState;

            params.title = state.favoritePage.title;
            params.order = state.favoritePage['order'];
            params.thumbnail = state.favoritePage['screenshot'];
            params.url = state.favoritePage.url;

            this.store.setInodeStored(state.favoritePage.inode);
        } else {
            params.title = pageState.params.page?.title;
            params.url = generateDotFavoritePageUrl(pageState);
            params.order = 1;
        }

        return {
            currentUserRoleId: ['', Validators.required],
            thumbnail: [params.thumbnail, Validators.required],
            title: [params.title, Validators.required],
            url: [params.url, Validators.required],
            order: [params.order, Validators.required],
            permissions: []
        };
    }
}
