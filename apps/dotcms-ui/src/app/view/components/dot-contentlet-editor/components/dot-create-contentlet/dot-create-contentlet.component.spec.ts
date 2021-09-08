import { By } from '@angular/platform-browser';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { Component, DebugElement, Input } from '@angular/core';
import { DotContentletEditorService } from '../../services/dot-contentlet-editor.service';
import { DotCreateContentletComponent } from './dot-create-contentlet.component';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { CoreWebService, LoginService } from '@dotcms/dotcms-js';
import { LoginServiceMock } from '../../../../../test/login-service.mock';
import { RouterTestingModule } from '@angular/router/testing';
import { MockDotRouterService } from '@tests/dot-router-service.mock';
import { CoreWebServiceMock } from '@tests/core-web.service.mock';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { Observable, of } from 'rxjs';
import { DotFormatDateService } from '@services/dot-format-date-service';
import { DotIframeService } from '@components/_common/iframe/service/dot-iframe/dot-iframe.service';
import { ActivatedRoute } from '@angular/router';
import { DotContentletWrapperComponent } from '../dot-contentlet-wrapper/dot-contentlet-wrapper.component';
import { DotAlertConfirmService } from '@services/dot-alert-confirm';
import { ConfirmationService } from 'primeng/api';

class DotContentletEditorServiceMock {
    get createUrl$(): Observable<any> {
        return of(undefined);
    }
}

@Component({
    selector: 'dot-iframe-dialog',
    template: ``
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
    const dotContentletEditorServiceMock: DotContentletEditorServiceMock = new DotContentletEditorServiceMock();

    beforeEach(
        waitForAsync(() => {
            TestBed.configureTestingModule({
                imports: [RouterTestingModule, HttpClientTestingModule],
                declarations: [
                    DotCreateContentletComponent,
                    DotContentletWrapperComponent,
                    DotIframeMockComponent
                ],
                providers: [
                    DotIframeService,
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
        })
    );

    beforeEach(() => {
        fixture = TestBed.createComponent(DotCreateContentletComponent);
        de = fixture.debugElement;
        component = de.componentInstance;

        dotCreateContentletWrapper = de.query(By.css('dot-contentlet-wrapper'));
        dotCreateContentletWrapperComponent = dotCreateContentletWrapper.componentInstance;
        routeService = TestBed.inject(ActivatedRoute);
        routerService = TestBed.inject(DotRouterService);
        dotIframeService = TestBed.inject(DotIframeService);
        spyOn(component.close, 'emit');
        spyOn(component.custom, 'emit');
        spyOn(dotIframeService, 'reloadData');
    });

    it('should have dot-contentlet-wrapper', () => {
        expect(dotCreateContentletWrapper).toBeTruthy();
    });

    it('should emit close and redirect to Content page when coming from starter', () => {
        routerService.currentSavedURL = '/c/content/new/';
        dotCreateContentletWrapper.triggerEventHandler('close', {});
        expect(component.close.emit).toHaveBeenCalledTimes(1);
        expect(routerService.goToContent).toHaveBeenCalledTimes(1);
        expect(dotIframeService.reloadData).toHaveBeenCalledWith('123-567');
    });

    it('should emit custom', () => {
        dotCreateContentletWrapper.triggerEventHandler('custom', {});
        expect(component.custom.emit).toHaveBeenCalledTimes(1);
    });

    it('should have url in null', () => {
        expect(dotCreateContentletWrapperComponent.url).toEqual(undefined);
    });

    it('should set url from service', () => {
        spyOnProperty(dotContentletEditorServiceMock, 'createUrl$', 'get').and.returnValue(
            of('hello.world.com')
        );
        fixture.detectChanges();
        expect(dotCreateContentletWrapperComponent.url).toEqual('hello.world.com');
    });

    it('should set url from resolver', () => {
        spyOnProperty<any>(routeService, 'data').and.returnValue(of({ url: 'url.from.resolver' }));
        fixture.detectChanges();
        expect(dotCreateContentletWrapperComponent.url).toEqual('url.from.resolver');
    });
});
