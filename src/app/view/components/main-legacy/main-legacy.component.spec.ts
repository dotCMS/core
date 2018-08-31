import { ComponentFixture, async } from '@angular/core/testing';
import { DOTTestBed } from '../../../test/dot-test-bed';
import { DebugElement, Component, Input } from '@angular/core';
import { MainComponentLegacyComponent } from './main-legacy.component';
import { RouterTestingModule } from '@angular/router/testing';
import { LoginService } from 'dotcms-js/dotcms-js';
import { LoginServiceMock } from '../../../test/login-service.mock';
import { By } from '@angular/platform-browser';
import { DotIframeService } from '../_common/iframe/service/dot-iframe/dot-iframe.service';
import { DotRouterService } from '../../../api/services/dot-router/dot-router.service';
import { DotContentletEditorModule } from '../dot-contentlet-editor/dot-contentlet-editor.module';
import { DotMenuService } from '../../../api/services/dot-menu.service';

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

describe('MainComponentLegacyComponent', () => {
    let component: MainComponentLegacyComponent;
    let fixture: ComponentFixture<MainComponentLegacyComponent>;
    let de: DebugElement;
    let dotRouterService: DotRouterService;
    let dotIframeService: DotIframeService;

    beforeEach(async(() => {
        DOTTestBed.configureTestingModule({
            imports: [RouterTestingModule, DotContentletEditorModule],
            providers: [
                {
                    provide: LoginService,
                    useClass: LoginServiceMock
                },
                DotMenuService
            ],
            declarations: [
                MainComponentLegacyComponent,
                MockDotDialogComponent,
                MockDotMainNavComponent,
                MockDotToolbarComponent
            ]
        }).compileComponents();
    }));

    beforeEach(() => {
        fixture = DOTTestBed.createComponent(MainComponentLegacyComponent);
        component = fixture.componentInstance;
        de = fixture.debugElement;
        dotRouterService = de.injector.get(DotRouterService);
        dotIframeService = de.injector.get(DotIframeService);
        spyOn(dotIframeService, 'reloadData');
        fixture.detectChanges();
    });

    it('should have basic layout elements', () => {
        expect(de.query(By.css('dot-alert-confirm')) !== null).toBe(true);
        expect(de.query(By.css('dot-toolbar')) !== null).toBe(true);
        expect(de.query(By.css('dot-main-nav')) !== null).toBe(true);
        expect(de.query(By.css('router-outlet')) !== null).toBe(true);
    });

    describe('Create Contentlet', () => {
        it('should refresh the current portlet data', () => {
            spyOnProperty(dotRouterService, 'currentPortlet', 'get').and.returnValue({
                id: 'site-browser'
            });
            const createContentlet: DebugElement = de.query(By.css('dot-create-contentlet'));
            createContentlet.triggerEventHandler('close', {});

            expect(dotIframeService.reloadData).toHaveBeenCalledWith('site-browser');
        });
    });
});
