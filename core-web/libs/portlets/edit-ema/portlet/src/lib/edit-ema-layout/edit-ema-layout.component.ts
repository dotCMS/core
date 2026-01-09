import { Subject } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import {
    ChangeDetectionStrategy,
    Component,
    DestroyRef,
    OnDestroy,
    OnInit,
    effect,
    inject,
    signal
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router } from '@angular/router';

import { MessageService } from 'primeng/api';

import { debounceTime, distinctUntilChanged, finalize, concatMap, take, tap } from 'rxjs/operators';

import { DotMessageService, DotPageLayoutService, DotRouterService } from '@dotcms/data-access';
import { DotTemplateDesigner } from '@dotcms/dotcms-models';
import { TemplateBuilderComponent } from '@dotcms/template-builder';

import { UVE_STATUS } from '../shared/enums';
import { UVEStore } from '../store/dot-uve.store';

export const DEBOUNCE_TIME = 5000;

@Component({
    selector: 'dot-edit-ema-layout',
    imports: [TemplateBuilderComponent],
    templateUrl: './edit-ema-layout.component.html',
    styleUrls: ['./edit-ema-layout.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class EditEmaLayoutComponent implements OnInit, OnDestroy {
    private readonly dotRouterService = inject(DotRouterService);
    private readonly dotPageLayoutService = inject(DotPageLayoutService);
    private readonly messageService = inject(MessageService);
    private readonly dotMessageService = inject(DotMessageService);
    readonly #router = inject(Router);
    readonly #destroyRef = inject(DestroyRef);

    protected readonly uveStore = inject(UVEStore);

    protected readonly $layoutProperties = this.uveStore.$layoutProps;

    readonly $handleCanEditLayout = effect(() => {
        // The only way to enter here directly is by the URL, so we need to redirect the user to the correct page
        if (this.uveStore.$canEditLayout()) {
            return;
        }

        this.#router.navigate(['edit-page/content'], {
            queryParamsHandling: 'merge'
        });
    });

    readonly $lastTemplate = signal<DotTemplateDesigner | null>(null);

    updateTemplate$ = new Subject<DotTemplateDesigner>();
    readonly $hasPendingSave = signal(false);

    ngOnInit(): void {
        this.initSaveTemplateDebounce();
        this.initForceSaveOnLeave();
    }

    /**
     * Cleanup lifecycle hook.
     * Saves any pending template changes before component destruction and completes the Subject
     * to prevent memory leaks.
     *
     * Note: $hasPendingSave is necessary because if the component is destroyed during debounceTime,
     * the HTTP request never starts (takeUntilDestroyed cancels it), so we need to save manually here.
     * If destroyed during the HTTP request, the request completes but callbacks don't run.
     *
     * @memberof EditEmaLayoutComponent
     */
    ngOnDestroy() {
        // If there's a pending save (component destroyed during debounceTime), save immediately
        if (this.$hasPendingSave() && this.$lastTemplate()) {
            const template = this.$lastTemplate();
            if (template) {
                // Save immediately without debounce
                // We don't use takeUntilDestroyed here because the component is already being destroyed
                // and we want the HTTP request to complete
                this.dotPageLayoutService
                    .save(this.uveStore.$layoutProps().pageId, {
                        ...template,
                        title: null
                    })
                    .pipe(take(1))
                    .subscribe({
                        next: () => {
                            this.uveStore.reloadCurrentPage();
                        },
                        error: (err: HttpErrorResponse) => {
                            this.handleErrorSaveTemplate(err);
                        },
                        complete: () => {
                            this.dotRouterService.allowRouteDeactivation();
                        }
                    });
            }
        }

        // Complete the Subject to prevent memory leaks
        this.updateTemplate$.complete();
    }

    /**
     * Handles template updates by triggering the debounced save mechanism
     * and preventing route deactivation until the save is complete.
     *
     * @param {DotTemplateDesigner} template - The updated template design to save
     * @memberof EditEmaLayoutComponent
     */
    nextTemplateUpdate(template: DotTemplateDesigner): void {
        this.dotRouterService.forbidRouteDeactivation();
        this.updateTemplate$.next(template);
    }

    /**
     * Updates the last known template state by merging the provided partial template
     * with the existing template data.
     *
     * @param {Partial<DotTemplateDesigner>} template - Partial template data to merge
     * @memberof EditEmaLayoutComponent
     */
    updateLastTemplate(template: Partial<DotTemplateDesigner>): void {
        this.$lastTemplate.update((prev) => ({ ...prev, ...template }));
    }

    /**
     * Saves the template immediately without debounce.
     * Used when the user explicitly requests a save or when leaving the page.
     * Note: To save a layout (not a template), the title must be null.
     *
     * @param {DotTemplateDesigner} template - The template to save
     * @memberof EditEmaLayoutComponent
     */
    saveTemplate(template: DotTemplateDesigner): void {
        this.messageService.add({
            severity: 'info',
            summary: 'Info',
            detail: this.dotMessageService.get('dot.common.message.saving'),
            life: 1000
        });

        this.dotPageLayoutService
            // To save a layout and not a template, the title should be null
            .save(this.uveStore.$layoutProps().pageId, { ...template, title: null })
            .pipe(take(1))
            .subscribe(
                () => this.handleSuccessSaveTemplate(),
                (err: HttpErrorResponse) => this.handleErrorSaveTemplate(err),
                () => this.dotRouterService.allowRouteDeactivation()
            );
    }

    /**
     * Initializes the debounced save mechanism for template updates.
     * Accumulates changes in $lastTemplate as they arrive, and saves automatically
     * after the user stops making changes for 1 second.
     *
     * Flow:
     * 1. Each change immediately updates $lastTemplate (accumulates state)
     * 2. debounceTime waits 1 second of inactivity (timer resets on each change)
     * 3. After inactivity, concatMap reads the accumulated $lastTemplate and sends the request
     * 4. concatMap ensures requests are queued and processed sequentially
     *
     * We use a Subject here instead of directly debouncing the HTTP request because
     * we need to reset the debounce timer every time a new update is received.
     * If we debounced the HTTP request directly, each new update would create a new
     * debounced observable instead of resetting the timer.
     *
     * @private
     * @see https://stackoverflow.com/questions/35991867/angular-2-using-observable-debounce-with-http-get
     * @see https://blog.bitsrc.io/3-ways-to-debounce-http-requests-in-angular-c407eb165ada
     * @memberof EditEmaLayoutComponent
     */
    private initSaveTemplateDebounce(): void {
        this.updateTemplate$
            .pipe(
                tap(() => {
                    this.$hasPendingSave.set(true);
                    // Prevent the user from accessing page properties while saving
                    this.uveStore.setUveStatus(UVE_STATUS.LOADING);
                }),
                tap((layout) => this.updateLastTemplate(layout)),
                debounceTime(DEBOUNCE_TIME),
                concatMap(() => {
                    const currentTemplate = this.$lastTemplate();

                    return this.dotPageLayoutService
                        .save(this.uveStore.$layoutProps().pageId, {
                            ...currentTemplate,
                            title: null
                        })
                        .pipe(
                            tap((response) => this.updateLastTemplate({ layout: response.layout })),
                            finalize(() => {
                                this.$hasPendingSave.set(false);
                                this.dotRouterService.allowRouteDeactivation();
                            })
                        );
                }),
                // takeUntilDestroyed after concatMap allows the HTTP request to complete
                // even if the component is destroyed during execution.
                // However, if destroyed during debounceTime, the request never starts,
                // so we need $hasPendingSave to detect and save in ngOnDestroy.
                takeUntilDestroyed(this.#destroyRef)
            )
            .subscribe({
                next: () => this.handleSuccessSaveTemplate(),
                error: (err: HttpErrorResponse) => {
                    this.$hasPendingSave.set(false);
                    this.handleErrorSaveTemplate(err);
                }
            });
    }

    /**
     * Handles successful template save operations.
     * Shows a success message, reloads the current page, and marks the client as not ready.
     *
     * @private
     * @memberof EditEmaLayoutComponent
     */
    private handleSuccessSaveTemplate(): void {
        this.messageService.add({
            severity: 'success',
            summary: 'Success',
            detail: this.dotMessageService.get('dot.common.message.saved'),
            life: 500
        });
        this.uveStore.reloadCurrentPage();
        this.uveStore.setIsClientReady(false);
    }

    /**
     * Handles errors that occur during template save operations.
     * Shows an error message and updates the UVE store status to ERROR.
     *
     * @private
     * @param {HttpErrorResponse} _ - The HTTP error response (unused but required for error handler signature)
     * @memberof EditEmaLayoutComponent
     */
    private handleErrorSaveTemplate(_: HttpErrorResponse): void {
        this.messageService.add({
            severity: 'error',
            summary: 'Error',
            detail: this.dotMessageService.get('dot.common.http.error.400.message')
        });

        this.uveStore.setUveStatus(UVE_STATUS.ERROR);
    }

    /**
     * Initializes the force save mechanism when the user attempts to leave the page.
     * Saves the current template state immediately when a page leave request is detected.
     * Uses distinctUntilChanged to prevent multiple save attempts when clicking on routes.
     *
     * @private
     * @memberof EditEmaLayoutComponent
     */
    private initForceSaveOnLeave(): void {
        this.dotRouterService.pageLeaveRequest$
            .pipe(
                takeUntilDestroyed(this.#destroyRef),
                distinctUntilChanged() // Prevent spam of toasts when clicking on routes
            )
            .subscribe(() => {
                const template = this.$lastTemplate();
                if (template) {
                    this.saveTemplate(template);
                }
            });
    }
}
