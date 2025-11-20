import { Spectator, SpyObject, createComponentFactory, mockProvider } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';

import { TooltipModule } from 'primeng/tooltip';

import { DotEventsService, DotRouterService, DotSystemConfigService } from '@dotcms/data-access';
import { LoginService } from '@dotcms/dotcms-js';
import { GlobalStore } from '@dotcms/store';
import { DotIconComponent } from '@dotcms/ui';
import { LoginServiceMock } from '@dotcms/utils-testing';

import { DotNavIconComponent } from './components/dot-nav-icon/dot-nav-icon.component';
import { DotNavItemComponent } from './components/dot-nav-item/dot-nav-item.component';
import { DotNavigationComponent } from './dot-navigation.component';
import { DotNavigationService } from './services/dot-navigation.service';
import { dotMenuMock, dotMenuMock1 } from './services/dot-navigation.service.spec';

import { DotMenuService } from '../../../api/services/dot-menu.service';
import { DotRandomIconPipe } from '../../pipes/dot-radom-icon/dot-random-icon.pipe';
import { IframeOverlayService } from '../_common/iframe/service/iframe-overlay.service';

describe('DotNavigationComponent collapsed', () => {
    let spectator: Spectator<DotNavigationComponent>;
    let iframeOverlayService: SpyObject<IframeOverlayService>;
    let globalStore: InstanceType<typeof GlobalStore>;
    let dotRouterService: SpyObject<DotRouterService>;

    const createComponent = createComponentFactory({
        component: DotNavigationComponent,
        imports: [
            DotNavIconComponent,
            DotIconComponent,
            BrowserAnimationsModule,
            TooltipModule,
            DotRandomIconPipe
        ],
        providers: [
            provideRouter([]),
            DotMenuService,
            mockProvider(IframeOverlayService),
            mockProvider(DotNavigationService),
            mockProvider(DotEventsService),
            mockProvider(DotRouterService, {
                currentPortlet: { id: '123' }
            }),
            {
                provide: LoginService,
                useClass: LoginServiceMock
            },
            {
                provide: DotSystemConfigService,
                useValue: { getSystemConfig: () => of({}) }
            },
            GlobalStore,
            provideHttpClient(),
            provideHttpClientTesting()
        ]
    });

    beforeEach(() => {
        spectator = createComponent({
            detectChanges: false
        });

        iframeOverlayService = spectator.inject(IframeOverlayService);
        globalStore = spectator.inject(GlobalStore);
        dotRouterService = spectator.inject(DotRouterService);

        // Load menu items in the GlobalStore instead of using service's items$
        globalStore.loadMenu([dotMenuMock(), dotMenuMock1()]);
    });

    it('should have all menus closed', () => {
        spectator.detectChanges();
        const items = spectator.queryAll('.dot-nav__list-item');

        items.forEach((item) => {
            expect(item.classList.contains('dot-nav__list-item--active')).toBe(false);
        });
    });

    it('should have dot-nav-item print correctly', () => {
        spectator.detectChanges();

        const items = spectator.queryAll(DotNavItemComponent);

        expect(items.length).toBe(2);
        // The store transforms DotMenu to MenuGroup with additional parent properties
        const expectedMenu1 = {
            id: '123',
            label: 'Name',
            icon: 'icon',
            menuItems: [
                {
                    active: false,
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
            ],
            isOpen: false
        };
        expect(items[0].data).toEqual(expectedMenu1);
        // For the second menu, we need to check it has the transformed structure
        expect(items[1].data.id).toBe('456');
        expect(items[1].data.menuItems.length).toBeGreaterThan(0);
    });

    it('should close on document click', () => {
        spectator.detectChanges();

        // First, ensure navigation is collapsed
        globalStore.collapseNavigation();
        spectator.detectChanges();

        // Then open a parent menu group
        globalStore.toggleParent('123');
        expect(globalStore.openParentMenuId()).toBe('123');

        // Then click on document (when collapsed)
        spectator.dispatchMouseEvent(spectator.element, 'click');
        // When collapsed, clicking should close all parent menu groups via GlobalStore
        expect(globalStore.openParentMenuId()).toBe(null);
    });

    describe('itemClick event', () => {
        it('should reload portlet and hide overlay', () => {
            spectator.detectChanges();

            const stopPropSpy = jest.fn();

            spectator.component.onItemClick({
                originalEvent: {
                    stopPropagation: stopPropSpy,
                    ctrlKey: false,
                    metaKey: false
                } as unknown as MouseEvent,
                data: dotMenuMock().menuItems[0]
            });

            expect(stopPropSpy).toHaveBeenCalled();
            expect(dotRouterService.reloadCurrentPortlet).toHaveBeenCalledWith('123');
            expect(dotRouterService.reloadCurrentPortlet).toHaveBeenCalledTimes(1);
            expect(iframeOverlayService.hide).toHaveBeenCalledTimes(1);
        });

        it('should NOT reload portlet', () => {
            spectator.detectChanges();

            const stopPropSpy = jest.fn();

            spectator.component.onItemClick({
                originalEvent: {
                    stopPropagation: stopPropSpy,
                    ctrlKey: true,
                    metaKey: false
                } as unknown as MouseEvent,
                data: dotMenuMock().menuItems[0]
            });

            expect(stopPropSpy).toHaveBeenCalled();
            expect(dotRouterService.reloadCurrentPortlet).not.toHaveBeenCalled();
        });
    });

    describe('menuClick event collapsed', () => {
        it('should navigate to portlet when menu is collapsed', () => {
            spectator.detectChanges();

            const mockMenu = globalStore.menuGroup().find((m) => m.id === '123');
            if (mockMenu) {
                spectator.component.onMenuClick({
                    originalEvent: {} as unknown as MouseEvent,
                    data: mockMenu
                });
            }

            expect(dotRouterService.gotoPortlet).toHaveBeenCalledWith('url/one', {}, '123');
            expect(dotRouterService.gotoPortlet).toHaveBeenCalledTimes(1);
        });

        it('should not have scroll when collapsed', () => {
            // Ensure navigation is collapsed
            globalStore.collapseNavigation();
            spectator.detectChanges();

            expect(spectator.debugElement.styles.cssText).toEqual('');
        });
    });
});

describe('DotNavigationComponent expanded', () => {
    let spectator: Spectator<DotNavigationComponent>;
    let iframeOverlayService: SpyObject<IframeOverlayService>;
    let globalStore: InstanceType<typeof GlobalStore>;
    let dotEventsService: SpyObject<DotEventsService>;
    let dotRouterService: SpyObject<DotRouterService>;

    const createComponent = createComponentFactory({
        component: DotNavigationComponent,
        imports: [
            DotNavIconComponent,
            DotIconComponent,
            BrowserAnimationsModule,
            TooltipModule,
            DotRandomIconPipe
        ],
        providers: [
            provideRouter([]),
            DotMenuService,
            mockProvider(IframeOverlayService),
            mockProvider(DotNavigationService),
            mockProvider(DotEventsService),
            mockProvider(DotRouterService, {
                currentPortlet: { id: '123' }
            }),
            {
                provide: LoginService,
                useClass: LoginServiceMock
            },
            {
                provide: DotSystemConfigService,
                useValue: { getSystemConfig: () => of({}) }
            },
            GlobalStore,
            provideHttpClient(),
            provideHttpClientTesting()
        ]
    });

    beforeEach(() => {
        spectator = createComponent({
            detectChanges: false
        });

        iframeOverlayService = spectator.inject(IframeOverlayService);
        globalStore = spectator.inject(GlobalStore);
        dotEventsService = spectator.inject(DotEventsService);
        dotRouterService = spectator.inject(DotRouterService);

        // Load menu items in the GlobalStore instead of using service's items$
        // Create menus without active items to avoid auto-opening when expanding
        const menuWithoutActive = {
            ...dotMenuMock(),
            menuItems: dotMenuMock().menuItems.map((item) => ({ ...item, active: false }))
        };
        const menu1WithoutActive = {
            ...dotMenuMock1(),
            menuItems: dotMenuMock1().menuItems.map((item) => ({ ...item, active: false }))
        };
        globalStore.loadMenu([menuWithoutActive, menu1WithoutActive]);
        // Set navigation as expanded
        globalStore.expandNavigation();
    });

    it('should have all menus closed', () => {
        spectator.detectChanges();
        const items = spectator.queryAll('.dot-nav__list-item');

        items.forEach((item) => {
            expect(item.classList.contains('dot-nav__list-item--active')).toBe(false);
        });
    });

    it('should have dot-nav-item print correctly', () => {
        spectator.detectChanges();

        const items = spectator.queryAll(DotNavItemComponent);

        expect(items.length).toBe(2);
        // Verify the structure, not exact equality since we modified active states
        expect(items[0].data.id).toBe('123');
        expect(items[1].data.id).toBe('456');
    });

    it('should close on document click', () => {
        spectator.detectChanges();

        // Set navigation to collapsed state
        globalStore.collapseNavigation();
        spectator.detectChanges();

        jest.spyOn(globalStore, 'closeAllParents');

        spectator.dispatchMouseEvent(spectator.element, 'click');
        // When collapsed, clicking should close sections
        expect(globalStore.closeAllParents).toHaveBeenCalledTimes(1);
    });

    describe('itemClick event', () => {
        it('should reload portlet and hide overlay', () => {
            spectator.detectChanges();

            const stopPropSpy = jest.fn();

            spectator.component.onItemClick({
                originalEvent: {
                    stopPropagation: stopPropSpy,
                    ctrlKey: false,
                    metaKey: false
                } as unknown as MouseEvent,
                data: dotMenuMock().menuItems[0]
            });

            expect(stopPropSpy).toHaveBeenCalled();
            expect(dotRouterService.reloadCurrentPortlet).toHaveBeenCalledWith('123');
            expect(dotRouterService.reloadCurrentPortlet).toHaveBeenCalledTimes(1);
            expect(iframeOverlayService.hide).toHaveBeenCalledTimes(1);
        });

        it('should NOT reload portlet', () => {
            spectator.detectChanges();

            const stopPropSpy = jest.fn();

            spectator.component.onItemClick({
                originalEvent: {
                    stopPropagation: stopPropSpy,
                    ctrlKey: true,
                    metaKey: false
                } as unknown as MouseEvent,
                data: dotMenuMock().menuItems[0]
            });

            expect(stopPropSpy).toHaveBeenCalled();
            expect(dotRouterService.reloadCurrentPortlet).not.toHaveBeenCalled();
        });

        it('should have scroll', () => {
            spectator.detectChanges();

            expect(spectator.debugElement.styles.cssText).toEqual('overflow-y: auto;');
        });
    });

    describe('menuClick event expanded', () => {
        it('should navigate and set open when menu is closed', () => {
            spectator.detectChanges();

            const mockMenu = globalStore.menuGroup().find((m) => m.id === '123');
            if (mockMenu) {
                mockMenu.isOpen = false;
            }

            if (mockMenu) {
                spectator.component.onMenuClick({
                    originalEvent: {} as unknown as MouseEvent,
                    data: mockMenu
                });
            }

            expect(dotRouterService.gotoPortlet).toHaveBeenCalledWith('url/one', {}, '123');
            expect(dotRouterService.gotoPortlet).toHaveBeenCalledTimes(1);
            // Verify parent menu group is set as open in GlobalStore
            expect(globalStore.openParentMenuId()).toBe('123');
        });

        it('should only set open when menu is already open', () => {
            spectator.detectChanges();

            // First, open the parent menu group
            globalStore.toggleParent('123');
            expect(globalStore.openParentMenuId()).toBe('123');

            const mockMenu = globalStore.menuGroup().find((m) => m.id === '123');

            spectator.component.onMenuClick({
                originalEvent: {} as unknown as MouseEvent,
                data: mockMenu
            });

            expect(dotRouterService.gotoPortlet).not.toHaveBeenCalled();
            // Verify parent menu group is toggled (closed) in GlobalStore
            expect(globalStore.openParentMenuId()).toBe(null);
        });
    });

    describe('collapse button', () => {
        it('should toggle navigation when collapse button is clicked', () => {
            spectator.detectChanges();

            spectator.component.handleCollapseButtonClick();

            expect(dotEventsService.notify).toHaveBeenCalledWith('dot-side-nav-toggle');
            expect(dotEventsService.notify).toHaveBeenCalledTimes(1);
        });
    });
});
