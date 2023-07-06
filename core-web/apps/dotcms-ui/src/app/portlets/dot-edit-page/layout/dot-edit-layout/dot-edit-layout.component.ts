import { Subject } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { Component, HostBinding, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { debounceTime, filter, finalize, pluck, switchMap, take, takeUntil } from 'rxjs/operators';

import { DotGlobalMessageService } from '@components/_common/dot-global-message/dot-global-message.service';
import { DotEditLayoutService } from '@dotcms/app/api/services/dot-edit-layout/dot-edit-layout.service';
import { DotHttpErrorManagerService } from '@dotcms/app/api/services/dot-http-error-manager/dot-http-error-manager.service';
import { DotRouterService } from '@dotcms/app/api/services/dot-router/dot-router.service';
import { DotTemplateContainersCacheService } from '@dotcms/app/api/services/dot-template-containers-cache/dot-template-containers-cache.service';
import {
    DotMessageService,
    DotPageLayoutService,
    DotSessionStorageService
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

@Component({
    selector: 'dot-edit-layout',
    templateUrl: './dot-edit-layout.component.html',
    styleUrls: ['./dot-edit-layout.component.scss']
})
export class DotEditLayoutComponent implements OnInit, OnDestroy {
    pageState: DotPageRender | DotPageRenderState;
    apiLink: string;

    updateTemplate = new Subject<DotTemplateDesigner>();
    destroy$: Subject<boolean> = new Subject<boolean>();
    featureFlag = FeaturedFlags.FEATURE_FLAG_TEMPLATE_BUILDER;

    @HostBinding('style.minWidth') width = '100%';

    private lastLayout: DotTemplateDesigner;

    constructor(
        private route: ActivatedRoute,
        private dotRouterService: DotRouterService,
        private dotGlobalMessageService: DotGlobalMessageService,
        private dotHttpErrorManagerService: DotHttpErrorManagerService,
        private dotEditLayoutService: DotEditLayoutService,
        private dotPageLayoutService: DotPageLayoutService,
        private dotMessageService: DotMessageService,
        private templateContainersCacheService: DotTemplateContainersCacheService,
        private dotSessionStorageService: DotSessionStorageService,
        private router: Router
    ) {}

    ngOnInit() {
        this.route.parent.parent.data
            .pipe(
                pluck('content'),
                filter((state: DotPageRenderState) => !!state),
                take(1)
            )
            .subscribe((state: DotPageRenderState) => {
                this.pageState = state;
                const mappedContainers = this.getRemappedContainers(state.containers);
                this.templateContainersCacheService.set(mappedContainers);
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
                () => this.canRouteBeDesativated(true)
            );
    }

    /**
     *  Handle next template value;
     *
     * @param {DotLayout} value
     * @memberof DotEditLayoutComponent
     */
    nextUpdateTemplate(value: DotTemplateDesigner) {
        this.canRouteBeDesativated(false);
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
                debounceTime(10000),
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
                        .pipe(finalize(() => this.canRouteBeDesativated(true)));
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
        this.pageState = updatedPage;
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

    /**
     * Let the user leave the route only when changes have been saved.
     *
     * @private
     * @param {boolean} value
     * @memberof DotEditLayoutComponent
     */
    private canRouteBeDesativated(value: boolean): void {
        this.dotEditLayoutService.changeDesactivateState(value);
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
        this.dotEditLayoutService.closeEditLayout$
            .pipe(takeUntil(this.destroy$))
            .subscribe((res) => {
                if (res) {
                    this.onSave(this.lastLayout);
                }
            });
    }
}
