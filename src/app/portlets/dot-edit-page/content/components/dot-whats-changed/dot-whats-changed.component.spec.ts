/* tslint:disable:no-unused-variable */
import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { DebugElement, Input, Component } from '@angular/core';

import { DotWhatsChangedComponent } from './dot-whats-changed.component';
import { LoginService } from 'dotcms-js';
import { LoginServiceMock } from '../../../../../test/login-service.mock';
import { IframeComponent } from '@components/_common/iframe/iframe-component';

@Component({
    selector: 'dot-test',
    template: '<dot-whats-changed [pageId]="pageId" [languageId]="languageId"></dot-whats-changed>'
})
class TestHostComponent {
    languageId: string;
    pageId: string;
}

@Component({
    selector: 'dot-iframe',
    template: ''
})
class TestDotIframeComponent {
    @Input()
    src: string;
}

describe('DotWhatsChangedComponent', () => {
    let component: DotWhatsChangedComponent;
    let fixture: ComponentFixture<TestHostComponent>;
    let de: DebugElement;
    let dotIframe: IframeComponent;

    beforeEach(async(() => {
        TestBed.configureTestingModule({
            declarations: [DotWhatsChangedComponent, TestDotIframeComponent, TestHostComponent],
            providers: [
                {
                    provide: LoginService,
                    useClass: LoginServiceMock
                }
            ]
        }).compileComponents();
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(TestHostComponent);

        de = fixture.debugElement.query(By.css('dot-whats-changed'));
        component = de.componentInstance;

        fixture.componentInstance.pageId = '123';
        fixture.componentInstance.languageId = '321';
        dotIframe = de.query(By.css('dot-iframe')).componentInstance;
        fixture.detectChanges();
    });

    it('should have dot-iframe', () => {
        expect(dotIframe).toBeTruthy();
    });

    it('should set url based on the page id', () => {
        expect(dotIframe.src).toEqual(
            `/html/portlet/ext/htmlpages/view_live_working_diff.jsp?id=${
                component.pageId
            }&pageLang=${component.languageId}`
        );
    });

    it('should reset url when languageId is change', () => {
        fixture.componentInstance.languageId = '123';
        fixture.detectChanges();

        expect(dotIframe.src).toEqual(
            `/html/portlet/ext/htmlpages/view_live_working_diff.jsp?id=123&pageLang=123`
        );
    });

    it('should reset url when pageId is change', () => {
        fixture.componentInstance.pageId = '321';
        fixture.detectChanges();

        expect(dotIframe.src).toEqual(
            `/html/portlet/ext/htmlpages/view_live_working_diff.jsp?id=321&pageLang=321`
        );
    });
});
