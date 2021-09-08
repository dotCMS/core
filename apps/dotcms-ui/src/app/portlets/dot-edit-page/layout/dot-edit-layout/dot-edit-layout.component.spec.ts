import { ActivatedRoute } from '@angular/router';
import { By } from '@angular/platform-browser';
import { Component, DebugElement, Input } from '@angular/core';
import { waitForAsync, ComponentFixture, TestBed } from '@angular/core/testing';

import { of, throwError } from 'rxjs';
import { HttpCode, ResponseView } from '@dotcms/dotcms-js';

import { DotEditLayoutComponent } from './dot-edit-layout.component';
import { mockDotRenderedPage, processedContainers } from '@tests/dot-page-render.mock';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { DotGlobalMessageService } from '@components/_common/dot-global-message/dot-global-message.service';
import { DotPageLayoutService } from '@services/dot-page-layout/dot-page-layout.service';
import { MockDotMessageService } from '@tests/dot-message-service.mock';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { DotTemplateContainersCacheService } from '@services/dot-template-containers-cache/dot-template-containers-cache.service';
import { DotLayout } from '@models/dot-edit-layout-designer';
import { CONTAINER_SOURCE } from '@models/container/dot-container.model';
import { DotPageRender } from '@models/dot-page/dot-rendered-page.model';
import { HttpResponse } from '@angular/common/http';
import { mockResponseView } from '@tests/response-view.mock';
import { DotEditLayoutService } from '@services/dot-edit-layout/dot-edit-layout.service';
import { DotHttpErrorManagerService } from '@services/dot-http-error-manager/dot-http-error-manager.service';

@Component({
    selector: 'dot-edit-layout-designer',
    template: ''
})
export class DotEditLayoutDesignerComponentMock {
    @Input()
    layout: DotLayout;

    @Input()
    title: string;

    @Input()
    theme: string;

    @Input()
    apiLink: string;

    @Input()
    url: string;
}

let fixture: ComponentFixture<DotEditLayoutComponent>;

const messageServiceMock = new MockDotMessageService({
    'dot.common.message.saving': 'Saving',
    'dot.common.message.saved': 'Saved'
});

describe('DotEditLayoutComponent', () => {
    let component: DotEditLayoutComponent;
    let layoutDesignerDe: DebugElement;
    let layoutDesigner: DotEditLayoutDesignerComponentMock;
    let dotPageLayoutService: DotPageLayoutService;
    let dotGlobalMessageService: DotGlobalMessageService;
    let dotTemplateContainersCacheService: DotTemplateContainersCacheService;
    let fakeLayout: DotLayout;
    let dotHttpErrorManagerService: DotHttpErrorManagerService;
    let dotEditLayoutService: DotEditLayoutService;

    beforeEach(
        waitForAsync(() => {
            TestBed.configureTestingModule({
                declarations: [DotEditLayoutDesignerComponentMock, DotEditLayoutComponent],
                providers: [
                    DotEditLayoutService,
                    {
                        provide: DotHttpErrorManagerService,
                        useValue: {
                            handle: jasmine.createSpy().and.returnValue(of({}))
                        }
                    },
                    {
                        provide: DotMessageService,
                        useValue: messageServiceMock
                    },
                    {
                        provide: DotGlobalMessageService,
                        useValue: {
                            loading: jasmine.createSpy(),
                            success: jasmine.createSpy(),
                            error: jasmine.createSpy()
                        }
                    },
                    {
                        provide: DotPageLayoutService,
                        useValue: {
                            save() {}
                        }
                    },
                    {
                        provide: DotTemplateContainersCacheService,
                        useValue: {
                            set: jasmine.createSpy()
                        }
                    },
                    {
                        provide: DotRouterService,
                        useValue: {
                            goToEditPage: jasmine.createSpy()
                        }
                    },
                    {
                        provide: ActivatedRoute,
                        useValue: {
                            parent: {
                                parent: {
                                    data: of({
                                        content: new DotPageRender(mockDotRenderedPage())
                                    })
                                }
                            }
                        }
                    }
                ]
            });
        })
    );

    beforeEach(() => {
        fixture = TestBed.createComponent(DotEditLayoutComponent);
        component = fixture.componentInstance;
        layoutDesignerDe = fixture.debugElement.query(By.css('dot-edit-layout-designer'));
        layoutDesigner = layoutDesignerDe.componentInstance;

        dotPageLayoutService = TestBed.inject(DotPageLayoutService);
        dotGlobalMessageService = TestBed.inject(DotGlobalMessageService);
        dotTemplateContainersCacheService = TestBed.inject(DotTemplateContainersCacheService);
        dotHttpErrorManagerService = TestBed.inject(DotHttpErrorManagerService);
        dotEditLayoutService = TestBed.inject(DotEditLayoutService);

        fakeLayout = {
            body: null,
            footer: false,
            header: true,
            sidebar: null,
            themeId: '123',
            title: 'Title',
            width: '100'
        };

        fixture.detectChanges();
    });

    it('should be 100% min-width in the host', () => {
        // https://github.com/dotCMS/core/issues/19540
        expect(fixture.debugElement.nativeElement.style.minWidth).toBe('100%');
    });

    it('should pass attr to the dot-edit-layout-designer', () => {
        const state = mockDotRenderedPage();
        expect(layoutDesigner.layout).toEqual(state.layout);
        expect(layoutDesigner.theme).toEqual(state.template.theme);
        expect(layoutDesigner.title).toEqual(state.page.title);
        expect(layoutDesigner.apiLink).toEqual('api/v1/page/render/an/url/test?language_id=1');
        expect(layoutDesigner.url).toEqual(state.page.pageURI);
    });

    describe('save', () => {
        it('should save the layout', () => {
            const res: DotPageRender = new DotPageRender(mockDotRenderedPage());
            spyOn(dotPageLayoutService, 'save').and.returnValue(of(res));

            layoutDesignerDe.triggerEventHandler('save', fakeLayout);

            expect(dotGlobalMessageService.loading).toHaveBeenCalledWith('Saving');
            expect(dotGlobalMessageService.success).toHaveBeenCalledWith('Saved');
            expect(dotGlobalMessageService.error).not.toHaveBeenCalled();

            expect(dotPageLayoutService.save).toHaveBeenCalledWith('123', {
                ...fakeLayout,
                title: null
            });
            expect(dotTemplateContainersCacheService.set).toHaveBeenCalledWith({
                '/default/': processedContainers[0].container,
                '/banner/': processedContainers[1].container
            });
            expect(component.pageState).toEqual(new DotPageRender(mockDotRenderedPage()));
        });

        it('should handle error when save fail', (done) => {
            spyOn(dotPageLayoutService, 'save').and.returnValue(
                throwError(
                    new ResponseView(new HttpResponse(mockResponseView(HttpCode.BAD_REQUEST)))
                )
            );

            layoutDesignerDe.triggerEventHandler('save', fakeLayout);
            expect(dotGlobalMessageService.error).toHaveBeenCalledWith('Unknown Error');
            expect(dotHttpErrorManagerService.handle).toHaveBeenCalledTimes(1);
            dotEditLayoutService.canBeDesactivated$.subscribe((resp) => {
                expect(resp).toBeTruthy();
                done();
            });
        });
    });
});
