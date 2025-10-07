import { Observable, of as observableOf } from 'rxjs';

import { DOCUMENT } from '@angular/common';
import { Component, Input, OnChanges, ViewChild, inject } from '@angular/core';
import { ActivatedRoute, Params } from '@angular/router';

import { map } from 'rxjs/operators';

import { DotLicenseService, DotMessageService } from '@dotcms/data-access';
import {
    DotPageRender,
    DotPageRenderState,
    DotPageToolUrlParams,
    DotTemplate,
    FEATURE_FLAG_NOT_FOUND,
    FeaturedFlags
} from '@dotcms/dotcms-models';
import { DotPageToolsSeoComponent } from '@dotcms/portlets/dot-ema/ui';

import { DotContentletEditorService } from '../../../../view/components/dot-contentlet-editor/services/dot-contentlet-editor.service';

interface DotEditPageNavItem {
    action?: (inode: string) => void;
    disabled: boolean;
    icon: string;
    label: string;
    link?: string;
    needsEntepriseLicense: boolean;
    tooltip?: string;
}
/**
 * Display the navigation for edit page
 *
 * @export
 * @class DotEditPageNavComponent
 * @implements {OnChanges}
 * @deprecated use the new nav bar from edit-ema
 */
@Component({
    selector: 'dot-edit-page-nav',
    templateUrl: './dot-edit-page-nav.component.html',
    styleUrls: ['./dot-edit-page-nav.component.scss'],
    standalone: false
})
export class DotEditPageNavComponent implements OnChanges {
    private dotLicenseService = inject(DotLicenseService);
    private dotContentletEditorService = inject(DotContentletEditorService);
    private dotMessageService = inject(DotMessageService);
    private readonly route = inject(ActivatedRoute);
    private document = inject<Document>(DOCUMENT);

    @ViewChild('pageTools') pageTools: DotPageToolsSeoComponent;
    @Input() pageState: DotPageRenderState;

    isEnterpriseLicense: boolean;
    model: Observable<DotEditPageNavItem[]>;
    currentUrlParams: DotPageToolUrlParams;

    queryParams: Params;

    isVariantMode = false;

    ngOnChanges(): void {
        this.model = !this.model
            ? this.loadNavItems()
            : observableOf(this.getNavItems(this.pageState, this.isEnterpriseLicense));

        this.currentUrlParams = this.getCurrentURLParams();
    }

    private loadNavItems(): Observable<DotEditPageNavItem[]> {
        return this.dotLicenseService.isEnterprise().pipe(
            map((isEnterpriseLicense: boolean) => {
                this.isEnterpriseLicense = isEnterpriseLicense;

                return this.getNavItems(this.pageState, isEnterpriseLicense);
            })
        );
    }

    private canGoToLayout(dotRenderedPage: DotPageRender): boolean {
        // Right now we only allowing users to edit layout, so no templates or advanced template can be edit from here.
        // https://github.com/dotCMS/core-web/pull/589
        return dotRenderedPage.page.canEdit && dotRenderedPage.template.drawed;
    }

    private getNavItems(
        dotRenderedPage: DotPageRender,
        enterpriselicense: boolean
    ): DotEditPageNavItem[] {
        const navItems: DotEditPageNavItem[] = [
            {
                needsEntepriseLicense: false,
                disabled: false,
                icon: 'insert_drive_file',
                label: this.dotMessageService.get('editpage.toolbar.nav.content'),
                link: 'content'
            },
            this.getLayoutNavItem(dotRenderedPage, enterpriselicense),
            this.getRulesNavItem(dotRenderedPage, enterpriselicense),
            {
                needsEntepriseLicense: false,
                disabled: false,
                icon: 'more_horiz',
                label: this.dotMessageService.get('editpage.toolbar.nav.properties'),
                action: (inode: string) => {
                    this.dotContentletEditorService.edit({
                        data: {
                            inode: inode
                        }
                    });
                }
            }
        ];

        const loadFrontendExperiments =
            this.route.snapshot.data?.featuredFlags[FeaturedFlags.LOAD_FRONTEND_EXPERIMENTS];
        // By default, or if flag is 'NOT_FOUND', ExperimentsNavItem is added to navItems.
        if (
            loadFrontendExperiments === true ||
            loadFrontendExperiments === FEATURE_FLAG_NOT_FOUND
        ) {
            navItems.push(this.getExperimentsNavItem(dotRenderedPage, enterpriselicense));
        }

        if (this.route.snapshot.data?.featuredFlags[FeaturedFlags.FEATURE_FLAG_SEO_PAGE_TOOLS]) {
            navItems.push(this.getPageToolsNavItem(enterpriselicense));
        }

        return navItems;
    }

    private getLayoutNavItem(
        dotRenderedPage: DotPageRender,
        enterpriselicense: boolean
    ): DotEditPageNavItem {
        // Right now we only allowing users to edit layout, so no templates or advanced template can be edit from here.
        // https://github.com/dotCMS/core-web/pull/589
        return {
            needsEntepriseLicense: !enterpriselicense,
            disabled:
                !this.canGoToLayout(dotRenderedPage) || this.disableLayoutOnExperimentVariant(),
            icon: 'view_quilt',
            label: this.getTemplateItemLabel(dotRenderedPage.template),
            link: 'layout',
            tooltip: dotRenderedPage.template.drawed
                ? null
                : this.dotMessageService.get('editpage.toolbar.nav.layout.advance.disabled')
        };
    }

    private getRulesNavItem(
        dotRenderedPage: DotPageRender,
        enterpriselicense: boolean
    ): DotEditPageNavItem {
        // Right now we only allowing users to edit layout, so no templates or advanced template can be edit from here.
        // https://github.com/dotCMS/core-web/pull/589
        return {
            needsEntepriseLicense: !enterpriselicense,
            disabled: this.isVariantMode ? true : false,
            icon: 'tune',
            label: this.dotMessageService.get('editpage.toolbar.nav.rules'),
            link: `rules/${dotRenderedPage.page.identifier}`
        };
    }

    private getExperimentsNavItem(
        dotRenderedPage: DotPageRender,
        enterpriselicense: boolean
    ): DotEditPageNavItem {
        return {
            needsEntepriseLicense: !enterpriselicense,
            disabled: false,
            icon: 'science',
            label: this.dotMessageService.get('editpage.toolbar.nav.experiments'),
            link: `experiments/${dotRenderedPage.page.identifier}`
        };
    }

    private getPageToolsNavItem(enterpriselicense: boolean): DotEditPageNavItem {
        return {
            needsEntepriseLicense: !enterpriselicense,
            disabled: false,
            icon: 'grid_view',
            label: this.dotMessageService.get('editpage.toolbar.nav.page.tools'),
            action: () => {
                this.showPageTools();
            }
        };
    }

    private getTemplateItemLabel(template: DotTemplate): string {
        return this.dotMessageService.get(
            !template ? 'editpage.toolbar.nav.layout' : 'editpage.toolbar.nav.layout'
        );
    }

    private disableLayoutOnExperimentVariant(): boolean {
        const experimentId = this.route.snapshot?.queryParams?.experimentId;
        const runningExperiment = this.pageState.state?.runningExperiment;

        const isCurrentExperimentAndRunning = experimentId === runningExperiment?.id;

        return isCurrentExperimentAndRunning && this.isVariantMode;
    }

    private showPageTools(): void {
        this.pageTools.toggleDialog();
    }

    /**
     * Get current URL
     * @returns string
     * @memberof DotEditPageMainComponent
     * */
    private getCurrentURLParams(): DotPageToolUrlParams {
        const { page, site } = this.pageState;

        return {
            requestHostName: this.document.defaultView.location.host,
            currentUrl: page.pageURI,
            siteId: site?.identifier,
            languageId: page.languageId
        };
    }
}
