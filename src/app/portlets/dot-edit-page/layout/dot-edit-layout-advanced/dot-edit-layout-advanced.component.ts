import { fromEvent as observableFromEvent, Observable } from 'rxjs';

import { map } from 'rxjs/operators';
import { Component, OnInit, Input, NgZone } from '@angular/core';
import { DotMenuService } from '@services/dot-menu.service';
import { DotGlobalMessageService } from '@components/_common/dot-global-message/dot-global-message.service';
import { DotMessageService } from '@services/dot-messages-service';
import { DotRenderedPageState } from '../../shared/models/dot-rendered-page-state.model';
import { DotPageStateService } from '../../content/services/dot-page-state/dot-page-state.service';

@Component({
    selector: 'dot-edit-layout-advanced',
    templateUrl: './dot-edit-layout-advanced.component.html',
    styleUrls: ['./dot-edit-layout-advanced.component.scss']
})
export class DotEditLayoutAdvancedComponent implements OnInit {
    @Input()
    pageState: DotRenderedPageState;

    url: Observable<string>;

    constructor(
        private dotMenuService: DotMenuService,
        private ngZone: NgZone,
        private dotGlobalMessageService: DotGlobalMessageService,
        private dotMessageService: DotMessageService,
        private dotPageStateService: DotPageStateService
    ) {
        dotMessageService
            .getMessages(['dot.common.message.saved', 'dot.common.message.saving'])
            .subscribe();
    }

    ngOnInit() {
        this.url = this.dotMenuService.getDotMenuId('templates').pipe(
            map((id: string) => {
                // tslint:disable-next-line:max-line-length
                return `c/portal/layout?ng=true&p_l_id=${id}&p_p_id=templates&p_p_action=1&p_p_state=maximized&_templates_struts_action=%2Fext%2Ftemplates%2Fedit_template&_templates_cmd=edit&inode=${
                    this.pageState.template.inode
                }&r=0d618b02-f184-48fe-88f4-e98563ee6e9e`;
            })
        );
    }

    /**
     * Bind custom event from the iframe
     *
     * @param {any} $event
     * @memberof DotEditLayoutAdvancedComponent
     */
    onLoad($event): void {
        observableFromEvent($event.target.contentWindow.document, 'ng-event').subscribe(
            (event: CustomEvent) => {
                this.ngZone.run(() => {
                    if (event.detail.name === 'advanced-template-saved') {
                        this.dotGlobalMessageService.display(
                            this.dotMessageService.get('dot.common.message.saved')
                        );

                        this.dotPageStateService
                            .get(this.pageState.page.pageURI)
                            .subscribe(
                                (pageState: DotRenderedPageState) =>
                                    (this.pageState.dotRenderedPageState = pageState)
                            );
                    } else {
                        this.dotGlobalMessageService.loading(
                            this.dotMessageService.get('dot.common.message.saving')
                        );
                    }
                });
            }
        );
    }
}
