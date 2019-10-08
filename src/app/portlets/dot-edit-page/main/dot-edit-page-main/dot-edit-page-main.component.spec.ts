import { of as observableOf, Subject } from 'rxjs';
import { mockUser } from './../../../../test/login-service.mock';
import { mockDotRenderedPage } from '../../../../test/dot-page-render.mock';
import { DotPageLayoutService } from '@services/dot-page-layout/dot-page-layout.service';
import { async, ComponentFixture } from '@angular/core/testing';

import { DotEditPageMainComponent } from './dot-edit-page-main.component';
import { DotEditPageNavModule } from '../dot-edit-page-nav/dot-edit-page-nav.module';
import { RouterTestingModule } from '@angular/router/testing';
import { By } from '@angular/platform-browser';
import { MockDotMessageService } from '../../../../test/dot-message-service.mock';
import { DotMessageService } from '@services/dot-messages-service';
import { ActivatedRoute } from '@angular/router';
import { DOTTestBed } from '../../../../test/dot-test-bed';
import { DotEditPageNavComponent } from '../dot-edit-page-nav/dot-edit-page-nav.component';
import { PageViewServiceMock } from '../../../../test/page-view.mock';
import { DotContentletEditorService } from '@components/dot-contentlet-editor/services/dot-contentlet-editor.service';
import { Injectable, Component, Output, EventEmitter } from '@angular/core';
import { DotPageStateService } from '../../content/services/dot-page-state/dot-page-state.service';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { DotPageRenderState } from '@portlets/dot-edit-page/shared/models/dot-rendered-page-state.model';
import { DotPageRender } from '@portlets/dot-edit-page/shared/models';

@Injectable()
class MockDotContentletEditorService {
    close$ = new Subject();
}

@Injectable()
class MockDotPageStateService {
    reload$ = new Subject();
    state$ = new Subject();
    get(): void {}
    reload(): void {
        this.reload$.next(new DotPageRenderState(mockUser, new DotPageRender(mockDotRenderedPage)));
    }
}

@Component({
    selector: 'dot-edit-contentlet',
    template: ''
})
class MockDotEditContentletComponent {
    @Output()
    custom = new EventEmitter<any>();
}

describe('DotEditPageMainComponent', () => {
    let component: DotEditPageMainComponent;
    let fixture: ComponentFixture<DotEditPageMainComponent>;
    let route: ActivatedRoute;
    let dotContentletEditorService: DotContentletEditorService;
    let dotPageStateService: DotPageStateService;
    let dotRouterService: DotRouterService;

    const messageServiceMock = new MockDotMessageService({
        'editpage.toolbar.nav.content': 'Content',
        'editpage.toolbar.nav.layout': 'Layout',
        'editpage.toolbar.nav.properties': 'Properties'
    });

    const mockDotRenderedPageState: DotPageRenderState = new DotPageRenderState(
        mockUser,
        new DotPageRender(mockDotRenderedPage)
    );

    beforeEach(async(() => {
        DOTTestBed.configureTestingModule({
            imports: [
                RouterTestingModule.withRoutes([
                    {
                        component: DotEditPageMainComponent,
                        path: ''
                    }
                ]),
                DotEditPageNavModule
            ],
            declarations: [DotEditPageMainComponent, MockDotEditContentletComponent],
            providers: [
                { provide: DotMessageService, useValue: messageServiceMock },
                {
                    provide: ActivatedRoute,
                    useValue: {
                        snapshot: {
                            queryParams: {
                                url: '/about-us/index'
                            }
                        }
                    }
                },
                { provide: DotPageLayoutService, useClass: PageViewServiceMock },
                {
                    provide: DotContentletEditorService,
                    useClass: MockDotContentletEditorService
                },
                {
                    provide: DotPageStateService,
                    useClass: MockDotPageStateService
                }
            ]
        });
    }));

    beforeEach(() => {
        fixture = DOTTestBed.createComponent(DotEditPageMainComponent);
        component = fixture.debugElement.componentInstance;
        route = fixture.debugElement.injector.get(ActivatedRoute);
        route.data = observableOf({
            content: mockDotRenderedPageState
        });
        dotContentletEditorService = fixture.debugElement.injector.get(DotContentletEditorService);
        dotRouterService = fixture.debugElement.injector.get(DotRouterService);
        dotPageStateService = fixture.debugElement.injector.get(DotPageStateService);
        fixture.detectChanges();
    });

    it('should have router-outlet', () => {
        expect(fixture.debugElement.query(By.css('router-outlet'))).not.toBeNull();
    });

    it('should have dot-edit-page-nav', () => {
        expect(fixture.debugElement.query(By.css('dot-edit-page-nav'))).not.toBeNull();
    });

    it('should bind correctly pageState param', () => {
        const nav: DotEditPageNavComponent = fixture.debugElement.query(By.css('dot-edit-page-nav'))
            .componentInstance;
        expect(nav.pageState).toEqual(mockDotRenderedPageState);
    });

    it('should call reload pageSte when IframeClose evt happens', () => {
        spyOn(dotPageStateService, 'get').and.callThrough();

        component.pageState$.subscribe((res) => {
            expect(res).toEqual(new DotPageRenderState(mockUser, new DotPageRender(mockDotRenderedPage)));
        });

        dotContentletEditorService.close$.next(true);
        expect(dotPageStateService.get).toHaveBeenCalledWith({
            url: '/about-us/index',
            viewAs: {
                language: mockDotRenderedPage.page.languageId
            }
        });
    });

    describe('handle custom events from contentlet editor', () => {
        let editContentlet: MockDotEditContentletComponent;

        beforeEach(() => {
            editContentlet = fixture.debugElement.query(By.css('dot-edit-contentlet'))
                .componentInstance;
        });

        it('should reload page when url attribute in dialog has been changed', () => {
            editContentlet.custom.emit({
                detail: {
                    name: 'save-page',
                    payload: {
                        htmlPageReferer:
                            '/about-us/index2?com.dotmarketing.htmlpage.language=1&host_id=48190c8c-42c4-46af-8d1a-0cd5db894797'
                    }
                }
            });
            dotContentletEditorService.close$.next(true);
            expect(dotRouterService.goToEditPage).toHaveBeenCalledWith(
                '/about-us/index2',
                mockDotRenderedPage.page.languageId.toString()
            );
        });

        it('should go to site-browser when page is deleted', () => {
            editContentlet.custom.emit({
                detail: {
                    name: 'deleted-page'
                }
            });
            expect(dotRouterService.goToSiteBrowser).toHaveBeenCalledTimes(1);
        });
    });
});
