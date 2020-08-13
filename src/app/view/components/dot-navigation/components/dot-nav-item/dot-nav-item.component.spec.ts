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

@Component({
    selector: 'dot-test-host-component',
    template: ` <dot-nav-item [data]="menu" [collapsed]="collapsed"></dot-nav-item> `
})
class TestHostComponent {
    menu: DotMenu = {
        ...dotMenuMock(),
        active: true
    };
    collapsed = false;
}

describe('DotNavItemComponent', () => {
    let fixtureHost: ComponentFixture<TestHostComponent>;
    let componentHost: TestHostComponent;
    let component: DotNavItemComponent;
    let de: DebugElement;
    let deHost: DebugElement;
    let navItem: DebugElement;
    let subNav: DebugElement;

    beforeEach(async(() => {
        TestBed.configureTestingModule({
            declarations: [TestHostComponent, DotNavItemComponent, DotSubNavComponent],
            imports: [
                DotNavIconModule,
                DotIconModule,
                RouterTestingModule,
                BrowserAnimationsModule,
                TooltipModule
            ]
        }).compileComponents();
    }));

    beforeEach(() => {
        fixtureHost = TestBed.createComponent(TestHostComponent);
        deHost = fixtureHost.debugElement;
        componentHost = fixtureHost.componentInstance;
        de = deHost.query(By.css('dot-nav-item'));
        component = de.componentInstance;
        fixtureHost.detectChanges();
        navItem = de.query(By.css('.dot-nav__item'));
        subNav = de.query(By.css('dot-sub-nav'));
    });

    it('should set classes', () => {
        expect(navItem.nativeElement.classList.contains('dot-nav__item--active')).toBe(true);
    });

    it('should have title wrapper set', () => {
        const title: DebugElement = de.query(By.css('.dot-nav__title'));

        expect(title).toBeDefined();
    });

    it('should have icons set', () => {
        const icon: DebugElement = de.query(By.css('dot-nav-icon'));
        const arrow: DebugElement = de.query(By.css('.dot-nav__item-arrow'));

        expect(icon.componentInstance.icon).toBe('icon');
        expect(arrow.componentInstance.name).toBe('arrow_drop_up');
    });

    it('should emit menuClick when nav__item is clicked', () => {
        spyOn(component.menuClick, 'emit');
        navItem.nativeElement.dispatchEvent(new MouseEvent('click', {}));
        expect(component.menuClick.emit).toHaveBeenCalledTimes(1);
    });

    it('should set label correctly', () => {
        const label: DebugElement = de.query(By.css('.dot-nav__item-label'));
        expect(label.nativeElement.textContent.trim()).toBe('Name');
    });

    describe('dot-sub-nav', () => {
        it('should set position correctly if there is not enough space at the bottom', () => {
            deHost.componentInstance.collapsed = true;

            subNav.nativeElement.style.position = 'absolute';
            subNav.nativeElement.style.top = '5000px'; // moving it out of the window

            fixtureHost.detectChanges();

            navItem.triggerEventHandler('mouseenter', {});
            fixtureHost.detectChanges();

            fixtureHost.whenStable().then(() => {
                expect(subNav.styles).not.toEqual({});
                spyOnProperty(window, 'innerHeight').and.returnValue(1760);
                navItem.triggerEventHandler('mouseenter', {});
                fixtureHost.detectChanges();
                expect(subNav.styles).toBeDefined();
            });
        });

        it('should set position correctly if there is enough space at the bottom', () => {
            deHost.componentInstance.collapsed = true;

            subNav.nativeElement.style.position = 'absolute';
            subNav.nativeElement.style.top = '5000px'; // moving it out of the window
            de.nativeElement.style.position = 'absolute';
            de.nativeElement.style.top = '800px';

            fixtureHost.detectChanges();

            navItem.triggerEventHandler('mouseenter', {});
            fixtureHost.detectChanges();

            expect(subNav.styles).toEqual({
                bottom: '0',
                top: 'auto'
            });
        });

        it('should reset menu position when mouseleave', () => {
            component.collapsed = true;
            de.triggerEventHandler('mouseleave', {});
            fixtureHost.detectChanges();
            expect(subNav.styles).toEqual({
                overflow: 'hidden'
            });
        });

        it('should set data correctly', () => {
            expect(subNav.componentInstance.data).toEqual(componentHost.menu);
            expect(subNav.componentInstance.collapsed).toBe(false);
        });

        it('should emit itemClick on dot-sub-nav itemClick', () => {
            spyOn(component.itemClick, 'emit');
            subNav.nativeElement.dispatchEvent(new CustomEvent('itemClick', {}));
            expect(component.itemClick.emit).toHaveBeenCalledTimes(1);
        });
    });

    describe('Collapsed', () => {
        beforeEach(() => {
            componentHost.collapsed = true;
            fixtureHost.detectChanges();
        });

        it('should set data correctly on sub-nav', () => {
            expect(subNav.componentInstance.collapsed).toBe(true);
        });
    });
});
