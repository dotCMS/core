import { of as observableOf, Observable } from 'rxjs';
import { DotTemplate } from './../../shared/models/dot-template.model';
import { DotPageRenderState } from './../../shared/models/dot-rendered-page-state.model';
import { Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { DotMessageService } from '@services/dot-messages-service';
import { DotPageRender } from '../../shared/models/dot-rendered-page.model';
import { DotLicenseService } from '@services/dot-license/dot-license.service';
import { DotContentletEditorService } from '@components/dot-contentlet-editor/services/dot-contentlet-editor.service';
import { map } from 'rxjs/operators';
import * as _ from 'lodash';

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

    isEnterpriseLicense: boolean;
    model: Observable<DotEditPageNavItem[]>;

    constructor(
        private dotLicenseService: DotLicenseService,
        private dotContentletEditorService: DotContentletEditorService,
        private dotMessageService: DotMessageService,
        public route: ActivatedRoute
    ) {}

    ngOnChanges(changes: SimpleChanges): void {
        if (this.layoutChanged(changes)) {
            this.model = !this.model
                ? this.loadNavItems()
                : observableOf(this.getNavItems(this.pageState, this.isEnterpriseLicense));
        }
    }

    private layoutChanged(changes: SimpleChanges): boolean {
        return changes.pageState.firstChange
            ? true
            : !_.isEqual(
                  changes.pageState.currentValue.layout,
                  changes.pageState.previousValue.layout
              );
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
        return [
            {
                needsEntepriseLicense: false,
                disabled: false,
                icon: 'description',
                label: this.dotMessageService.get('editpage.toolbar.nav.content'),
                link: 'content'
            },
            this.getLayoutNavItem(dotRenderedPage, enterpriselicense),
            this.getRulesNavItem(dotRenderedPage, enterpriselicense),
            {
                needsEntepriseLicense: false,
                disabled: false,
                icon: 'add',
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
    }

    private getLayoutNavItem(
        dotRenderedPage: DotPageRender,
        enterpriselicense: boolean
    ): DotEditPageNavItem {
        // Right now we only allowing users to edit layout, so no templates or advanced template can be edit from here.
        // https://github.com/dotCMS/core-web/pull/589
        return {
            needsEntepriseLicense: !enterpriselicense,
            disabled: !this.canGoToLayout(dotRenderedPage),
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
            disabled: false,
            icon: 'tune',
            label: this.dotMessageService.get('editpage.toolbar.nav.rules'),
            link: `rules/${dotRenderedPage.page.identifier}`
        };
    }

    private getTemplateItemLabel(template: DotTemplate): string {
        return this.dotMessageService.get(
            !template ? 'editpage.toolbar.nav.layout' : 'editpage.toolbar.nav.layout'
        );
    }
}
