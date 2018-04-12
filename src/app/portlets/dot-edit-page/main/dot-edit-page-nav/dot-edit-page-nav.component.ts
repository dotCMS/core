import { DotTemplate } from './../../shared/models/dot-template.model';
import { DotRenderedPageState } from './../../shared/models/dot-rendered-page-state.model';
import { Component, Input, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { DotMessageService } from '../../../../api/services/dot-messages-service';
import { Observable } from 'rxjs/Observable';
import { DotRenderedPage } from '../../shared/models/dot-rendered-page.model';
import { DotLicenseService } from '../../../../api/services/dot-license/dot-license.service';

interface DotEditPageNavItem {
    needsEntepriseLicense: boolean;
    disabled: boolean;
    icon: string;
    label: string;
    link: string[];
}

@Component({
    selector: 'dot-edit-page-nav',
    templateUrl: './dot-edit-page-nav.component.html',
    styleUrls: ['./dot-edit-page-nav.component.scss']
})
export class DotEditPageNavComponent implements OnInit {
    @Input() pageState: DotRenderedPageState;
    model: Observable<DotEditPageNavItem[]>;

    constructor(public route: ActivatedRoute, private dotMessageService: DotMessageService, private dotLicenseService: DotLicenseService) {}

    ngOnInit(): void {
        this.model = this.dotMessageService
            .getMessages([
                'editpage.toolbar.nav.content',
                'editpage.toolbar.nav.layout',
                'editpage.toolbar.nav.code',
                'editpage.toolbar.nav.license.enterprise.only'
            ])
            .mergeMap(() => {
                return this.dotLicenseService.isEnterpriseLicense();
            })
            .mergeMap((isEnterpriseLicense: boolean) => {
                return Observable.of(this.getNavItems(this.pageState, isEnterpriseLicense));
            });
    }

    private canGoToLayout(dotRenderedPage: DotRenderedPage): boolean {
        return !dotRenderedPage.page.canEdit;
    }

    private getNavItems(dotRenderedPage: DotRenderedPage, enterpriselicense: boolean): DotEditPageNavItem[] {
        const result = [
            {
                needsEntepriseLicense: false,
                disabled: false,
                icon: 'fa fa-file-text',
                label: this.dotMessageService.get('editpage.toolbar.nav.content'),
                link: ['./content']
            }
        ];

        // Right now we only allowing users to edit layout, so no templates or advanced template can be edit from here.
        // https://github.com/dotCMS/core-web/pull/589
        if (dotRenderedPage.layout) {
            result.push(this.getTemplateNavItem(dotRenderedPage, enterpriselicense));
        }

        return result;
    }

    private getTemplateNavItem(dotRenderedPage: DotRenderedPage, enterpriselicense: boolean): DotEditPageNavItem {
        return {
            needsEntepriseLicense: !enterpriselicense,
            disabled: this.canGoToLayout(dotRenderedPage),
            icon: this.getTemplateItemIcon(dotRenderedPage.template),
            label: this.getTemplateItemLabel(dotRenderedPage.template),
            link: ['./layout']
        };
    }

    private getTemplateItemIcon(template: DotTemplate): string {
        return !template ? 'fa fa-th-large' : template.drawed ? 'fa fa-th-large' : 'fa fa-code';
    }

    private getTemplateItemLabel(template: DotTemplate): string {
        return this.dotMessageService.get(
            !template ? 'editpage.toolbar.nav.layout' : template.drawed ? 'editpage.toolbar.nav.layout' : 'editpage.toolbar.nav.code'
        );
    }
}
