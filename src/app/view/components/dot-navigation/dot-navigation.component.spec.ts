import { ComponentFixture } from '@angular/core/testing';
import { DebugElement } from '@angular/core';
import { DOTTestBed } from '../../../test/dot-test-bed';
import { By } from '@angular/platform-browser';
import { RouterTestingModule } from '@angular/router/testing';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { NavigationEnd } from '@angular/router';

import { DotNavigationComponent } from './dot-navigation.component';
import { DotNavIconModule } from './components/dot-nav-icon/dot-nav-icon.module';
import { DotNavigationService } from './services/dot-navigation.service';
import { DotMenuService } from '@services/dot-menu.service';
import { DotSubNavComponent } from './components/dot-sub-nav/dot-sub-nav.component';
import { DotNavItemComponent } from './components/dot-nav-item/dot-nav-item.component';
import { DotIconModule } from '../_common/dot-icon/dot-icon.module';
import { LoginService } from 'dotcms-js';
import { LoginServiceMock } from '../../../test/login-service.mock';
import { DotMenu } from '@models/navigation';
import { BehaviorSubject, Observable } from 'rxjs';
import { skip } from 'rxjs/operators';
import { dotMenuMock, dotMenuMock1 } from './services/dot-navigation.service.spec';
import { TooltipModule } from 'primeng/primeng';

class FakeNavigationService {
    private _collapsed$: BehaviorSubject<boolean> = new BehaviorSubject(false);

    _routeEvents: BehaviorSubject<NavigationEnd> = new BehaviorSubject(
        new NavigationEnd(0, '', '')
    );
    _items$: BehaviorSubject<DotMenu[]> = new BehaviorSubject([dotMenuMock(), dotMenuMock1()]);

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
}

describe('DotNavigationComponent', () => {
    let fixture: ComponentFixture<DotNavigationComponent>;
    let de: DebugElement;
    let navItem: DebugElement;

    let dotNavigationService;

    beforeEach(() => {
        DOTTestBed.configureTestingModule({
            declarations: [DotNavigationComponent, DotSubNavComponent, DotNavItemComponent],
            imports: [
                DotNavIconModule,
                DotIconModule,
                RouterTestingModule,
                BrowserAnimationsModule,
                TooltipModule
            ],
            providers: [
                DotMenuService,
                {
                    provide: DotNavigationService,
                    useClass: FakeNavigationService
                },
                {
                    provide: LoginService,
                    useClass: LoginServiceMock
                }
            ]
        });

        fixture = DOTTestBed.createComponent(DotNavigationComponent);
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

    describe('itemClick event', () => {
        let stopProp;

        beforeEach(() => {
            stopProp = jasmine.createSpy('stopProp');
        });

        it('should reload portlet', () => {
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

            it('should set tooltip properties', () => {
                fixture.detectChanges();
                expect(navItem.attributes['ng-reflect-disabled']).toBe('false');
                expect(navItem.attributes['ng-reflect-text']).toBe(dotMenuMock().tabName);
                expect(navItem.attributes['tooltipStyleClass']).toBe('dot-nav__tooltip');
            });

            it('should navigate to portlet when menu is collapsed', () => {
                expect(dotNavigationService.goTo).toHaveBeenCalledWith('url/link1');
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

            it('should disable tooltip', () => {
                expect(navItem.attributes['ng-reflect-disabled']).toBe('true');
            });
        });
    });
});
