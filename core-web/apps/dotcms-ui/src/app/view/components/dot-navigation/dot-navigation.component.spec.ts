/* eslint-disable @typescript-eslint/no-empty-function */

import { BehaviorSubject, Observable } from 'rxjs';

import { DebugElement } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { NavigationEnd } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';

import { TooltipModule } from 'primeng/tooltip';

import { skip } from 'rxjs/operators';

import { IframeOverlayService } from '@components/_common/iframe/service/iframe-overlay.service';
import { DotMenuService } from '@dotcms/app/api/services/dot-menu.service';
import { LoginService } from '@dotcms/dotcms-js';
import { DotMenu } from '@dotcms/dotcms-models';
import { DotIconModule } from '@dotcms/ui';
import { LoginServiceMock } from '@dotcms/utils-testing';
import { DotRandomIconPipeModule } from '@pipes/dot-radom-icon/dot-random-icon.pipe.module';

import { DotNavIconModule } from './components/dot-nav-icon/dot-nav-icon.module';
import { DotNavItemComponent } from './components/dot-nav-item/dot-nav-item.component';
import { DotSubNavComponent } from './components/dot-sub-nav/dot-sub-nav.component';
import { DotNavigationComponent } from './dot-navigation.component';
import { DotNavigationService } from './services/dot-navigation.service';
import { dotMenuMock, dotMenuMock1 } from './services/dot-navigation.service.spec';

class FakeNavigationService {
    _routeEvents: BehaviorSubject<NavigationEnd> = new BehaviorSubject(
        new NavigationEnd(0, '', '')
    );
    _items$: BehaviorSubject<DotMenu[]> = new BehaviorSubject([dotMenuMock(), dotMenuMock1()]);

    private _collapsed$: BehaviorSubject<boolean> = new BehaviorSubject(false);
    get items$(): Observable<DotMenu[]> {
        return this._items$.asObservable();
    }

    get collapsed$(): BehaviorSubject<boolean> {
        return this._collapsed$;
    }

    onNavigationEnd(): Observable<NavigationEnd> {
        return this._routeEvents.asObservable().pipe(skip(1));
    }

    setOpen() {
        this._items$.next([
            {
                ...dotMenuMock(),
                isOpen: true
            },
            dotMenuMock1()
        ]);
    }

    expandMenu() {
        this._items$.next([
            {
                ...dotMenuMock(),
                isOpen: true,
                menuItems: [
                    {
                        ...dotMenuMock().menuItems[0],
                        active: true
                    },
                    {
                        ...dotMenuMock().menuItems[1],
                        active: false
                    }
                ]
            },
            dotMenuMock1()
        ]);
    }

    triggerOnNavigationEnd(url?: string) {
        return this._routeEvents.next(new NavigationEnd(0, url || '/url/789', url || '/url/789'));
    }

    goTo() {}
    reloadCurrentPortlet() {}
    closeAllSections() {}
}

describe('DotNavigationComponent', () => {
    let fixture: ComponentFixture<DotNavigationComponent>;
    let de: DebugElement;
    let navItem: DebugElement;

    let dotNavigationService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [DotNavigationComponent, DotSubNavComponent, DotNavItemComponent],
            imports: [
                DotNavIconModule,
                DotIconModule,
                RouterTestingModule,
                BrowserAnimationsModule,
                TooltipModule,
                DotRandomIconPipeModule
            ],
            providers: [
                DotMenuService,
                IframeOverlayService,
                {
                    provide: DotNavigationService,
                    useClass: FakeNavigationService
                },
                {
                    provide: LoginService,
                    useClass: LoginServiceMock
                }
            ]
        }).compileComponents();

        fixture = TestBed.createComponent(DotNavigationComponent);
        de = fixture.debugElement;

        dotNavigationService = de.injector.get(DotNavigationService);

        spyOn(dotNavigationService, 'goTo');
        spyOn(dotNavigationService, 'reloadCurrentPortlet');
        spyOn(dotNavigationService, 'setOpen').and.callThrough();

        fixture.detectChanges();
        navItem = de.query(By.css('dot-nav-item'));
    });

    it('should have all menus closed', () => {
        const items: DebugElement[] = de.queryAll(By.css('.dot-nav__list-item'));

        items.forEach((item: DebugElement) => {
            expect(item.nativeElement.classList.contains('dot-nav__list-item--active')).toBe(false);
        });
    });

    it('should have dot-nav-item print correctly', () => {
        const items: DebugElement[] = de.queryAll(By.css('dot-nav-item'));
        expect(items.length).toBe(2);
        expect(items[0].componentInstance.data).toEqual(dotMenuMock());
        expect(items[1].componentInstance.data).toEqual(dotMenuMock1());
    });

    it('should close on document click', () => {
        const collapsed$: BehaviorSubject<boolean> = new BehaviorSubject(true);
        spyOnProperty(dotNavigationService, 'collapsed$', 'get').and.returnValue(collapsed$);
        spyOn(dotNavigationService, 'closeAllSections');
        document.dispatchEvent(new MouseEvent('click'));
        expect(dotNavigationService.closeAllSections).toHaveBeenCalledTimes(1);
    });

    describe('itemClick event', () => {
        let stopProp;
        let iframeOverlayService: IframeOverlayService;

        beforeEach(() => {
            stopProp = jasmine.createSpy('stopProp');
            iframeOverlayService = de.injector.get(IframeOverlayService);
            spyOn(iframeOverlayService, 'hide');
        });

        it('should reload portlet and hide overlay', () => {
            navItem.triggerEventHandler('itemClick', {
                originalEvent: {
                    stopPropagation: stopProp,
                    ctrlKey: false,
                    metaKey: false
                },
                data: dotMenuMock()
            });

            expect(stopProp).toHaveBeenCalledTimes(1);
            expect(dotNavigationService.reloadCurrentPortlet).toHaveBeenCalledWith('123');
            expect(iframeOverlayService.hide).toHaveBeenCalledTimes(1);
        });

        it('should NOT reload portlet', () => {
            navItem.triggerEventHandler('itemClick', {
                originalEvent: {
                    stopPropagation: stopProp,
                    ctrlKey: true,
                    metaKey: false
                },
                data: dotMenuMock()
            });

            expect(stopProp).toHaveBeenCalledTimes(1);
            expect(dotNavigationService.reloadCurrentPortlet).not.toHaveBeenCalled();
        });
    });

    describe('menuClick event ', () => {
        describe('collapsed', () => {
            beforeEach(() => {
                const collapsed$: BehaviorSubject<boolean> = new BehaviorSubject(true);
                spyOnProperty(dotNavigationService, 'collapsed$', 'get').and.returnValue(
                    collapsed$
                );
                navItem.triggerEventHandler('menuClick', {
                    originalEvent: {},
                    data: dotMenuMock()
                });
                fixture.detectChanges();
            });

            it('should navigate to portlet when menu is collapsed', () => {
                expect(dotNavigationService.goTo).toHaveBeenCalledWith('url/link1');
            });

            it('should not have scroll', () => {
                expect(fixture.debugElement.styles.cssText).toEqual('');
            });
        });

        describe('expanded', () => {
            beforeEach(() => {
                const collapsed$: BehaviorSubject<boolean> = new BehaviorSubject(false);
                spyOnProperty(dotNavigationService, 'collapsed$', 'get').and.returnValue(
                    collapsed$
                );
                navItem.triggerEventHandler('menuClick', {
                    originalEvent: {},
                    data: dotMenuMock()
                });
                fixture.detectChanges();
            });

            it('should expand menu', () => {
                const firstItem: DebugElement = de.query(By.css('.dot-nav__list-item'));
                expect(
                    firstItem.nativeElement.classList.contains('dot-nav__list-item--active')
                ).toBe(true);
                const firstMenuLink: DebugElement = firstItem.query(By.css('.dot-nav-sub__link'));
                expect(
                    firstMenuLink.nativeElement.classList.contains('dot-nav-sub__link--active')
                ).toBe(false);
            });

            it('should NOT navigate to porlet', () => {
                expect(dotNavigationService.goTo).not.toHaveBeenCalled();
            });

            it('should have scroll', () => {
                expect(fixture.debugElement.styles.cssText).toEqual('overflow-y: auto;');
            });
        });
    });
});
