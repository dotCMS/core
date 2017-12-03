import { ActivatedRoute } from '@angular/router';
import { By } from '@angular/platform-browser';
import { ComponentFixture, async } from '@angular/core/testing';
import { DOTTestBed } from '../../../../../test/dot-test-bed';
import { DebugElement } from '@angular/core';
import { DotContentletService } from '../../../../../api/services/dot-contentlet.service';
import { DotMenuService } from '../../../../../api/services/dot-menu.service';
import { DotRouterService } from '../../../../../api/services/dot-router-service';
import { IFrameModule } from '../index';
import { IframePortletLegacyComponent } from './iframe-porlet-legacy.component';
import { Observable } from 'rxjs/Observable';
import { RouterTestingModule } from '@angular/router/testing';
import { SocketFactory, SiteService, LoginService } from 'dotcms-js/dotcms-js';

describe('IframePortletLegacyComponent', () => {
    let comp: IframePortletLegacyComponent;
    let fixture: ComponentFixture<IframePortletLegacyComponent>;
    let de: DebugElement;
    let el: HTMLElement;
    let route: ActivatedRoute;
    let dotMenuService: DotMenuService;

    beforeEach(
        async(() => {
            DOTTestBed.configureTestingModule({
                declarations: [],
                imports: [IFrameModule, RouterTestingModule],
                providers: [
                    DotContentletService,
                    DotMenuService,
                    DotRouterService,
                    LoginService,
                    SiteService,
                    SocketFactory,
                    {
                        provide: ActivatedRoute,
                        useValue: {
                            parent: {
                                url: Observable.of([{
                                    path: 'an-url'
                                }])
                            }
                        }
                    }
                ]
            });

            fixture = DOTTestBed.createComponent(IframePortletLegacyComponent);
            comp = fixture.componentInstance;
            de = fixture.debugElement;
            el = de.nativeElement;
            route = fixture.debugElement.injector.get(ActivatedRoute);
            dotMenuService = fixture.debugElement.injector.get(DotMenuService);
        })
    );

    it('should have dot-iframe component', () => {
        expect(fixture.debugElement.query(By.css('dot-iframe'))).toBeDefined();
    });

    it('should set query param url to the dot-iframe src', () => {
        route.params = Observable.of({ id: 'portlet-id' });
        route.queryParams = Observable.of({ url: 'hello/world' });

        let src: string;
        comp.url.subscribe(url => {
            src = url;
        });

        fixture.detectChanges();

        expect(src).toEqual('hello/world');
    });

    it('should set router param id to the dot-iframe src', () => {
        route.queryParams = Observable.of({});
        route.params = Observable.of({ id: 'portlet-id' });

        spyOn(dotMenuService, 'getUrlById').and.returnValue(Observable.of('fake-url'));

        let src: string;

        comp.url.subscribe(url => {
            src = url;
        });

        fixture.detectChanges();

        expect(dotMenuService.getUrlById).toHaveBeenCalledWith('portlet-id');
        expect(src).toEqual('fake-url');
    });
});
