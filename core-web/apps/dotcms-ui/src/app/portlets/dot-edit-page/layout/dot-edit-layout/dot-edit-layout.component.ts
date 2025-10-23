import { Subject } from 'rxjs';

import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, HostBinding, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';

import { debounceTime, filter, finalize, pluck, switchMap, take, takeUntil } from 'rxjs/operators';

import {
    DotHttpErrorManagerService,
    DotMessageService,
    DotPageLayoutService,
    DotRouterService,
    DotSessionStorageService,
    DotGlobalMessageService,
    DotPageStateService
} from '@dotcms/data-access';
import { ResponseView } from '@dotcms/dotcms-js';
import {
    DotContainer,
    DotContainerMap,
    DotPageRender,
    DotPageRenderState,
    DotTemplateDesigner,
    FeaturedFlags
} from '@dotcms/dotcms-models';
import { TemplateBuilderModule } from '@dotcms/template-builder';

import { DotTemplateContainersCacheService } from '../../../../api/services/dot-template-containers-cache/dot-template-containers-cache.service';
import { DotGlobalMessageComponent } from '../../../../view/components/_common/dot-global-message/dot-global-message.component';

export const DEBOUNCE_TIME = 5000;

@Component({
    selector: 'dot-edit-layout',
    templateUrl: './dot-edit-layout.component.html',
    styleUrls: ['./dot-edit-layout.component.scss'],
    imports: [CommonModule, RouterModule, TemplateBuilderModule, DotGlobalMessageComponent]
})
export class DotEditLayoutComponent implements OnInit, OnDestroy {
    private route = inject(ActivatedRoute);
    private dotRouterService = inject(DotRouterService);
    private dotGlobalMessageService = inject(DotGlobalMessageService);
    private dotHttpErrorManagerService = inject(DotHttpErrorManagerService);
    private dotPageLayoutService = inject(DotPageLayoutService);
    private dotMessageService = inject(DotMessageService);
    private templateContainersCacheService = inject(DotTemplateContainersCacheService);
    private dotSessionStorageService = inject(DotSessionStorageService);
    private router = inject(Router);

    pageState: DotPageRender | DotPageRenderState;
    apiLink: string;

    updateTemplate = new Subject<DotTemplateDesigner>();
    destroy$: Subject<boolean> = new Subject<boolean>();
    featureFlag = FeaturedFlags.FEATURE_FLAG_TEMPLATE_BUILDER;

    containerMap: DotContainerMap;

    @HostBinding('style.minWidth') width = '100%';

    private lastLayout: DotTemplateDesigner;
    private pageStateStore = inject(DotPageStateService);

    templateIdentifier = signal('');

    ngOnInit() {
        this.route.parent.parent.data
            .pipe(
                pluck('content'),
                filter((state: DotPageRenderState) => !!state),
                take(1)
            )
            .subscribe((state: DotPageRenderState) => {
                this.updatePageState(state);

                const mappedContainers = this.getRemappedContainers(state.containers);
                this.templateContainersCacheService.set(mappedContainers);
            });

        this.pageStateStore.state$.pipe(takeUntil(this.destroy$)).subscribe((state) => {
            this.updatePageState(state);
        });

        this.saveTemplateDebounce();
        this.apiLink = `api/v1/page/render${this.pageState.page.pageURI}?language_id=${this.pageState.page.languageId}`;
        this.subscribeOnChangeBeforeLeaveHandler();
    }

    ngOnDestroy() {
        if (!this.router.routerState.snapshot.url.startsWith('/edit-page/content')) {
            this.dotSessionStorageService.removeVariantId();
        }

        this.destroy$.next(true);
        this.destroy$.complete();
    }

    /**
     * Updates the page state and the template identifier with a new state.
     *
     * @param {DotPageRenderState} newState
     * @memberof DotEditLayoutComponent
     */
    updatePageState(newState: DotPageRenderState | DotPageRender) {
        this.pageState = newState;
        this.templateIdentifier.set(newState.template.identifier);
        this.containerMap = newState.containerMap;
    }

    /**
     * Handle cancel in layout event
     *
     * @memberof DotEditLayoutComponent
     */
    onCancel(): void {
        this.dotRouterService.goToEditPage({
            url: this.pageState.page.pageURI
        });
    }

    /**
     * Handle save layout event
     *
     * @param {DotTemplate} value
     * @memberof DotEditLayoutComponent
     */
    onSave(value: DotTemplateDesigner): void {
        this.dotGlobalMessageService.loading(
            this.dotMessageService.get('dot.common.message.saving')
        );

        this.dotPageLayoutService
            // To save a layout and no a template the title should be null
            .save(this.pageState.page.identifier, { ...value, title: null })
            .pipe(take(1))
            .subscribe(
                (updatedPage: DotPageRender) => this.handleSuccessSaveTemplate(updatedPage),
                (err: ResponseView) => this.handleErrorSaveTemplate(err),
                () => this.dotRouterService.allowRouteDeactivation()
            );
    }

    /**
     *  Handle next template value;
     *
     * @param {DotLayout} value
     * @memberof DotEditLayoutComponent
     */
    nextUpdateTemplate(value: DotTemplateDesigner) {
        this.dotRouterService.forbidRouteDeactivation();
        this.updateTemplate.next(value);
        this.lastLayout = value;
    }

    /**
     * Save template changes after 10 seconds
     *
     * @private
     * @memberof DotEditLayoutComponent
     */
    private saveTemplateDebounce() {
        // The reason why we are using a Subject [updateTemplate] here is
        // because we can not just simply add a debounceTime to the HTTP Request
        // we need to reset the time everytime the observable is called.
        // More Information Here:
        // - https://stackoverflow.com/questions/35991867/angular-2-using-observable-debounce-with-http-get
        // - https://blog.bitsrc.io/3-ways-to-debounce-http-requests-in-angular-c407eb165ada
        this.updateTemplate
            .pipe(
                // debounceTime should be before takeUntil to avoid calling the observable after unsubscribe.
                // More information: https://stackoverflow.com/questions/58974320/how-is-it-possible-to-stop-a-debounced-rxjs-observable
                debounceTime(DEBOUNCE_TIME),
                takeUntil(this.destroy$),
                switchMap((layout: DotTemplateDesigner) => {
                    this.dotGlobalMessageService.loading(
                        this.dotMessageService.get('dot.common.message.saving')
                    );

                    return this.dotPageLayoutService
                        .save(this.pageState.page.identifier, {
                            ...layout,
                            title: null
                        })
                        .pipe(finalize(() => this.dotRouterService.allowRouteDeactivation()));
                })
            )
            .subscribe(
                (updatedPage: DotPageRender) => this.handleSuccessSaveTemplate(updatedPage),
                (err: ResponseView) => this.handleErrorSaveTemplate(err)
            );
    }

    /**
     *
     * Handle Success on Save template
     * @param {DotPageRender} updatedPage
     * @memberof DotEditLayoutComponent
     */
    private handleSuccessSaveTemplate(updatedPage: DotPageRender) {
        const mappedContainers = this.getRemappedContainers(updatedPage.containers);
        this.templateContainersCacheService.set(mappedContainers);

        this.dotGlobalMessageService.success(
            this.dotMessageService.get('dot.common.message.saved')
        );
        this.templateIdentifier.set(updatedPage.template.identifier);
        // We need to pass the new layout to the template builder to sync the value with the backend
        this.updatePageState(updatedPage);
    }

    /**
     *
     * Handle Error on Save template
     * @param {ResponseView} err
     * @memberof DotEditLayoutComponent
     */
    private handleErrorSaveTemplate(err: ResponseView) {
        this.dotGlobalMessageService.error(err.response.statusText);
        this.dotHttpErrorManagerService.handle(new HttpErrorResponse(err.response)).subscribe();
    }

    private getRemappedContainers(containers: {
        [key: string]: {
            container: DotContainer;
        };
    }): DotContainerMap {
        return Object.keys(containers).reduce(
            (acc: { [key: string]: DotContainer }, id: string) => {
                return {
                    ...acc,
                    [id]: containers[id].container
                };
            },
            {}
        );
    }

    /**
     * Handle save changes before leave
     *
     * @private
     * @memberof DotEditLayoutComponent
     */
    private subscribeOnChangeBeforeLeaveHandler(): void {
        this.dotRouterService.pageLeaveRequest$.pipe(takeUntil(this.destroy$)).subscribe(() => {
            this.onSave(this.lastLayout);
        });
    }
}
