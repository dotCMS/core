import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Component, DebugElement } from '@angular/core';
import { By } from '@angular/platform-browser';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { RouterTestingModule } from '@angular/router/testing';

import { TooltipModule } from 'primeng/tooltip';

import { DotCurrentUserService, DotSystemConfigService } from '@dotcms/data-access';
import { MenuGroup } from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';
import { DotCurrentUserServiceMock } from '@dotcms/utils-testing';

import { DotNavItemComponent } from './dot-nav-item.component';

import {
    LABEL_IMPORTANT_ICON,
    DotRandomIconPipe
} from '../../../../pipes/dot-radom-icon/dot-random-icon.pipe';
import { DotNavIconComponent } from '../dot-nav-icon/dot-nav-icon.component';
import { DotSubNavComponent } from '../dot-sub-nav/dot-sub-nav.component';

const defaultMenu: MenuGroup = {
    id: '123',
    label: 'Name',
    icon: 'icon',
    isOpen: false,
    menuItems: [
        {
            active: true,
            ajax: true,
            angular: true,
            id: '123',
            label: 'Label 1',
            url: 'url/one',
            menuLink: 'url/one',
            parentMenuId: '123',
            parentMenuLabel: 'Name',
            parentMenuIcon: 'icon'
        },
        {
            active: false,
            ajax: true,
            angular: true,
            id: '456',
            label: 'Label 2',
            url: 'url/two',
            menuLink: 'url/two',
            parentMenuId: '123',
            parentMenuLabel: 'Name',
            parentMenuIcon: 'icon'
        }
    ]
};

const menuForStore = {
    active: false,
    id: '123',
    isOpen: false,
    menuItems: defaultMenu.menuItems,
    name: 'Name',
    tabDescription: 'Description',
    tabIcon: 'icon',
    tabName: 'Name',
    url: 'url',
    label: 'Name'
};

@Component({
    selector: 'dot-nav-item-host',
    standalone: true,
    imports: [DotNavItemComponent],
    template: '<dot-nav-item [data]="menu" [collapsed]="collapsed"></dot-nav-item>'
})
class TestHostComponent {
    menu: MenuGroup = { ...defaultMenu };
    collapsed = false;
}

describe('DotNavItemComponent', () => {
    let spectator: Spectator<TestHostComponent>;
    let component: DotNavItemComponent;
    let host: TestHostComponent;
    let globalStore: InstanceType<typeof GlobalStore>;
    let navItemEl: HTMLElement;
    let subNavDe: DebugElement;

    beforeAll(() => {
        Element.prototype.getClientRects = jest.fn(
            () =>
                [
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
                ] as unknown as DOMRectList
        );
        Element.prototype.getBoundingClientRect = jest.fn(
            () =>
                ({
                    bottom: 1000,
                    height: 200,
                    top: 800,
                    left: 0,
                    right: 200,
                    width: 200,
                    x: 0,
                    y: 800
                }) as unknown as DOMRect
        );
    });

    const createComponent = createComponentFactory({
        component: TestHostComponent,
        imports: [
            RouterTestingModule,
            NoopAnimationsModule,
            TooltipModule,
            DotRandomIconPipe,
            DotSubNavComponent,
            DotNavIconComponent
        ],
        providers: [
            { provide: DotCurrentUserService, useClass: DotCurrentUserServiceMock },
            {
                provide: DotSystemConfigService,
                useValue: { getSystemConfig: () => of({}) }
            },
            GlobalStore,
            provideHttpClient(),
            provideHttpClientTesting()
        ],
        detectChanges: false
    });

    beforeEach(() => {
        defaultMenu.isOpen = true;
        spectator = createComponent();
        host = spectator.component;
        host.menu = { ...defaultMenu };
        host.collapsed = false;
        component = spectator.query(DotNavItemComponent);
        globalStore = spectator.inject(GlobalStore);
        globalStore.loadMenu([menuForStore]);
        spectator.detectChanges();

        navItemEl = spectator.query(byTestId('nav-item')) as HTMLElement;
        subNavDe = spectator.debugElement.query(By.css('dot-sub-nav'));
    });

    it('should set classes', () => {
        expect(navItemEl?.classList.contains('dot-nav__item--active')).toBe(true);
    });

    it('should have title wrapper set', () => {
        const title = spectator.query('.dot-nav__title');
        expect(title).toBeDefined();
    });

    it('should have icons set', () => {
        const iconDe = spectator.debugElement.query(By.css('dot-nav-icon'));
        const arrow = spectator.query(byTestId('nav-item-toggle'))?.querySelector('i');
        expect(iconDe?.componentInstance?.icon).toBe('icon');
        expect(arrow?.classList.contains('pi-chevron-up')).toBe(true);
    });

    it('should avoid label_important icon', () => {
        host.menu = { ...defaultMenu, icon: LABEL_IMPORTANT_ICON };
        spectator.detectChanges();
        const iconDe = spectator.debugElement.query(By.css('dot-nav-icon'));
        expect(iconDe?.componentInstance?.icon).not.toBe(LABEL_IMPORTANT_ICON);
    });

    it('should emit menuClick when nav__item is clicked', () => {
        const mainArea = spectator.query(byTestId('nav-item-main'));
        jest.spyOn(component.menuClick, 'emit');
        (mainArea as HTMLElement)?.click();
        expect(component.menuClick.emit).toHaveBeenCalledTimes(1);
    });

    describe('Toggle functionality', () => {
        it('should have two clickable areas (main and toggle)', () => {
            const mainArea = spectator.query(byTestId('nav-item-main'));
            const toggleArea = spectator.query(byTestId('nav-item-toggle'));
            expect(mainArea).toBeDefined();
            expect(toggleArea).toBeDefined();
        });

        it('should emit menuClick when clicking on the main area (first 2/3)', () => {
            const mainArea = spectator.query(byTestId('nav-item-main')) as HTMLElement;
            jest.spyOn(component.menuClick, 'emit');
            mainArea?.click();
            spectator.detectChanges();
            expect(component.menuClick.emit).toHaveBeenCalledTimes(1);
            expect(component.menuClick.emit).toHaveBeenCalledWith({
                originalEvent: expect.any(MouseEvent),
                data: expect.objectContaining({ id: '123', label: 'Name' })
            });
        });

        it('should emit menuClick with toggleOnly flag when clicking on toggle area (last 1/3)', () => {
            const toggleArea = spectator.query(byTestId('nav-item-toggle')) as HTMLElement;
            jest.spyOn(component.menuClick, 'emit');
            toggleArea?.click();
            spectator.detectChanges();
            expect(component.menuClick.emit).toHaveBeenCalledTimes(1);
            expect(component.menuClick.emit).toHaveBeenCalledWith({
                originalEvent: expect.any(MouseEvent),
                data: expect.objectContaining({ id: '123' }),
                toggleOnly: true
            });
        });

        it('should emit menuClick without toggleOnly flag when clicking on main area', () => {
            const mainArea = spectator.query(byTestId('nav-item-main')) as HTMLElement;
            jest.spyOn(component.menuClick, 'emit');
            mainArea?.click();
            spectator.detectChanges();
            expect(component.menuClick.emit).toHaveBeenCalledWith(
                expect.not.objectContaining({ toggleOnly: true })
            );
        });

        it('should stop propagation when clicking toggle area', () => {
            const toggleArea = spectator.query(byTestId('nav-item-toggle')) as HTMLElement;
            const event = new MouseEvent('click', { bubbles: true });
            jest.spyOn(event, 'stopPropagation');
            toggleArea?.dispatchEvent(event);
            expect(event.stopPropagation).toHaveBeenCalled();
        });
    });

    it('should set label correctly', () => {
        const label = spectator.query(byTestId('nav-item-label'));
        expect(label?.textContent?.trim()).toBe('Name');
    });

    describe('dot-sub-nav', () => {
        it('should set position correctly if there is not enough space at the bottom', async () => {
            defaultMenu.isOpen = true;
            spectator = createComponent();
            host = spectator.component;
            host.menu = { ...defaultMenu };
            host.collapsed = true;
            component = spectator.query(DotNavItemComponent);
            globalStore = spectator.inject(GlobalStore);
            globalStore.loadMenu([menuForStore]);
            spectator.detectChanges();
            navItemEl = spectator.query(byTestId('nav-item')) as HTMLElement;
            const subNav = spectator.debugElement.query(By.css('dot-sub-nav'))
                ?.componentInstance as DotSubNavComponent;
            const mockRect = { bottom: 2000, height: 200, top: 1800 };
            jest.spyOn(subNav?.ul?.nativeElement, 'getClientRects').mockReturnValue([mockRect]);
            Object.defineProperty(window, 'innerHeight', {
                writable: true,
                configurable: true,
                value: 1760
            });
            spectator.detectChanges();
            navItemEl?.dispatchEvent(new MouseEvent('mouseenter', { bubbles: true }));
            spectator.detectChanges();
            await spectator.fixture.whenStable();
            expect(component.customStyles).toBeDefined();
        });

        it('should set position correctly if there is enough space at the bottom', () => {
            defaultMenu.isOpen = true;
            spectator = createComponent();
            host = spectator.component;
            host.menu = { ...defaultMenu };
            host.collapsed = true;
            component = spectator.query(DotNavItemComponent);
            globalStore = spectator.inject(GlobalStore);
            globalStore.loadMenu([menuForStore]);
            spectator.detectChanges();
            navItemEl = spectator.query(byTestId('nav-item')) as HTMLElement;
            const subNav = spectator.debugElement.query(By.css('dot-sub-nav'));
            const subNavComp = subNav?.componentInstance as DotSubNavComponent;
            const mockRect = { bottom: 2000, height: 200, top: 1800 };
            jest.spyOn(subNavComp?.ul?.nativeElement, 'getClientRects').mockReturnValue([mockRect]);
            Object.defineProperty(window, 'innerHeight', {
                writable: true,
                configurable: true,
                value: 1200
            });
            (subNav?.nativeElement as HTMLElement).style.position = 'absolute';
            (subNav?.nativeElement as HTMLElement).style.top = '5000px';
            navItemEl.style.position = 'absolute';
            navItemEl.style.top = '800px';
            spectator.detectChanges();
            navItemEl?.dispatchEvent(new MouseEvent('mouseenter', { bubbles: true }));
            spectator.detectChanges();
            expect(component.customStyles).toEqual(
                expect.objectContaining({
                    bottom: '0',
                    top: 'auto'
                })
            );
        });

        it('should reset menu position when mouseleave', () => {
            defaultMenu.isOpen = true;
            spectator = createComponent();
            host = spectator.component;
            host.menu = { ...defaultMenu };
            host.collapsed = true;
            component = spectator.query(DotNavItemComponent);
            globalStore = spectator.inject(GlobalStore);
            globalStore.loadMenu([menuForStore]);
            spectator.detectChanges();
            const navItemHost = spectator.query('dot-nav-item') as HTMLElement;
            navItemHost?.dispatchEvent(new MouseEvent('mouseleave', { bubbles: true }));
            spectator.detectChanges();
            expect(component.customStyles).toEqual({ overflow: 'hidden' });
        });

        it('should set data correctly', () => {
            expect(subNavDe?.componentInstance?.data).toEqual(
                expect.objectContaining({ id: '123', label: 'Name' })
            );
            expect(subNavDe?.componentInstance?.collapsed).toBe(false);
        });

        it('should emit itemClick on dot-sub-nav itemClick', () => {
            jest.spyOn(component.itemClick, 'emit');
            (subNavDe?.componentInstance as DotSubNavComponent)?.itemClick?.emit({
                originalEvent: new MouseEvent('click'),
                data: defaultMenu.menuItems[0]
            });
            expect(component.itemClick.emit).toHaveBeenCalledTimes(1);
        });
    });

    describe('Collapsed', () => {
        beforeEach(() => {
            defaultMenu.isOpen = true;
            spectator = createComponent();
            host = spectator.component;
            host.menu = { ...defaultMenu };
            host.collapsed = true;
            component = spectator.query(DotNavItemComponent);
            globalStore = spectator.inject(GlobalStore);
            globalStore.loadMenu([menuForStore]);
            spectator.detectChanges();
            subNavDe = spectator.debugElement.query(By.css('dot-sub-nav'));
        });

        it('should set data correctly on sub-nav', () => {
            expect(subNavDe?.componentInstance?.collapsed).toBe(true);
        });
    });
});
