import { Spectator, SpyObject, createComponentFactory, mockProvider } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';

import { TooltipModule } from 'primeng/tooltip';

import { DotSystemConfigService } from '@dotcms/data-access';
import { LoginService } from '@dotcms/dotcms-js';
import { GlobalStore } from '@dotcms/store';
import { DotIconModule } from '@dotcms/ui';
import { LoginServiceMock } from '@dotcms/utils-testing';

import { DotNavIconModule } from './components/dot-nav-icon/dot-nav-icon.module';
import { DotNavItemComponent } from './components/dot-nav-item/dot-nav-item.component';
import { DotSubNavComponent } from './components/dot-sub-nav/dot-sub-nav.component';
import { DotNavigationComponent } from './dot-navigation.component';
import { DotNavigationService } from './services/dot-navigation.service';
import { dotMenuMock, dotMenuMock1 } from './services/dot-navigation.service.spec';

import { DotMenuService } from '../../../api/services/dot-menu.service';
import { DotRandomIconPipeModule } from '../../pipes/dot-radom-icon/dot-random-icon.pipe.module';
import { IframeOverlayService } from '../_common/iframe/service/iframe-overlay.service';

describe('DotNavigationComponent collapsed', () => {
    let spectator: Spectator<DotNavigationComponent>;
    let navigationService: SpyObject<DotNavigationService>;
    let iframeOverlayService: SpyObject<IframeOverlayService>;

    const createComponent = createComponentFactory({
        component: DotNavigationComponent,
        entryComponents: [DotSubNavComponent, DotNavItemComponent],
        imports: [
            DotNavIconModule,
            DotIconModule,
            BrowserAnimationsModule,
            TooltipModule,
            DotRandomIconPipeModule
        ],
        providers: [
            provideRouter([]),
            DotMenuService,
            mockProvider(IframeOverlayService),
            mockProvider(DotNavigationService, {
                items$: of([dotMenuMock(), dotMenuMock1()]),
                collapsed$: of(true)
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

        navigationService = spectator.inject(DotNavigationService);
        iframeOverlayService = spectator.inject(IframeOverlayService);
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
        expect(items[0].data).toEqual(dotMenuMock());
        expect(items[1].data).toEqual(dotMenuMock1());
    });

    it('should close on document click', () => {
        spectator.detectChanges();

        spectator.dispatchMouseEvent(spectator.element, 'click');
        expect(navigationService.closeAllSections).toHaveBeenCalledTimes(1);
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
            expect(navigationService.reloadCurrentPortlet).toHaveBeenCalledWith('123');
            expect(navigationService.reloadCurrentPortlet).toHaveBeenCalledTimes(1);
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
            expect(navigationService.reloadCurrentPortlet).not.toHaveBeenCalled();
        });
    });

    describe('menuClick event collapsed', () => {
        it('should navigate to portlet when menu is collapsed', () => {
            spectator.detectChanges();

            spectator.component.onMenuClick({
                originalEvent: {} as unknown as MouseEvent,
                data: dotMenuMock()
            });

            expect(navigationService.goTo).toHaveBeenCalledWith('url/link1');
            expect(navigationService.goTo).toHaveBeenCalledTimes(1);
        });

        it('should not have scroll', () => {
            spectator.detectChanges();

            spectator.component.onMenuClick({
                originalEvent: {} as unknown as MouseEvent,
                data: dotMenuMock()
            });

            expect(spectator.debugElement.styles.cssText).toEqual('');
        });
    });
});

describe('DotNavigationComponent expanded', () => {
    let spectator: Spectator<DotNavigationComponent>;
    let navigationService: SpyObject<DotNavigationService>;
    let iframeOverlayService: SpyObject<IframeOverlayService>;

    const createComponent = createComponentFactory({
        component: DotNavigationComponent,
        entryComponents: [DotSubNavComponent, DotNavItemComponent],
        imports: [
            DotNavIconModule,
            DotIconModule,
            BrowserAnimationsModule,
            TooltipModule,
            DotRandomIconPipeModule
        ],
        providers: [
            provideRouter([]),
            DotMenuService,
            mockProvider(IframeOverlayService),
            mockProvider(DotNavigationService, {
                items$: of([dotMenuMock(), dotMenuMock1()]),
                collapsed$: of(false)
            }),
            {
                provide: LoginService,
                useClass: LoginServiceMock
            }
        ]
    });

    beforeEach(() => {
        spectator = createComponent({
            detectChanges: false
        });

        navigationService = spectator.inject(DotNavigationService);
        iframeOverlayService = spectator.inject(IframeOverlayService);
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
        expect(items[0].data).toEqual(dotMenuMock());
        expect(items[1].data).toEqual(dotMenuMock1());
    });

    it('should close on document click', () => {
        spectator.detectChanges();

        spectator.dispatchMouseEvent(spectator.element, 'click');
        expect(navigationService.closeAllSections).not.toHaveBeenCalledTimes(1);
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
            expect(navigationService.reloadCurrentPortlet).toHaveBeenCalledWith('123');
            expect(navigationService.reloadCurrentPortlet).toHaveBeenCalledTimes(1);
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
            expect(navigationService.reloadCurrentPortlet).not.toHaveBeenCalled();
        });

        it('should have scroll', () => {
            spectator.detectChanges();

            expect(spectator.debugElement.styles.cssText).toEqual('overflow-y: auto;');
        });
    });

    describe('menuClick event expanded', () => {
        it('should navigate and set open when menu is closed', () => {
            spectator.detectChanges();

            const mockMenu = {
                ...dotMenuMock(),
                isOpen: false
            };

            spectator.component.onMenuClick({
                originalEvent: {} as unknown as MouseEvent,
                data: mockMenu
            });

            expect(navigationService.goTo).toHaveBeenCalledWith('url/link1');
            expect(navigationService.goTo).toHaveBeenCalledTimes(1);
            expect(navigationService.setOpen).toHaveBeenCalledWith('123');
            expect(navigationService.setOpen).toHaveBeenCalledTimes(1);
        });

        it('should only set open when menu is already open', () => {
            spectator.detectChanges();

            const mockMenu = {
                ...dotMenuMock(),
                isOpen: true
            };

            spectator.component.onMenuClick({
                originalEvent: {} as unknown as MouseEvent,
                data: mockMenu
            });

            expect(navigationService.goTo).not.toHaveBeenCalled();
            expect(navigationService.setOpen).toHaveBeenCalledWith('123');
            expect(navigationService.setOpen).toHaveBeenCalledTimes(1);
        });
    });

    describe('collapse button', () => {
        it('should toggle navigation when collapse button is clicked', () => {
            spectator.detectChanges();

            spectator.component.handleCollapseButtonClick();

            expect(navigationService.toggle).toHaveBeenCalledTimes(1);
        });
    });
});
