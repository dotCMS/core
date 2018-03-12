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

describe('DotEditPageMainComponent', () => {
    let component: DotEditPageMainComponent;
    let fixture: ComponentFixture<DotEditPageMainComponent>;
    let route: ActivatedRoute;

    const messageServiceMock = new MockDotMessageService({
        'editpage.toolbar.nav.content': 'Content',
        'editpage.toolbar.nav.layout': 'Layout'
    });

    const mockDotRenderedPageState: DotRenderedPageState = new DotRenderedPageState(mockDotRenderedPage, null, mockUser);

    beforeEach(
        async(() => {
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
                declarations: [DotEditPageMainComponent],
                providers: [
                    { provide: DotMessageService, useValue: messageServiceMock },
                    { provide: PageViewService, useClass: PageViewServiceMock },
                ]
            });
        })
    );

    beforeEach(() => {
        fixture = DOTTestBed.createComponent(DotEditPageMainComponent);
        route = fixture.debugElement.injector.get(ActivatedRoute);
        route.data = Observable.of({
            content: mockDotRenderedPageState
        });
        component = fixture.componentInstance;
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
});
