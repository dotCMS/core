import { of as observableOf } from 'rxjs';

import { Component, Input } from '@angular/core';
import { ComponentFixture } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';

import { DotLegacyTemplateAdditionalActionsComponent } from './dot-legacy-template-additional-actions-iframe.component';

import { DotMenuService } from '../../../../../../api/services/dot-menu.service';
import { DOTTestBed } from '../../../../../../test/dot-test-bed';

@Component({
    selector: 'dot-iframe',
    template: '',
    standalone: false
})
class MockDotIframeComponent {
    @Input()
    src: string;
}

describe('DotLegacyAdditionalActionsComponent', () => {
    let component: DotLegacyTemplateAdditionalActionsComponent;
    let fixture: ComponentFixture<DotLegacyTemplateAdditionalActionsComponent>;

    beforeEach(() => {
        DOTTestBed.configureTestingModule({
            declarations: [DotLegacyTemplateAdditionalActionsComponent, MockDotIframeComponent],
            providers: [
                DotMenuService,
                {
                    provide: ActivatedRoute,
                    useValue: {
                        params: observableOf({ id: '1', tabName: 'properties' })
                    }
                }
            ]
        });

        fixture = DOTTestBed.createComponent(DotLegacyTemplateAdditionalActionsComponent);
        component = fixture.componentInstance;
    });

    it('should set additionalPropertiesURL right', () => {
        let urlResult;
        const dotMenuService: DotMenuService = fixture.debugElement.injector.get(DotMenuService);
        jest.spyOn(dotMenuService, 'getDotMenuId').mockReturnValue(observableOf('2'));

        fixture.detectChanges();

        component.url.subscribe((url) => (urlResult = url));
        expect(dotMenuService.getDotMenuId).toHaveBeenCalledWith('templates');
        expect(urlResult).toEqual(
            // tslint:disable-next-line:max-line-length
            `c/portal/layout?p_l_id=2&p_p_id=templates&p_p_action=1&p_p_state=maximized&p_p_mode=view&_templates_struts_action=%2Fext%2Ftemplates%2Fedit_template&_templates_cmd=edit&inode=1&drawed=false&selectedTab=properties`
        );
    });
});
