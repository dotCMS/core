/* tslint:disable:no-unused-variable */
import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { Component, DebugElement } from '@angular/core';

import { DotNavItemComponent } from './dot-nav-item.component';
import { DotNavIconModule } from '../dot-nav-icon/dot-nav-icon.module';
import { DotIconModule } from '../../../_common/dot-icon/dot-icon.module';
import { DotSubNavComponent } from '../dot-sub-nav/dot-sub-nav.component';
import { RouterTestingModule } from '@angular/router/testing';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { dotMenuMock } from '../../services/dot-navigation.service.spec';
import { DotMenu } from '@models/navigation';
import { TooltipModule } from 'primeng/primeng';
import { IframeOverlayService } from '@components/_common/iframe/service/iframe-overlay.service';
import { DotEventsService } from '@services/dot-events/dot-events.service';
import { DOTTestBed } from '@tests/dot-test-bed';
import { BehaviorSubject } from 'rxjs';

@Component({
    selector: 'dot-test-host-component',
    template: `
        <dot-nav-item [data]="menu" [collapsed]="collapsed$ | async"></dot-nav-item>
    `
})
class TestHostComponent {
    menu: DotMenu = {
        ...dotMenuMock(),
        active: true
    };
    collapsed$: BehaviorSubject<boolean> = new BehaviorSubject(false);
}

describe('DotNavItemComponent', () => {
    let fixtureHost: ComponentFixture<TestHostComponent>;
    let componentHost: TestHostComponent;
    let dotEventsService: DotEventsService;
    let iframeOverlayService: IframeOverlayService;
    let component: DotNavItemComponent;
    let fixture: ComponentFixture<DotNavItemComponent>;
    let de: DebugElement;
    let deHost: DebugElement;
    let navItem: DebugElement;
    let subNav: DebugElement;

    beforeEach(
        async(() => {
            TestBed.configureTestingModule({
                declarations: [TestHostComponent, DotNavItemComponent, DotSubNavComponent],
                providers: [IframeOverlayService, DotEventsService],
                imports: [
                    DotNavIconModule,
                    DotIconModule,
                    RouterTestingModule,
                    BrowserAnimationsModule,
                    TooltipModule
                ]
            }).compileComponents();
        })
    );

    beforeEach(() => {
        fixtureHost = DOTTestBed.createComponent(TestHostComponent);
        deHost = fixtureHost.debugElement;
        componentHost = fixtureHost.componentInstance;
        de = deHost.query(By.css('dot-nav-item'));
        component = de.componentInstance;
        fixture = TestBed.createComponent(DotNavItemComponent);
        fixtureHost.detectChanges();
        dotEventsService = fixture.debugElement.injector.get(DotEventsService);
        iframeOverlayService = fixture.debugElement.injector.get(IframeOverlayService);
        navItem = de.query(By.css('.dot-nav__item'));
        subNav = de.query(By.css('dot-sub-nav'));
    });

    it('should set classes', () => {
        expect(navItem.nativeElement.classList.contains('dot-nav__item--active')).toBe(true);
    });

    it('should have icons set', () => {
        const icon: DebugElement = de.query(By.css('dot-nav-icon'));
        const arrow: DebugElement = de.query(By.css('.dot-nav__item-arrow'));

        expect(icon.componentInstance.icon).toBe('icon');
        expect(arrow.componentInstance.name).toBe('arrow_drop_up');
    });

    it('should emit menuClick and notify when nav__item is clicked', () => {
        spyOn(dotEventsService, 'notify');
        spyOn(component.menuClick, 'emit');
        navItem.nativeElement.dispatchEvent(new MouseEvent('click', {}));

        expect(dotEventsService.notify).toHaveBeenCalledWith('hide-sub-nav-fly-out');
        expect(component.menuClick.emit).toHaveBeenCalledTimes(1);
    });

    it('should set label correctly', () => {
        const label: DebugElement = de.query(By.css('.dot-nav__item-label'));
        expect(label.nativeElement.textContent.trim()).toBe('Name');
    });

    describe('dot-sub-nav', () => {
        it('should set data correctly', () => {
            expect(subNav.componentInstance.data).toEqual(componentHost.menu);
            expect(subNav.componentInstance.contextmenu).toBe(false);
        });

        it('should notify and emit itemClick on dot-sub-nav itemClick', () => {
            spyOn(dotEventsService, 'notify');
            spyOn(component.itemClick, 'emit');
            subNav.nativeElement.dispatchEvent(new CustomEvent('itemClick', {}));

            expect(dotEventsService.notify).toHaveBeenCalledWith('hide-sub-nav-fly-out');
            expect(component.itemClick.emit).toHaveBeenCalledTimes(1);
        });
    });

    describe('Collapsed', () => {
        beforeEach(() => {
            componentHost.collapsed$.next(true);
            fixtureHost.detectChanges();
        });

        it('should set data correctly on sub-nav', () => {
            expect(subNav.componentInstance.contextmenu).toBe(false);
        });

        describe('Fly-out Menu', () => {
            beforeEach(() => {
                spyOn(dotEventsService, 'notify').and.callThrough();
                spyOn(iframeOverlayService, 'show').and.callThrough();
                de.triggerEventHandler('contextmenu', new MouseEvent('contextmenu'));
                fixtureHost.detectChanges();
            });

            it('should set data correctly on sub-nav', () => {
                expect(subNav.componentInstance.contextmenu).toBe(true);
            });

            it('should open on right click', () => {
                expect(component.contextmenu).toBe(true);
                expect(dotEventsService.notify).toHaveBeenCalledTimes(1);
                expect(iframeOverlayService.show).toHaveBeenCalledTimes(1);
                expect(de.nativeElement.classList.contains('contextmenu')).toBe(true);
            });
            it('should close on iframe overlay click', () => {
                iframeOverlayService.hide();
                expect(component.contextmenu).toBe(false);
            });
            it('should close on dotEventsService  hide-sub-nav-fly-out', () => {
                spyOn(iframeOverlayService, 'hide');
                dotEventsService.notify('hide-sub-nav-fly-out');

                expect(component.contextmenu).toBe(false);
                expect(iframeOverlayService.hide).toHaveBeenCalledTimes(1);
            });
            it('should close on document click', () => {
                document.dispatchEvent(new MouseEvent('click'));
                expect(component.contextmenu).toBe(false);
            });
        });
    });

    describe('tooltip', () => {
        beforeEach(() => {});

        it('should set tooltip properties', () => {
            expect(navItem.attributes['ng-reflect-disabled']).toEqual('true');
            expect(navItem.attributes['ng-reflect-text']).toEqual(dotMenuMock().tabName);
            expect(navItem.attributes['tooltipStyleClass']).toEqual('dot-nav__tooltip');
            expect(navItem.attributes['appendTo']).toEqual('target');
        });

        describe('collapse', () => {
            beforeEach(() => {
                componentHost.collapsed$.next(true);
                fixtureHost.detectChanges();
            });

            it('should enable on collapse', () => {
                expect(navItem.attributes['ng-reflect-disabled']).toEqual('false');
            });

            it('should be null and disabled on right-click', () => {
                de.triggerEventHandler('contextmenu', new MouseEvent('contextmenu'));
                fixtureHost.detectChanges();
                expect(navItem.attributes['ng-reflect-disabled']).toEqual('true');
                expect(navItem.attributes['ng-reflect-text']).toEqual(null);
            });
        });
    });
});
