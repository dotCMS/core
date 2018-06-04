import { ComponentFixture, async } from '@angular/core/testing';
import { DOTTestBed } from '../../../test/dot-test-bed';
import { DebugElement, Component, EventEmitter, Output, Input } from '@angular/core';
import { MainComponentLegacyComponent } from './main-legacy.component';
import { RouterTestingModule } from '@angular/router/testing';
import { LoginService } from 'dotcms-js/dotcms-js';
import { LoginServiceMock } from '../../../test/login-service.mock';
import { By } from '@angular/platform-browser';
import { DotRouterService } from '../../../api/services/dot-router/dot-router.service';

@Component({
    selector: 'dot-dialog',
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
    selector: 'dot-contentlet-editor',
    template: ''
})
class MockDotContentletEditorComponent {
    @Output() close: EventEmitter<any> = new EventEmitter();
}

@Component({
    selector: 'dot-main-nav',
    template: ''
})
class MockDotMainNavComponent {}

describe('MainComponentLegacyComponent', () => {
    let component: MainComponentLegacyComponent;
    let fixture: ComponentFixture<MainComponentLegacyComponent>;
    let de: DebugElement;
    let dotRouterService: DotRouterService;

    beforeEach(async(() => {
        DOTTestBed.configureTestingModule({
            imports: [RouterTestingModule],
            providers: [
                {
                    provide: LoginService,
                    useClass: LoginServiceMock
                }
            ],
            declarations: [
                MainComponentLegacyComponent,
                MockDotContentletEditorComponent,
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
        fixture.detectChanges();
    });

    describe('Contentlet Editor', () => {
        describe('Events', () => {
            it('should refresh the current portlet on close if portlet is content', () => {
                spyOn(dotRouterService, 'reloadCurrentPortlet');
                spyOnProperty(dotRouterService, 'currentPortlet', 'get').and.returnValue({
                    id: 'content'
                });
                const contentletEditor: DebugElement = de.query(By.css('dot-contentlet-editor'));
                contentletEditor.triggerEventHandler('close', {});

                expect(dotRouterService.reloadCurrentPortlet).toHaveBeenCalledTimes(1);
            });

            it('should not refresh the current portlet on close', () => {
                spyOn(dotRouterService, 'reloadCurrentPortlet');
                spyOnProperty(dotRouterService, 'currentPortlet', 'get').and.returnValue({
                    id: 'what'
                });
                const contentletEditor: DebugElement = de.query(By.css('dot-contentlet-editor'));
                contentletEditor.triggerEventHandler('close', {});

                expect(dotRouterService.reloadCurrentPortlet).not.toHaveBeenCalled();
            });
        });
    });
});
