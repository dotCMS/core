import { ActivatedRoute } from '@angular/router';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { By } from '@angular/platform-browser';
import { ComponentFixture } from '@angular/core/testing';
import { DOTTestBed } from '../../../../test/dot-test-bed';
import { DebugElement } from '@angular/core/src/debug/debug_node';
import { DotConfirmationService } from '../../../../api/services/dot-confirmation';
import { DotEditLayoutComponent } from './dot-edit-layout.component';
import { DotEditLayoutGridModule } from '../dot-edit-layout-grid/dot-edit-layout-grid.module';
import { FormatDateService } from '../../../../api/services/format-date-service';
import { LoginService, SocketFactory } from 'dotcms-js/dotcms-js';
import { MessageService } from '../../../../api/services/messages-service';
import { MockMessageService } from '../../../../test/message-service.mock';
import { Observable } from 'rxjs/Observable';
import { PageViewService } from '../../../../api/services/page-view/page-view.service';
import { PaginatorService } from '../../../../api/services/paginator';
import { RouterTestingModule } from '@angular/router/testing';
import { DotEditLayoutService } from '../../shared/services/dot-edit-layout.service';

describe('DotEditLayoutComponent', () => {
    const fakePageView = {
        pageView: {
            page: {
                identifier: '123'
            },
            layout: {
                body: {
                    rows: []
                }
            }
        }
    };

    let component: DotEditLayoutComponent;
    let fixture: ComponentFixture<DotEditLayoutComponent>;

    const messageServiceMock = new MockMessageService({
        'editpage.confirm.header': '',
        'editpage.confirm.message.delete': '',
        'editpage.confirm.message.delete.warning': '',
        'editpage.action.cancel': '',
        'editpage.action.delete': '',
        'editpage.action.save': ''
    });

    beforeEach(() => {
        DOTTestBed.configureTestingModule({
            declarations: [DotEditLayoutComponent],
            imports: [DotEditLayoutGridModule, RouterTestingModule, BrowserAnimationsModule],
            providers: [
                DotConfirmationService,
                FormatDateService,
                LoginService,
                PageViewService,
                PaginatorService,
                SocketFactory,
                DotEditLayoutService,
                {
                    provide: ActivatedRoute,
                    useValue: {
                        data: Observable.of(fakePageView)
                    }
                },
                { provide: MessageService, useValue: messageServiceMock }
            ]
        });

        fixture = DOTTestBed.createComponent(DotEditLayoutComponent);
        component = fixture.componentInstance;

        fixture.detectChanges();
    });

    it('should have dot-edit-layout-grid', () => {
        const gridLayout: DebugElement = fixture.debugElement.query(By.css('dot-edit-layout-grid'));
        expect(gridLayout).toBeDefined();
    });
});
