/* eslint-disable @typescript-eslint/no-explicit-any */

import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';
import { Observable, of } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { Component, Input } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { ConfirmationService } from 'primeng/api';

import { DotCustomEventHandlerService } from '@dotcms/app/api/services/dot-custom-event-handler/dot-custom-event-handler.service';
import {
    DotAlertConfirmService,
    DotEventsService,
    DotFormatDateService,
    DotIframeService,
    DotRouterService,
    DotUiColorsService
} from '@dotcms/data-access';
import {
    CoreWebService,
    DotcmsEventsService,
    LoggerService,
    LoginService,
    StringUtils
} from '@dotcms/dotcms-js';
import {
    CoreWebServiceMock,
    DotcmsEventsServiceMock,
    LoginServiceMock,
    MockDotRouterService
} from '@dotcms/utils-testing';

import { DotCreateContentletComponent } from './dot-create-contentlet.component';

import { IframeOverlayService } from '../../../_common/iframe/service/iframe-overlay.service';
import { DotContentletEditorService } from '../../services/dot-contentlet-editor.service';
import { DotContentletWrapperComponent } from '../dot-contentlet-wrapper/dot-contentlet-wrapper.component';

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
    let spectator: Spectator<DotCreateContentletComponent>;
    let dotIframeService: DotIframeService;
    let routerService: DotRouterService;
    let routeService: ActivatedRoute;
    const dotContentletEditorServiceMock: DotContentletEditorServiceMock =
        new DotContentletEditorServiceMock();

    const createComponent = createComponentFactory({
        component: DotCreateContentletComponent,
        imports: [HttpClientTestingModule, DotContentletWrapperComponent, DotIframeMockComponent],
        providers: [
            DotIframeService,
            DotEventsService,
            DotFormatDateService,
            DotAlertConfirmService,
            DotUiColorsService,
            IframeOverlayService,
            ConfirmationService,
            LoggerService,
            StringUtils,
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
            { provide: DotcmsEventsService, useClass: DotcmsEventsServiceMock },
            {
                provide: ActivatedRoute,
                useValue: {
                    get data() {
                        return of({ url: undefined });
                    },
                    snapshot: {
                        queryParams: {}
                    }
                }
            },
            {
                provide: DotCustomEventHandlerService,
                useValue: {
                    handle: jest.fn()
                }
            }
        ],
        detectChanges: false
    });

    beforeEach(() => {
        spectator = createComponent({
            detectChanges: false
        });
        routeService = spectator.inject(ActivatedRoute);
        routerService = spectator.inject(DotRouterService);
        dotIframeService = spectator.inject(DotIframeService);
        jest.spyOn(spectator.component.shutdown, 'emit');
        jest.spyOn(spectator.component.custom, 'emit');
        jest.spyOn(dotIframeService, 'reloadData');
    });

    it('should have dot-contentlet-wrapper', () => {
        spectator.detectChanges();
        const dotCreateContentletWrapper = spectator.query('dot-contentlet-wrapper');
        expect(dotCreateContentletWrapper).toBeTruthy();
    });

    it('should emit shutdown and redirect to Content page when coming from starter', () => {
        jest.spyOn(routerService, 'currentSavedURL', 'get').mockReturnValue('/c/content/new/');
        spectator.detectChanges();
        spectator.component.onClose({});
        expect(spectator.component.shutdown.emit).toHaveBeenCalledTimes(1);
        expect(routerService.goToContent).toHaveBeenCalledTimes(1);
        expect(dotIframeService.reloadData).toHaveBeenCalledWith('123-567');
        expect(dotIframeService.reloadData).toHaveBeenCalledTimes(1);
    });

    it('should emit shutdown and redirect to Pages page when shutdown from pages', () => {
        jest.spyOn(routerService, 'currentSavedURL', 'get').mockReturnValue('/pages/new/');
        spectator.detectChanges();
        spectator.component.onClose({});
        expect(spectator.component.shutdown.emit).toHaveBeenCalledTimes(1);
        expect(routerService.gotoPortlet).toHaveBeenCalledTimes(1);
        expect(dotIframeService.reloadData).toHaveBeenCalledWith('123-567');
        expect(dotIframeService.reloadData).toHaveBeenCalledTimes(1);
    });

    it('should emit custom', () => {
        spectator.detectChanges();
        spectator.triggerEventHandler('dot-contentlet-wrapper', 'custom', {});
        expect(spectator.component.custom.emit).toHaveBeenCalledTimes(1);
    });

    it('should have url in null', () => {
        spectator.detectChanges();
        const dotCreateContentletWrapperComponent = spectator.query(
            'dot-contentlet-wrapper'
        ) as unknown as DotContentletWrapperComponent;
        expect(dotCreateContentletWrapperComponent.url).toEqual(undefined);
    });

    it('should set url from service', (done) => {
        const dotContentletEditorService = spectator.inject(DotContentletEditorService);
        jest.spyOn(dotContentletEditorService, 'createUrl$', 'get').mockReturnValue(
            of('hello.world.com')
        );

        spectator.component.ngOnInit();

        spectator.component.url$.subscribe((url) => {
            expect(url).toEqual('hello.world.com');
            done();
        });
    });

    it('should set url from resolver', (done) => {
        const dotContentletEditorService = spectator.inject(DotContentletEditorService);
        // Reset the service mock to return undefined so the resolver value is used
        jest.spyOn(dotContentletEditorService, 'createUrl$', 'get').mockReturnValue(of(undefined));
        jest.spyOn(routeService, 'data', 'get').mockReturnValue(of({ url: 'url.from.resolver' }));

        spectator.component.ngOnInit();

        spectator.component.url$.subscribe((url) => {
            expect(url).toEqual('url.from.resolver');
            done();
        });
    });
});
