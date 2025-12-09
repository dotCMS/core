import { Subject } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import {
    ChangeDetectionStrategy,
    Component,
    OnDestroy,
    OnInit,
    effect,
    inject
} from '@angular/core';
import { Router } from '@angular/router';

import { MessageService } from 'primeng/api';

import {
    debounceTime,
    distinctUntilChanged,
    finalize,
    switchMap,
    take,
    takeUntil,
    tap
} from 'rxjs/operators';

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

    private lastTemplate: DotTemplateDesigner;

    updateTemplate$ = new Subject<DotTemplateDesigner>();
    destroy$: Subject<boolean> = new Subject<boolean>();

    ngOnInit(): void {
        this.initSaveTemplateDebounce();
        this.initForceSaveOnLeave();
    }

    ngOnDestroy() {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    /**
     * Handle the update to trigger the debounce and forbid the route deactivation
     *
     * @param {DotTemplateDesigner} template
     * @memberof EditEmaLayoutComponent
     */
    nextTemplateUpdate(template: DotTemplateDesigner) {
        this.dotRouterService.forbidRouteDeactivation();
        this.updateTemplate$.next(template);
        this.lastTemplate = template;
    }

    /**
     * Save the template
     *
     * @param {DotTemplateDesigner} template
     * @memberof EditEmaLayoutComponent
     */
    saveTemplate(template: DotTemplateDesigner) {
        this.messageService.add({
            severity: 'info',
            summary: 'Info',
            detail: this.dotMessageService.get('dot.common.message.saving'),
            life: 1000
        });

        this.dotPageLayoutService
            // To save a layout and no a template the title should be null
            .save(this.uveStore.$layoutProps().pageId, { ...template, title: null })
            .pipe(take(1))
            .subscribe(
                () => this.handleSuccessSaveTemplate(),
                (err: HttpErrorResponse) => this.handleErrorSaveTemplate(err),
                () => this.dotRouterService.allowRouteDeactivation()
            );
    }

    /**
     * Init the debounce to save the template when the Template Builder is idle for 5 seconds
     *
     * @private
     * @memberof EditEmaLayoutComponent
     */
    private initSaveTemplateDebounce() {
        // The reason why we are using a Subject [updateTemplate] here is
        // because we can not just simply add a debounceTime to the HTTP Request
        // we need to reset the time everytime the observable is called.
        // More Information Here:
        // - https://stackoverflow.com/questions/35991867/angular-2-using-observable-debounce-with-http-get
        // - https://blog.bitsrc.io/3-ways-to-debounce-http-requests-in-angular-c407eb165ada
        this.updateTemplate$
            .pipe(
                // debounceTime should be before takeUntil to avoid calling the observable after unsubscribe.
                // More information: https://stackoverflow.com/questions/58974320/how-is-it-possible-to-stop-a-debounced-rxjs-observable
                tap(() => this.uveStore.setUveStatus(UVE_STATUS.LOADING)), // Prevent the user to access page properties
                debounceTime(DEBOUNCE_TIME),
                takeUntil(this.destroy$),
                switchMap((layout: DotTemplateDesigner) => {
                    this.messageService.add({
                        severity: 'info',
                        summary: 'Info',
                        detail: this.dotMessageService.get('dot.common.message.saving'),
                        life: 1000
                    });

                    return this.dotPageLayoutService
                        .save(this.uveStore.$layoutProps().pageId, {
                            ...layout,
                            title: null
                        })
                        .pipe(finalize(() => this.dotRouterService.allowRouteDeactivation()));
                })
            )
            .subscribe(
                () => this.handleSuccessSaveTemplate(),
                (err: HttpErrorResponse) => this.handleErrorSaveTemplate(err)
            );
    }

    /**
     * Handle the success save template
     *
     * @private
     * @template T
     * @memberof EditEmaLayoutComponent
     */
    private handleSuccessSaveTemplate(): void {
        this.messageService.add({
            severity: 'success',
            summary: 'Success',
            detail: this.dotMessageService.get('dot.common.message.saved')
        });
        this.uveStore.reloadCurrentPage();
        this.uveStore.setIsClientReady(false);
    }

    /**
     * Handle the error save template
     *
     * @private
     * @param {HttpErrorResponse} httpError
     * @memberof EditEmaLayoutComponent
     */
    private handleErrorSaveTemplate(_: HttpErrorResponse) {
        this.messageService.add({
            severity: 'error',
            summary: 'Error',
            detail: this.dotMessageService.get('dot.common.http.error.400.message')
        });

        this.uveStore.setUveStatus(UVE_STATUS.ERROR);
    }

    /**
     * Init the force save on leave
     *
     * @private
     * @memberof EditEmaLayoutComponent
     */
    private initForceSaveOnLeave(): void {
        this.dotRouterService.pageLeaveRequest$
            .pipe(takeUntil(this.destroy$), distinctUntilChanged()) // To prevent an spam of toasts when clicking on some route
            .subscribe(() => {
                this.saveTemplate(this.lastTemplate);
            });
    }
}
