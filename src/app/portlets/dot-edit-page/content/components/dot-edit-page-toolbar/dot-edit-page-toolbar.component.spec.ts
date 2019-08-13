import { async, ComponentFixture } from '@angular/core/testing';
import { DebugElement, Component, Input } from '@angular/core';
import { By } from '@angular/platform-browser';

import { of } from 'rxjs';

import { CheckboxModule, ToolbarModule } from 'primeng/primeng';

import { MockDotMessageService } from '@tests/dot-message-service.mock';
import { DOTTestBed } from '@tests/dot-test-bed';
import { mockDotRenderedPageState } from '@tests/dot-rendered-page-state.mock';
import { DotPageStateService } from '../../services/dot-page-state/dot-page-state.service';

import { DotEditPageStateControllerModule } from '../dot-edit-page-state-controller/dot-edit-page-state-controller.module';
import { DotEditPageToolbarComponent } from './dot-edit-page-toolbar.component';
import { DotEditPageViewAsControllerModule } from '../dot-edit-page-view-as-controller/dot-edit-page-view-as-controller.module';
import { DotLicenseService } from '@services/dot-license/dot-license.service';
import { DotMessageService } from '@services/dot-messages-service';
import { DotPageMode } from '@portlets/dot-edit-page/shared/models/dot-page-mode.enum';
import { DotRenderedPageState } from '@portlets/dot-edit-page/shared/models/dot-rendered-page-state.model';

@Component({
    selector: 'dot-test-host-component',
    template: `
        <dot-edit-page-toolbar [pageState]="pageState"></dot-edit-page-toolbar>
    `
})
class TestHostComponent {
    @Input() pageState: DotRenderedPageState = mockDotRenderedPageState;
}

describe('DotEditPageToolbarComponent', () => {
    let fixtureHost: ComponentFixture<TestHostComponent>;
    let componentHost: TestHostComponent;
    let component: DotEditPageToolbarComponent;
    let de: DebugElement;
    let deHost: DebugElement;
    let dotLicenseService: DotLicenseService;

    beforeEach(async(() => {
        DOTTestBed.configureTestingModule({
            declarations: [TestHostComponent, DotEditPageToolbarComponent],
            imports: [
                CheckboxModule,
                DotEditPageViewAsControllerModule,
                DotEditPageStateControllerModule,
                ToolbarModule
            ],
            providers: [
                DotLicenseService,
                {
                    provide: DotMessageService,
                    useValue: new MockDotMessageService({
                        'dot.common.whats.changed': 'Whats'
                    })
                },
                {
                    provide: DotPageStateService,
                    useValue: {}
                }
            ]
        });
    }));

    beforeEach(() => {
        fixtureHost = DOTTestBed.createComponent(TestHostComponent);
        deHost = fixtureHost.debugElement;
        componentHost = fixtureHost.componentInstance;

        de = deHost.query(By.css('dot-edit-page-toolbar'));
        component = de.componentInstance;

        dotLicenseService = de.injector.get(DotLicenseService);
    });

    describe('elements', () => {
        beforeEach(() => {
            fixtureHost.detectChanges();
        });

        it('should have a primeng toolbar element', () => {
            expect(de.query(By.css('p-toolbar'))).toBeDefined();
            expect(de.query(By.css('.ui-toolbar-group-left'))).toBeDefined();
            expect(de.query(By.css('.ui-toolbar-group-right'))).toBeDefined();
        });

        it('should have page state controller', () => {
            const pageStateController = de.query(By.css('dot-edit-page-state-controller'))
                .componentInstance;
            expect(pageStateController).toBeDefined();
            expect(pageStateController.pageState).toEqual(mockDotRenderedPageState);
        });

        it('should have view as controller', () => {
            const viewAsController = de.query(By.css('dot-edit-page-view-as-controller'))
                .componentInstance;
            expect(viewAsController).toBeDefined();
            expect(viewAsController.pageState).toEqual(mockDotRenderedPageState);
        });
    });

    describe('what\'s change', () => {
        describe('no license', () => {
            beforeEach(() => {
                spyOn(dotLicenseService, 'isEnterprise').and.returnValue(of(false));
                fixtureHost.detectChanges();
            });

            it('should not show', () => {
                const whatsChangedElem = de.query(By.css('p-checkbox'));
                expect(whatsChangedElem).toBeNull();
            });
        });

        describe('with license', () => {
            beforeEach(() => {
                spyOn(component.whatschange, 'emit');
                spyOn(dotLicenseService, 'isEnterprise').and.returnValue(of(true));
            });

            it('should hide what\'s change selector', () => {
                componentHost.pageState.state.mode = DotPageMode.EDIT;
                fixtureHost.detectChanges();

                const whatsChangedElem = de.query(By.css('p-checkbox'));
                expect(whatsChangedElem).toBeNull();
            });

            it('should have what\'s change selector', () => {
                componentHost.pageState.state.mode = DotPageMode.PREVIEW;
                fixtureHost.detectChanges();

                const whatsChangedElem = de.query(By.css('p-checkbox'));
                expect(whatsChangedElem).toBeDefined();
                expect(whatsChangedElem.componentInstance.label).toBe('Whats');
            });

            describe('events', () => {
                let whatsChangedElem: DebugElement;

                beforeEach(() => {
                    componentHost.pageState.state.mode = DotPageMode.PREVIEW;
                    fixtureHost.detectChanges();
                    whatsChangedElem = de.query(By.css('p-checkbox'));
                });

                it('should emit what\'s change in true', () => {
                    whatsChangedElem.triggerEventHandler('onChange', true);
                    expect(component.whatschange.emit).toHaveBeenCalledTimes(1);
                    expect(component.whatschange.emit).toHaveBeenCalledWith(true);
                });

                it('should emit what\'s change in false', () => {
                    whatsChangedElem.triggerEventHandler('onChange', false);
                    expect(component.whatschange.emit).toHaveBeenCalledTimes(1);
                    expect(component.whatschange.emit).toHaveBeenCalledWith(false);
                });
            });
        });
    });
});
