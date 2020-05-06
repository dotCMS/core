import { ComponentFixture, async } from '@angular/core/testing';
import { DOTTestBed } from '../../../test/dot-test-bed';
import { DebugElement, Component, Input } from '@angular/core';
import { MainComponentLegacyComponent } from './main-legacy.component';
import { RouterTestingModule } from '@angular/router/testing';
import { LoginService } from 'dotcms-js';
import { LoginServiceMock } from '../../../test/login-service.mock';
import { By } from '@angular/platform-browser';
import { DotIframeService } from '../_common/iframe/service/dot-iframe/dot-iframe.service';
import { DotContentletEditorModule } from '../dot-contentlet-editor/dot-contentlet-editor.module';
import { DotMenuService } from '@services/dot-menu.service';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { DotCustomEventHandlerService } from '@services/dot-custom-event-handler/dot-custom-event-handler.service';

@Component({
    selector: 'dot-alert-confirm',
    template: ''
})
class MockDotDialogComponent {}

@Component({
    selector: 'dot-toolbar',
    template: ''
})
class MockDotToolbarComponent {
    @Input() collapsed: boolean;
}

@Component({
    selector: 'dot-main-nav',
    template: ''
})
class MockDotMainNavComponent {
    @Input() collapsed: boolean;
}

@Component({
    selector: 'dot-message-display',
    template: ''
})
class MockDotMessageDisplayComponent {}

@Component({
    selector: 'dot-large-message-display',
    template: ''
})
class MockDotLargeMessageDisplayComponent {}

@Component({
    selector: 'dot-push-publish-dialog',
    template: ''
})
class MockDotPushPublishDialogComponent {}

describe('MainComponentLegacyComponent', () => {
    let fixture: ComponentFixture<MainComponentLegacyComponent>;
    let de: DebugElement;
    let dotIframeService: DotIframeService;
    let dotRouterService: DotRouterService;
    let dotCustomEventHandlerService: DotCustomEventHandlerService;

    beforeEach(
        async(() => {
            DOTTestBed.configureTestingModule({
                imports: [RouterTestingModule, DotContentletEditorModule],
                providers: [
                    {
                        provide: LoginService,
                        useClass: LoginServiceMock
                    },
                    DotMenuService,
                    DotCustomEventHandlerService
                ],
                declarations: [
                    MainComponentLegacyComponent,
                    MockDotDialogComponent,
                    MockDotMainNavComponent,
                    MockDotToolbarComponent,
                    MockDotMessageDisplayComponent,
                    MockDotLargeMessageDisplayComponent,
                    MockDotPushPublishDialogComponent
                ]
            }).compileComponents();
        })
    );

    beforeEach(() => {
        fixture = DOTTestBed.createComponent(MainComponentLegacyComponent);
        de = fixture.debugElement;
        dotIframeService = de.injector.get(DotIframeService);
        dotRouterService = de.injector.get(DotRouterService);
        dotCustomEventHandlerService = de.injector.get(DotCustomEventHandlerService);
        spyOn(dotIframeService, 'reloadData');
        fixture.detectChanges();
    });

    it('should have basic layout elements', () => {
        expect(de.query(By.css('dot-alert-confirm')) !== null).toBe(true);
        expect(de.query(By.css('dot-toolbar')) !== null).toBe(true);
        expect(de.query(By.css('dot-main-nav')) !== null).toBe(true);
        expect(de.query(By.css('router-outlet')) !== null).toBe(true);
        expect(de.query(By.css('dot-push-publish-dialog')) !== null).toBe(true);
    });

    it('should have messages components', () => {
        expect(de.query(By.css('dot-large-message-display')) !== null).toBe(true);
        expect(de.query(By.css('dot-large-message-display')) !== null).toBe(true);
    });

    describe('Create Contentlet', () => {
        let createContentlet: DebugElement;
        beforeEach(() => {
            createContentlet = de.query(By.css('dot-create-contentlet'));
        });

        it('should refresh the current portlet data', () => {
            spyOnProperty(dotRouterService, 'currentPortlet', 'get').and.returnValue({
                id: 'site-browser'
            });

            createContentlet.triggerEventHandler('close', {});

            expect(dotIframeService.reloadData).toHaveBeenCalledWith('site-browser');
        });

        it('should call dotCustomEventHandlerService on customEvent', () => {
            spyOn(dotCustomEventHandlerService, 'handle');
            createContentlet.triggerEventHandler('custom', { data: 'test' });

            expect(dotCustomEventHandlerService.handle).toHaveBeenCalledWith({ data: 'test' });
        });
    });
});
