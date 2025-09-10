/* eslint-disable @typescript-eslint/no-explicit-any */

import { Observable, of } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { Component, DebugElement, Input } from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { ActivatedRoute } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';

import { ConfirmationService } from 'primeng/api';

import {
    DotAlertConfirmService,
    DotEventsService,
    DotFormatDateService,
    DotIframeService,
    DotRouterService
} from '@dotcms/data-access';
import { CoreWebService, LoginService } from '@dotcms/dotcms-js';
import { CoreWebServiceMock, LoginServiceMock, MockDotRouterService } from '@dotcms/utils-testing';

import { DotCreateContentletComponent } from './dot-create-contentlet.component';

import { DotContentletEditorService } from '../../services/dot-contentlet-editor.service';
import { DotContentletWrapperComponent } from '../dot-contentlet-wrapper/dot-contentlet-wrapper.component';

class DotContentletEditorServiceMock {
    get createUrl$(): Observable<any> {
        return of(undefined);
    }
}

@Component({
    selector: 'dot-iframe-dialog',
    template: ``,
    standalone: false
})
class DotIframeMockComponent {
    @Input() url;
    @Input() header;
}

describe('DotCreateContentletComponent', () => {
    let de: DebugElement;
    let fixture: ComponentFixture<DotCreateContentletComponent>;
    let dotCreateContentletWrapper: DebugElement;
    let dotCreateContentletWrapperComponent: DotContentletWrapperComponent;
    let component: DotCreateContentletComponent;
    let routeService: ActivatedRoute;
    let dotIframeService: DotIframeService;
    let routerService;
    const dotContentletEditorServiceMock: DotContentletEditorServiceMock =
        new DotContentletEditorServiceMock();

    beforeEach(waitForAsync(() => {
        TestBed.configureTestingModule({
            imports: [RouterTestingModule, HttpClientTestingModule],
            declarations: [
                DotCreateContentletComponent,
                DotContentletWrapperComponent,
                DotIframeMockComponent
            ],
            providers: [
                DotIframeService,
                DotEventsService,
                DotFormatDateService,
                DotAlertConfirmService,
                ConfirmationService,
                {
                    provide: DotContentletEditorService,
                    useValue: dotContentletEditorServiceMock
                },
                {
                    provide: LoginService,
                    useClass: LoginServiceMock
                },
                {
                    provide: DotRouterService,
                    useClass: MockDotRouterService
                },
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                {
                    provide: ActivatedRoute,
                    useValue: {
                        get data() {
                            return of({ url: undefined });
                        }
                    }
                }
            ]
        });
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(DotCreateContentletComponent);
        de = fixture.debugElement;
        component = de.componentInstance;

        dotCreateContentletWrapper = de.query(By.css('dot-contentlet-wrapper'));
        dotCreateContentletWrapperComponent = dotCreateContentletWrapper.componentInstance;
        routeService = TestBed.inject(ActivatedRoute);
        routerService = TestBed.inject(DotRouterService);
        dotIframeService = TestBed.inject(DotIframeService);
        jest.spyOn(component.shutdown, 'emit');
        jest.spyOn(component.custom, 'emit');
        jest.spyOn(dotIframeService, 'reloadData');
    });

    it('should have dot-contentlet-wrapper', () => {
        expect(dotCreateContentletWrapper).toBeTruthy();
    });

    it('should emit shutdown and redirect to Content page when coming from starter', () => {
        routerService.currentSavedURL = '/c/content/new/';
        component.onClose({});
        expect(component.shutdown.emit).toHaveBeenCalledTimes(1);
        expect(routerService.goToContent).toHaveBeenCalledTimes(1);
        expect(dotIframeService.reloadData).toHaveBeenCalledWith('123-567');
        expect(dotIframeService.reloadData).toHaveBeenCalledTimes(1);
    });

    it('should emit shutdown and redirect to Pages page when shutdown from pages', () => {
        routerService.currentSavedURL = '/pages/new/';
        component.onClose({});
        expect(component.shutdown.emit).toHaveBeenCalledTimes(1);
        expect(routerService.gotoPortlet).toHaveBeenCalledTimes(1);
        expect(dotIframeService.reloadData).toHaveBeenCalledWith('123-567');
        expect(dotIframeService.reloadData).toHaveBeenCalledTimes(1);
    });

    it('should emit custom', () => {
        dotCreateContentletWrapper.triggerEventHandler('custom', {});
        expect(component.custom.emit).toHaveBeenCalledTimes(1);
    });

    it('should have url in null', () => {
        expect(dotCreateContentletWrapperComponent.url).toEqual(undefined);
    });

    it('should set url from service', () => {
        jest.spyOn(dotContentletEditorServiceMock, 'createUrl$', 'get', 'get').mockReturnValue(
            of('hello.world.com')
        );
        fixture.detectChanges();
        expect(dotCreateContentletWrapperComponent.url).toEqual('hello.world.com');
    });

    it('should set url from resolver', () => {
        Object.defineProperty(routeService, 'data', {
            get: jest.fn().mockReturnValue(of({ url: 'url.from.resolver' })),
            configurable: true
        });
        fixture.detectChanges();
        expect(dotCreateContentletWrapperComponent.url).toEqual('url.from.resolver');
    });
});
