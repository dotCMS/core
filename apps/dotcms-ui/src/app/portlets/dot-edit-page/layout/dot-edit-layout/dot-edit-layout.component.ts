import { pluck, filter, take } from 'rxjs/operators';
import { Component, HostBinding, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { DotPageRenderState } from '../../shared/models/dot-rendered-page-state.model';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { DotGlobalMessageService } from '@components/_common/dot-global-message/dot-global-message.service';
import { DotPageRender } from '@models/dot-page/dot-rendered-page.model';
import { DotPageLayoutService } from '@services/dot-page-layout/dot-page-layout.service';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { ResponseView } from '@dotcms/dotcms-js';
import { DotTemplateContainersCacheService } from '@services/dot-template-containers-cache/dot-template-containers-cache.service';
import { DotContainerMap, DotContainer } from '@shared/models/container/dot-container.model';
import { DotLayout } from '@models/dot-edit-layout-designer';

@Component({
    selector: 'dot-edit-layout',
    templateUrl: './dot-edit-layout.component.html',
    styleUrls: ['./dot-edit-layout.component.scss']
})
export class DotEditLayoutComponent implements OnInit {
    pageState: DotPageRender | DotPageRenderState;
    apiLink: string;

    @HostBinding('style.minWidth') width = '100%';

    constructor(
        private route: ActivatedRoute,
        private dotRouterService: DotRouterService,
        private dotGlobalMessageService: DotGlobalMessageService,
        private dotPageLayoutService: DotPageLayoutService,
        private dotMessageService: DotMessageService,
        private templateContainersCacheService: DotTemplateContainersCacheService
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

        this.apiLink = `api/v1/page/render${this.pageState.page.pageURI}?language_id=${this.pageState.page.languageId}`;
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
    onSave(value: DotLayout): void {
        this.dotGlobalMessageService.loading(
            this.dotMessageService.get('dot.common.message.saving')
        );

        this.dotPageLayoutService
            .save(this.pageState.page.identifier, {
                ...value,
                // To save a layout and no a template the title should be null
                title: null
            })
            .pipe(take(1))
            .subscribe(
                (updatedPage: DotPageRender) => {
                    const mappedContainers = this.getRemappedContainers(updatedPage.containers);
                    this.templateContainersCacheService.set(mappedContainers);

                    this.dotGlobalMessageService.success(
                        this.dotMessageService.get('dot.common.message.saved')
                    );
                    this.pageState = updatedPage;
                },
                (err: ResponseView) => {
                    this.dotGlobalMessageService.error(err.response.statusText);
                }
            );
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
}
