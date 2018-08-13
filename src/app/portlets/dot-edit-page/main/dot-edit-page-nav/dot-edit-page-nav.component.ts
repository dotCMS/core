import { DotTemplate } from './../../shared/models/dot-template.model';
import { DotRenderedPageState } from './../../shared/models/dot-rendered-page-state.model';
import { Component, Input, OnChanges, SimpleChanges} from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { DotMessageService } from '../../../../api/services/dot-messages-service';
import { Observable } from 'rxjs/Observable';
import { DotRenderedPage } from '../../shared/models/dot-rendered-page.model';
import { DotLicenseService } from '../../../../api/services/dot-license/dot-license.service';
import { DotContentletEditorService } from '../../../../view/components/dot-contentlet-editor/services/dot-contentlet-editor.service';
import { map } from 'rxjs/operators/map';
import { mergeMap } from 'rxjs/operators/mergeMap';
import * as _ from 'lodash';

interface DotEditPageNavItem {
    needsEntepriseLicense: boolean;
    disabled: boolean;
    icon: string;
    label: string;
    link?: string;
    action?: object;
    tooltip?: string;
}

@Component({
    selector: 'dot-edit-page-nav',
    templateUrl: './dot-edit-page-nav.component.html',
    styleUrls: ['./dot-edit-page-nav.component.scss']
})
export class DotEditPageNavComponent implements OnChanges {
    @Input() pageState: DotRenderedPageState;
    model: Observable<DotEditPageNavItem[]>;
    isEnterpriseLicense: boolean;

    constructor(
        private dotLicenseService: DotLicenseService,
        private dotContentletEditorService: DotContentletEditorService,
        public dotMessageService: DotMessageService,
        public route: ActivatedRoute
    ) {}

    ngOnChanges(changes: SimpleChanges): void {
        if (this.layoutChanged(changes)) {
            this.model = !this.model ? this.loadNavItems() : Observable.of(this.getNavItems(this.pageState, this.isEnterpriseLicense));
        }
    }

    private layoutChanged(changes: SimpleChanges): boolean {
        return changes.pageState.firstChange
            ? true
            : !_.isEqual(changes.pageState.currentValue.layout, changes.pageState.previousValue.layout);
    }

    private loadNavItems(): Observable<DotEditPageNavItem[]> {
        return this.dotMessageService
            .getMessages([
                'editpage.toolbar.nav.content',
                'editpage.toolbar.nav.properties',
                'editpage.toolbar.nav.layout',
                'editpage.toolbar.nav.code',
                'editpage.toolbar.nav.license.enterprise.only',
                'editpage.toolbar.nav.layout.advance.disabled'
            ])
            .pipe(
                mergeMap(() => {
                    return this.dotLicenseService.isEnterprise();
                }),
                map((isEnterpriseLicense: boolean) => {
                    this.isEnterpriseLicense = isEnterpriseLicense;
                    return this.getNavItems(this.pageState, isEnterpriseLicense);
                })
            );
    }

    private canGoToLayout(dotRenderedPage: DotRenderedPage): boolean {
        // Right now we only allowing users to edit layout, so no templates or advanced template can be edit from here.
        // https://github.com/dotCMS/core-web/pull/589
        return dotRenderedPage.page.canEdit && dotRenderedPage.template.drawed;
    }

    private getNavItems(dotRenderedPage: DotRenderedPage, enterpriselicense: boolean): DotEditPageNavItem[] {
        const result = [
            {
                needsEntepriseLicense: false,
                disabled: false,
                icon: 'description',
                label: this.dotMessageService.get('editpage.toolbar.nav.content'),
                link: 'content'
            },
            this.getTemplateNavItem(dotRenderedPage, enterpriselicense),
            {
                needsEntepriseLicense: false,
                disabled: false,
                icon: 'add',
                label: this.dotMessageService.get('editpage.toolbar.nav.properties'),
                action: (inode) => {
                    this.dotContentletEditorService.edit({
                        data: {
                            inode: inode
                        }
                    });
                }
            }
        ];

        return result;
    }

    private getTemplateNavItem(dotRenderedPage: DotRenderedPage, enterpriselicense: boolean): DotEditPageNavItem {
        // Right now we only allowing users to edit layout, so no templates or advanced template can be edit from here.
        // https://github.com/dotCMS/core-web/pull/589
        return {
            needsEntepriseLicense: !enterpriselicense,
            disabled: !this.canGoToLayout(dotRenderedPage),
            icon: 'view_quilt',
            label: this.getTemplateItemLabel(dotRenderedPage.template),
            link: 'layout',
            tooltip: dotRenderedPage.template.drawed ? null : this.dotMessageService.get('editpage.toolbar.nav.layout.advance.disabled')
        };
    }

    private getTemplateItemLabel(template: DotTemplate): string {
        return this.dotMessageService.get(!template ? 'editpage.toolbar.nav.layout' : 'editpage.toolbar.nav.layout');
    }
}
