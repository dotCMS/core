import { Observable, of as observableOf } from 'rxjs';

import { Component, EventEmitter, Input, OnChanges, Output } from '@angular/core';
import { ActivatedRoute, Params } from '@angular/router';

import { map } from 'rxjs/operators';

import { DotContentletEditorService } from '@components/dot-contentlet-editor/services/dot-contentlet-editor.service';
import { DotLicenseService, DotMessageService } from '@dotcms/data-access';
import {
    DotPageRender,
    DotPageRenderState,
    DotTemplate,
    FeaturedFlags
} from '@dotcms/dotcms-models';

interface DotEditPageNavItem {
    action?: (inode: string) => void;
    disabled: boolean;
    icon: string;
    label: string;
    link?: string;
    needsEntepriseLicense: boolean;
    tooltip?: string;
}

@Component({
    selector: 'dot-edit-page-nav',
    templateUrl: './dot-edit-page-nav.component.html',
    styleUrls: ['./dot-edit-page-nav.component.scss']
})
export class DotEditPageNavComponent implements OnChanges {
    @Input() pageState: DotPageRenderState;
    @Output() openOverlayPanel: EventEmitter<void> = new EventEmitter();

    isEnterpriseLicense: boolean;
    model: Observable<DotEditPageNavItem[]>;

    queryParams: Params;

    isVariantMode = false;

    constructor(
        private dotLicenseService: DotLicenseService,
        private dotContentletEditorService: DotContentletEditorService,
        private dotMessageService: DotMessageService,
        private readonly route: ActivatedRoute
    ) {}

    ngOnChanges(): void {
        this.model = !this.model
            ? this.loadNavItems()
            : observableOf(this.getNavItems(this.pageState, this.isEnterpriseLicense));
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

        if (this.route.snapshot.data?.featuredFlags[FeaturedFlags.LOAD_FRONTEND_EXPERIMENTS]) {
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
                this.openOverlayPanel.emit();
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
}
