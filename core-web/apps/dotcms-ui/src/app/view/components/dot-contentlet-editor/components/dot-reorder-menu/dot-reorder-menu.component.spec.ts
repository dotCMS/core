import { of, Subject } from 'rxjs';

import { DebugElement } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { RouterTestingModule } from '@angular/router/testing';

import {
    DotMessageService,
    DotIframeService,
    DotRouterService,
    DotUiColorsService,
    DotLoadingIndicatorService
} from '@dotcms/data-access';
import { LoginService, LoggerService, StringUtils, DotcmsEventsService } from '@dotcms/dotcms-js';
import { DotMessagePipe } from '@dotcms/ui';
import {
    LoginServiceMock,
    MockDotMessageService,
    MockDotRouterService,
    MockDotUiColorsService
} from '@dotcms/utils-testing';

import { DotReorderMenuComponent } from './dot-reorder-menu.component';

import { IframeOverlayService } from '../../../_common/iframe/service/iframe-overlay.service';
import { DotIframeDialogComponent } from '../../../dot-iframe-dialog/dot-iframe-dialog.component';

describe('DotReorderMenuComponent', () => {
    let component: DotReorderMenuComponent;
    let de: DebugElement;
    let fixture: ComponentFixture<DotReorderMenuComponent>;

    beforeEach(() => {
        const messageServiceMock = new MockDotMessageService({
            'editpage.content.contentlet.menu.reorder.title': 'Menu order Title'
        });

        TestBed.configureTestingModule({
            imports: [
                DotReorderMenuComponent,
                DotIframeDialogComponent,
                BrowserAnimationsModule,
                RouterTestingModule,
                DotMessagePipe
            ],
            providers: [
                {
                    provide: LoginService,
                    useClass: LoginServiceMock
                },
                {
                    provide: DotMessageService,
                    useValue: messageServiceMock
                },
                {
                    provide: DotIframeService,
                    useValue: {
                        get: jest.fn().mockReturnValue(of({})),
                        post: jest.fn().mockReturnValue(of({})),
                        reloaded: jest.fn().mockReturnValue(of({})),
                        ran: jest.fn().mockReturnValue(of({})),
                        reloadedColors: jest.fn().mockReturnValue(of({})),
                        run: jest.fn().mockReturnValue(of({}))
                    }
                },
                { provide: DotRouterService, useClass: MockDotRouterService },
                { provide: DotUiColorsService, useClass: MockDotUiColorsService },
                { provide: DotLoadingIndicatorService, useValue: {} },
                {
                    provide: IframeOverlayService,
                    useValue: {
                        overlay: new Subject<boolean>()
                    }
                },
                {
                    provide: DotcmsEventsService,
                    useValue: {
                        subscribeToEvents: jest.fn().mockReturnValue(of({})),
                        subscribeTo: jest.fn().mockReturnValue(of({}))
                    }
                },
                { provide: LoggerService, useValue: { debug: jest.fn() } },
                { provide: StringUtils, useValue: { to: jest.fn() } }
            ]
        });
    });

    beforeEach(() => {
        fixture = TestBed.createComponent(DotReorderMenuComponent);
        de = fixture.debugElement;
        component = de.componentInstance;
        component.url = 'test';
    });

    describe('default', () => {
        it('should have right params', () => {
            fixture.detectChanges();
            const dotIframeDialogElement = de.query(By.css('dot-iframe-dialog')).componentInstance;
            expect(dotIframeDialogElement).not.toBe(null);
            expect(dotIframeDialogElement.header).toBe('Menu order Title');
            expect(dotIframeDialogElement.url).toBe('test');
        });

        it('should emit shutdown', () => {
            jest.spyOn(component.shutdown, 'emit');
            fixture.detectChanges();
            const dotIframeDialogElement = de.query(By.css('dot-iframe-dialog')).componentInstance;
            dotIframeDialogElement.shutdown.emit();
            expect(component.shutdown.emit).toHaveBeenCalledTimes(1);
        });
    });
});
