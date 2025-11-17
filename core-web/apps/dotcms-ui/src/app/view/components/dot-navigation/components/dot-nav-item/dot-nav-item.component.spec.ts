import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Component, DebugElement } from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { RouterTestingModule } from '@angular/router/testing';

import { TooltipModule } from 'primeng/tooltip';

import { DotSystemConfigService } from '@dotcms/data-access';
import { DotMenu } from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';
import { DotIconComponent } from '@dotcms/ui';

import { DotNavItemComponent } from './dot-nav-item.component';

import {
    LABEL_IMPORTANT_ICON,
    DotRandomIconPipe
} from '../../../../pipes/dot-radom-icon/dot-random-icon.pipe';
import { dotMenuMock } from '../../services/dot-navigation.service.spec';
import { DotNavIconComponent } from '../dot-nav-icon/dot-nav-icon.component';
import { DotSubNavComponent } from '../dot-sub-nav/dot-sub-nav.component';

@Component({
    selector: 'dot-test-host-component',
    template: `
        <dot-nav-item [data]="menu" [collapsed]="collapsed"></dot-nav-item>
    `,
    standalone: false
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

    // Mock getClientRects globally to avoid undefined errors
    beforeAll(() => {
        Element.prototype.getClientRects = jest.fn(() => [
            {
                bottom: 1000,
                height: 200,
                top: 800,
                left: 0,
                right: 200,
                width: 200,
                x: 0,
                y: 800
            }
        ]);

        Element.prototype.getBoundingClientRect = jest.fn(() => ({
            bottom: 1000,
            height: 200,
            top: 800,
            left: 0,
            right: 200,
            width: 200,
            x: 0,
            y: 800
        }));
    });

    beforeEach(waitForAsync(() => {
        TestBed.configureTestingModule({
            declarations: [TestHostComponent],
            imports: [
                DotNavItemComponent,
                DotSubNavComponent,
                DotNavIconComponent,
                DotIconComponent,
                RouterTestingModule,
                BrowserAnimationsModule,
                TooltipModule,
                DotRandomIconPipe
            ],
            providers: [
                {
                    provide: DotSystemConfigService,
                    useValue: { getSystemConfig: () => ({ of: jest.fn() }) }
                },
                GlobalStore,
                provideHttpClient(),
                provideHttpClientTesting(),
                DotRandomIconPipe
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
        navItem = de.query(By.css('[data-testid="nav-item"]'));
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

    it('should avoid label_important icon', () => {
        componentHost.menu.tabIcon = LABEL_IMPORTANT_ICON;
        fixtureHost.detectChanges();
        const icon: DebugElement = de.query(By.css('dot-nav-icon'));

        expect(icon.componentInstance.icon).not.toBe(LABEL_IMPORTANT_ICON);
    });

    it('should emit menuClick when nav__item is clicked', () => {
        const mainArea = de.query(By.css('[data-testid="nav-item-main"]'));
        jest.spyOn(component.menuClick, 'emit');
        mainArea.nativeElement.click();
        expect(component.menuClick.emit).toHaveBeenCalledTimes(1);
    });

    describe('Toggle functionality', () => {
        let mainArea: DebugElement;
        let toggleArea: DebugElement;

        beforeEach(() => {
            mainArea = de.query(By.css('[data-testid="nav-item-main"]'));
            toggleArea = de.query(By.css('[data-testid="nav-item-toggle"]'));
        });

        it('should have two clickable areas (main and toggle)', () => {
            expect(mainArea).toBeDefined();
            expect(toggleArea).toBeDefined();
        });

        it('should emit menuClick when clicking on the main area (first 2/3)', () => {
            jest.spyOn(component.menuClick, 'emit');
            mainArea.nativeElement.click();
            fixtureHost.detectChanges();

            expect(component.menuClick.emit).toHaveBeenCalledTimes(1);
            expect(component.menuClick.emit).toHaveBeenCalledWith({
                originalEvent: expect.any(MouseEvent),
                data: componentHost.menu
            });
        });

        it('should emit menuClick with toggleOnly flag when clicking on toggle area (last 1/3)', () => {
            jest.spyOn(component.menuClick, 'emit');
            toggleArea.nativeElement.click();
            fixtureHost.detectChanges();

            expect(component.menuClick.emit).toHaveBeenCalledTimes(1);
            expect(component.menuClick.emit).toHaveBeenCalledWith({
                originalEvent: expect.any(MouseEvent),
                data: componentHost.menu,
                toggleOnly: true
            });
        });

        it('should emit menuClick without toggleOnly flag when clicking on main area', () => {
            jest.spyOn(component.menuClick, 'emit');
            mainArea.nativeElement.click();
            fixtureHost.detectChanges();

            expect(component.menuClick.emit).toHaveBeenCalledWith({
                originalEvent: expect.any(MouseEvent),
                data: componentHost.menu
            });
            expect(component.menuClick.emit).toHaveBeenCalledWith(
                expect.not.objectContaining({ toggleOnly: true })
            );
        });

        it('should stop propagation when clicking toggle area', () => {
            const event = new MouseEvent('click', { bubbles: true });
            jest.spyOn(event, 'stopPropagation');

            toggleArea.nativeElement.dispatchEvent(event);

            expect(event.stopPropagation).toHaveBeenCalled();
        });
    });

    it('should set label correctly', () => {
        const label: DebugElement = de.query(By.css('[data-testid="nav-item-label"]'));
        expect(label.nativeElement.textContent.trim()).toBe('Name');
    });

    describe('dot-sub-nav', () => {
        it('should set position correctly if there is not enough space at the bottom', async () => {
            deHost.componentInstance.collapsed = true;

            // Mock getClientRects to return a valid rect with bottom property
            const mockRect = { bottom: 2000, height: 200, top: 1800 };
            jest.spyOn(subNav.componentInstance.ul.nativeElement, 'getClientRects').mockReturnValue(
                [mockRect]
            );

            // Mock window.innerHeight using Object.defineProperty
            Object.defineProperty(window, 'innerHeight', {
                writable: true,
                configurable: true,
                value: 1760
            });

            fixtureHost.detectChanges();

            navItem.nativeElement.dispatchEvent(new MouseEvent('mouseenter', { bubbles: true }));
            fixtureHost.detectChanges();

            await fixtureHost.whenStable();

            expect(subNav.styles).toBeDefined();
        });

        it('should set position correctly if there is enough space at the bottom', () => {
            deHost.componentInstance.collapsed = true;

            // Mock getClientRects to return a rect that does NOT fit in the bottom space
            const mockRect = { bottom: 2000, height: 200, top: 1800 };
            jest.spyOn(subNav.componentInstance.ul.nativeElement, 'getClientRects').mockReturnValue(
                [mockRect]
            );

            // Mock window.innerHeight to be smaller than the bottom position
            Object.defineProperty(window, 'innerHeight', {
                writable: true,
                configurable: true,
                value: 1200
            });

            subNav.nativeElement.style.position = 'absolute';
            subNav.nativeElement.style.top = '5000px'; // moving it out of the window
            de.nativeElement.style.position = 'absolute';
            de.nativeElement.style.top = '800px';

            fixtureHost.detectChanges();

            navItem.nativeElement.dispatchEvent(new MouseEvent('mouseenter', { bubbles: true }));
            fixtureHost.detectChanges();

            expect(subNav.styles.cssText).toEqual(
                'height: 0px; overflow: hidden; position: absolute; top: 5000px; bottom: 0px;'
            );
        });

        it('should reset menu position when mouseleave', () => {
            component.collapsed = true;
            de.nativeElement.dispatchEvent(new MouseEvent('mouseleave', { bubbles: true }));
            fixtureHost.detectChanges();
            expect(subNav.styles.cssText).toEqual('height: 0px; overflow: hidden;');
        });

        it('should set data correctly', () => {
            expect(subNav.componentInstance.data).toEqual(componentHost.menu);
            expect(subNav.componentInstance.collapsed).toBe(false);
        });

        it('should emit itemClick on dot-sub-nav itemClick', () => {
            jest.spyOn(component.itemClick, 'emit');
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
