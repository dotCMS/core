import { Observable, Subject } from 'rxjs';

import { CommonModule } from '@angular/common';
import { Component, OnInit, OnDestroy, CUSTOM_ELEMENTS_SCHEMA, inject } from '@angular/core';
import { FormGroup, Validators, UntypedFormBuilder, ReactiveFormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { InputTextModule } from 'primeng/inputtext';

import { filter, map, startWith, takeUntil } from 'rxjs/operators';

import { DotTempFileUploadService, DotWorkflowActionsFireService } from '@dotcms/data-access';
import { DotCMSContentlet } from '@dotcms/dotcms-models';
import {
    DotFormDialogComponent,
    DotFieldValidationMessageComponent,
    DotPagesFavoritePageEmptySkeletonComponent,
    DotFieldRequiredDirective,
    DotMessagePipe
} from '@dotcms/ui';

import {
    DotFavoritePageState,
    DotFavoritePageStore,
    DotFavoritePageActionState
} from './store/dot-favorite-page.store';

export interface DotFavoritePageProps {
    favoritePageUrl?: string;
    isAdmin: boolean;
    favoritePage?: DotCMSContentlet;
}

export interface DotFavoritePageFormData {
    inode?: string;
    thumbnail?: string;
    title: string;
    url: string;
    order: number;
}

@Component({
    selector: 'dot-favorite-page',
    templateUrl: 'dot-favorite-page.component.html',
    styleUrls: ['./dot-favorite-page.component.scss'],
    imports: [
        CommonModule,
        ButtonModule,
        DotFormDialogComponent,
        DotFieldValidationMessageComponent,
        DotPagesFavoritePageEmptySkeletonComponent,
        InputTextModule,
        ReactiveFormsModule,
        DotFieldRequiredDirective,
        DotMessagePipe
    ],
    schemas: [CUSTOM_ELEMENTS_SCHEMA],
    providers: [DotFavoritePageStore, DotTempFileUploadService, DotWorkflowActionsFireService]
})
export class DotFavoritePageComponent implements OnInit, OnDestroy {
    private ref = inject(DynamicDialogRef);
    private config = inject(DynamicDialogConfig);
    private fb = inject(UntypedFormBuilder);
    private store = inject(DotFavoritePageStore);

    form: FormGroup;
    isFormValid$: Observable<boolean>;
    timeStamp: string;

    vm$: Observable<DotFavoritePageState> = this.store.vm$;

    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor() {
        this.store.setInitialStateData({
            favoritePageUrl: this.config.data.page.favoritePageUrl,
            favoritePage: this.config.data.page.favoritePage
        });
    }

    ngOnInit(): void {
        // Needed to avoid browser to cache thumbnail img when reloaded, since it's fetched from the same URL
        this.timeStamp = new Date().getTime().toString();
        this.store.formState$
            .pipe(
                takeUntil(this.destroy$),
                filter((formStateData) => !!formStateData)
            )
            .subscribe((formStateData: DotFavoritePageFormData) => {
                this.form = this.fb.group({
                    inode: [formStateData?.inode],
                    thumbnail: [formStateData?.thumbnail],
                    title: [formStateData?.title, Validators.required],
                    url: [formStateData?.url, Validators.required],
                    order: [formStateData?.order, Validators.required]
                });

                this.isFormValid$ = this.form.valueChanges.pipe(
                    takeUntil(this.destroy$),
                    map(() => {
                        return this.form.valid;
                    }),
                    startWith(false)
                );

                requestAnimationFrame(() => {
                    if (formStateData?.inode) {
                        this.form.get('thumbnail').setValue(formStateData?.thumbnail);
                    } else {
                        this.setPreviewThumbnailListener();
                    }
                });
            });

        this.store.renderThumbnail$
            .pipe(
                takeUntil(this.destroy$),
                filter((renderThumbnail) => renderThumbnail)
            )
            .subscribe(() => {
                requestAnimationFrame(() => {
                    this.setPreviewThumbnailListener();
                });
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

    /**
     * Handle save button
     *
     * @memberof DotFavoritePageComponent
     */
    onSave(): void {
        this.store.saveFavoritePage(this.form.getRawValue());
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

    renderThumbnail(): void {
        this.store.setRenderThumbnail(true);
    }

    private setPreviewThumbnailListener() {
        const dotHtmlToImageElement = document.querySelector('dot-html-to-image');
        if (dotHtmlToImageElement) {
            dotHtmlToImageElement.addEventListener('pageThumbnail', (event: CustomEvent) => {
                if (event.detail.file) {
                    this.form.get('thumbnail').setValue(event.detail.file);
                } else {
                    this.form.get('thumbnail').setValue('');
                    this.store.setShowFavoriteEmptySkeleton(true);
                    console.warn(
                        'We apologize for the inconvenience. The following error occurred while generating a thumbnail for this page:',
                        event?.detail?.error
                    );
                }
            });
        }
    }
}
