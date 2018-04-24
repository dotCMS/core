/* tslint:disable:no-unused-variable */
import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { DebugElement, Input, Component } from '@angular/core';

import { DOTTestBed } from '../../../../../test/dot-test-bed';
import { DotWhatsChangedComponent } from './dot-whats-changed.component';
import { LoginService } from 'dotcms-js/dotcms-js';
import { LoginServiceMock } from '../../../../../test/login-service.mock';
import { IframeComponent } from '../../../../../view/components/_common/iframe/iframe-component';

@Component({
    selector: 'dot-iframe',
    template: ''
})
class TestDotIframeComponent {
    @Input() src: string;
}

describe('DotWhatsChangedComponent', () => {
    let component: DotWhatsChangedComponent;
    let fixture: ComponentFixture<DotWhatsChangedComponent>;
    let de: DebugElement;
    let dotIframe: IframeComponent;

    beforeEach(async(() => {
        DOTTestBed.configureTestingModule({
            declarations: [DotWhatsChangedComponent, TestDotIframeComponent],
            providers: [
                {
                    provide: LoginService,
                    useClass: LoginServiceMock
                }
            ]
        });
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(DotWhatsChangedComponent);
        component = fixture.componentInstance;
        de = fixture.debugElement;

        component.pageId = '123';
        dotIframe = de.query(By.css('dot-iframe')).componentInstance;
        fixture.detectChanges();
    });

    it('should have dot-iframe', () => {
        expect(dotIframe).toBeTruthy();
    });

    it('should set url based on the page id', () => {
        expect(dotIframe.src).toEqual('/html/portlet/ext/htmlpages/view_live_working_diff.jsp?id=123&pageLang=1');
    });
});
