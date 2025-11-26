import { Observable, of as observableOf } from 'rxjs';

import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { combineLatest, switchMap } from 'rxjs/operators';

import { DotMenuService } from '../../../../../../api/services/dot-menu.service';
import { IframeComponent } from '../../../../../../view/components/_common/iframe/iframe-component/iframe.component';

@Component({
    selector: 'dot-legacy-addtional-actions',
    templateUrl: './dot-legacy-template-additional-actions-iframe.component.html',
    imports: [CommonModule, IframeComponent]
})
export class DotLegacyTemplateAdditionalActionsComponent implements OnInit {
    private route = inject(ActivatedRoute);
    private dotMenuService = inject(DotMenuService);

    url: Observable<string>;

    ngOnInit(): void {
        this.url = this.route.params.pipe(
            combineLatest(this.dotMenuService.getDotMenuId('templates')),
            switchMap((resp) => {
                const tabName = resp[0].tabName;
                const templateId = resp[0].id;
                const portletId = resp[1];

                return observableOf(
                    // tslint:disable-next-line:max-line-length
                    `c/portal/layout?p_l_id=${portletId}&p_p_id=templates&p_p_action=1&p_p_state=maximized&p_p_mode=view&_templates_struts_action=%2Fext%2Ftemplates%2Fedit_template&_templates_cmd=edit&inode=${templateId}&drawed=false&selectedTab=${tabName}`
                );
            })
        );
    }
}
