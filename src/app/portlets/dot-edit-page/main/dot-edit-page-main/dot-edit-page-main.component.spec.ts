import { mockUser } from './../../../../test/login-service.mock';
import { mockDotRenderedPage } from './../../../../test/dot-rendered-page.mock';
import { PageViewService } from '../../../../api/services/page-view/page-view.service';
import { async, ComponentFixture } from '@angular/core/testing';

import { DotEditPageMainComponent } from './dot-edit-page-main.component';
import { DotEditPageNavModule } from '../dot-edit-page-nav/dot-edit-page-nav.module';
import { RouterTestingModule } from '@angular/router/testing';
import { By } from '@angular/platform-browser';
import { MockDotMessageService } from '../../../../test/dot-message-service.mock';
import { DotMessageService } from '../../../../api/services/dot-messages-service';
import { ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs/Observable';
import { DOTTestBed } from '../../../../test/dot-test-bed';
import { DotEditPageNavComponent } from '../dot-edit-page-nav/dot-edit-page-nav.component';
import { PageViewServiceMock } from '../../../../test/page-view.mock';
import { DotRenderedPageState } from '../../shared/models/dot-rendered-page-state.model';
import { DotContentletEditorService } from '../../../../view/components/dot-contentlet-editor/services/dot-contentlet-editor.service';
import { Injectable, Component, Output, EventEmitter } from '@angular/core';
import { DotPageStateService } from '../../content/services/dot-page-state/dot-page-state.service';
import { Subject } from 'rxjs/Subject';
import { DotRouterService } from '../../../../api/services/dot-router/dot-router.service';

@Injectable()
class MockDotContentletEditorService {
    close$ = new Subject;
}

@Injectable()
class MockDotPageStateService {
    reload$ = new Subject;
    reload(): void {
        this.reload$.next(new DotRenderedPageState(mockUser, mockDotRenderedPage));
    }
}

@Component({
    selector: 'dot-edit-contentlet',
    template: ''
})
class MockDotEditContentletComponent {
    @Output() custom = new EventEmitter<any>();
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

    const mockDotRenderedPageState: DotRenderedPageState = new DotRenderedPageState(mockUser, mockDotRenderedPage);

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
                { provide: PageViewService, useClass: PageViewServiceMock },
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
        route.data = Observable.of({
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
        const nav: DotEditPageNavComponent = fixture.debugElement.query(By.css('dot-edit-page-nav')).componentInstance;
        expect(nav.pageState).toEqual(mockDotRenderedPageState);
    });

    it('should reload page when url attribute in dialog has been changed', () => {
        spyOn(dotRouterService, 'goToEditPage');
        let editContentlet: MockDotEditContentletComponent;
        const mockMessage = {
            detail: {
                name: 'save-page',
                payload: {
                    htmlPageReferer: '/about-us/index2?com.dotmarketing.htmlpage.language=1&host_id=48190c8c-42c4-46af-8d1a-0cd5db894797'
                }
            }
        };

        editContentlet = fixture.debugElement.query(By.css('dot-edit-contentlet')).componentInstance;
        editContentlet.custom.emit(mockMessage);
        dotContentletEditorService.close$.next(true);
        expect(dotRouterService.goToEditPage).toHaveBeenCalledWith('/about-us/index2');
    });

    it('should call reload pageSte when IframeClose evt happens', () => {
        spyOn(component, 'pageState');
        spyOn(dotPageStateService, 'reload').and.callThrough();
        dotContentletEditorService.close$.next(true);
        expect(dotPageStateService.reload).toHaveBeenCalledWith('/about-us/index');
        expect(component.pageState).toEqual(Observable.of(new DotRenderedPageState(mockUser, mockDotRenderedPage)));
    });
});
