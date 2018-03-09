import { DotTemplate } from './../../shared/models/dot-template.model';
import { DotRenderedPageState } from './../../shared/models/dot-rendered-page-state.model';
import { Component, Input, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { DotMessageService } from '../../../../api/services/dot-messages-service';
import { Observable } from 'rxjs/Observable';
import { DotRenderedPage } from '../../shared/models/dot-rendered-page.model';

interface DotEditPageNavItem {
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

    constructor(public route: ActivatedRoute, private dotMessageService: DotMessageService) {}

    ngOnInit(): void {
        this.model = this.dotMessageService
            .getMessages(['editpage.toolbar.nav.content', 'editpage.toolbar.nav.layout', 'editpage.toolbar.nav.code'])
            .mergeMap(() => Observable.of(this.getNavItems(this.pageState)));
    }

    private canGoToLayout(dotRenderedPage: DotRenderedPage): boolean {
        return !dotRenderedPage.page.canEdit;
    }

    private getNavItems(dotRenderedPage: DotRenderedPage): DotEditPageNavItem[] {
        const result = [
            {
                disabled: false,
                icon: 'fa fa-file-text',
                label: this.dotMessageService.get('editpage.toolbar.nav.content'),
                link: ['./content']
            }
        ];

        result.push(this.getTemplateNavItem(dotRenderedPage));

        return result;
    }

    private getTemplateNavItem(dotRenderedPage: DotRenderedPage): DotEditPageNavItem {
        return {
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
