import { Observable, Subject } from 'rxjs';

import { Component, OnInit, OnDestroy } from '@angular/core';
import { FormGroup, Validators, UntypedFormBuilder } from '@angular/forms';

import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

import { filter, map, startWith, takeUntil } from 'rxjs/operators';

import { DotCMSContentlet } from '@dotcms/dotcms-models';

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
    currentUserRoleId: string;
    inode?: string;
    thumbnail?: string;
    title: string;
    url: string;
    order: number;
    permissions?: string[];
}

@Component({
    selector: 'dot-favorite-page',
    templateUrl: 'dot-favorite-page.component.html',
    styleUrls: ['./dot-favorite-page.component.scss'],
    providers: [DotFavoritePageStore]
})
export class DotFavoritePageComponent implements OnInit, OnDestroy {
    form: FormGroup;
    isFormValid$: Observable<boolean>;
    timeStamp: string;

    vm$: Observable<DotFavoritePageState> = this.store.vm$;

    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor(
        private ref: DynamicDialogRef,
        private config: DynamicDialogConfig,
        private fb: UntypedFormBuilder,
        private store: DotFavoritePageStore
    ) {
        this.store.setInitialStateData({
            favoritePageUrl: this.config.data.page.favoritePageUrl,
            favoritePage: this.config.data.page.favoritePage
        });
    }

    ngOnInit(): void {
        // Needed to avoid browser to cache thumbnail img when reloaded, since it's fetched from the same URL
        this.timeStamp = new Date().getTime().toString();

        this.store.formState$
            .pipe(takeUntil(this.destroy$))
            .subscribe((formStateData: DotFavoritePageFormData) => {
                if (formStateData) {
                    this.form = this.fb.group({
                        currentUserRoleId: [formStateData?.currentUserRoleId, Validators.required],
                        inode: [formStateData?.inode],
                        thumbnail: [formStateData?.thumbnail, Validators.required],
                        title: [formStateData?.title, Validators.required],
                        url: [{ value: formStateData?.url, disabled: true }, Validators.required],
                        order: [formStateData?.order, Validators.required],
                        permissions: [formStateData?.permissions]
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
                }
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
        dotHtmlToImageElement.addEventListener('pageThumbnail', (event: CustomEvent) => {
            this.form.get('thumbnail').setValue(event.detail.file);
        });
    }
}
