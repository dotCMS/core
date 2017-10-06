import { By } from '@angular/platform-browser';
import { DebugElement } from '@angular/core';
import { RouterTestingModule } from '@angular/router/testing';
import { TestBed, async } from '@angular/core/testing';

import { AppComponent } from './app.component';
import { DOTTestBed } from './test/dot-test-bed';
import { NotLicensedService } from './api/services/not-licensed-service';
import { DotMenuService } from './api/services/dot-menu.service';
import { LoginService, SocketFactory } from 'dotcms-js/dotcms-js';
import { DotRouterService } from './api/services/dot-router-service';
import { MockMessageService } from './test/message-service.mock';
import { DotConfirmationService } from './api/services/dot-confirmation';

describe('AppComponent', () => {
    const messageServiceMock = new MockMessageService({
        'contenttypes.action.yes': 'Yes',
        'contenttypes.action.no': 'No'
    });

    beforeEach(() => {
        DOTTestBed.configureTestingModule({
            declarations: [AppComponent],
            imports: [RouterTestingModule.withRoutes([{ path: 'test', component: AppComponent }])],
            providers: [
                DotRouterService,
                LoginService,
                NotLicensedService,
                DotMenuService,
                SocketFactory,
                DotConfirmationService
            ]
        });
    });

    it(
        'should have router-outlet',
        async(() => {
            const fixture = TestBed.createComponent(AppComponent);
            const app = fixture.debugElement.componentInstance;
            const de: DebugElement = fixture.debugElement.query(By.css('router-outlet'));
            expect(de).not.toBeNull();
        })
    );
});
