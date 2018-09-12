import { ComponentFixture } from '@angular/core/testing';
import { DebugElement, Component } from '@angular/core';
import { DOTTestBed } from '../../../test/dot-test-bed';
import { By } from '@angular/platform-browser';
import { RouterTestingModule } from '@angular/router/testing';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { NavigationEnd } from '@angular/router';

import { DotNavigationComponent } from './dot-navigation.component';
import { DotNavIconModule } from './components/dot-nav-icon/dot-nav-icon.module';
import { DotNavigationService } from './services/dot-navigation.service';
import { DotMenuService } from '../../../api/services/dot-menu.service';
import { DotSubNavComponent } from './components/dot-sub-nav/dot-sub-nav.component';
import { DotNavItemComponent } from './components/dot-nav-item/dot-nav-item.component';
import { DotIconModule } from '../_common/dot-icon/dot-icon.module';
import { LoginService } from 'dotcms-js/dotcms-js';
import { LoginServiceMock } from '../../../test/login-service.mock';
import { DotMenu } from '../../../shared/models/navigation';
import { BehaviorSubject, Observable } from 'rxjs';
import { skip } from 'rxjs/operators';
import { dotMenuMock, dotMenuMock1 } from './services/dot-navigation.service.spec';

@Component({
    selector: 'dot-test',
    template: `<dot-main-nav [collapsed]="collapsed"></dot-main-nav>`
})
class HostTestComponent {
    collapsed = true;

    toggleCollapsed(): void {
        this.collapsed = !this.collapsed;
    }
}

class FakeNavigationService {
    _routeEvents: BehaviorSubject<NavigationEnd> = new BehaviorSubject(new NavigationEnd(0, '', ''));
    _items$: BehaviorSubject<DotMenu[]> = new BehaviorSubject([dotMenuMock(), dotMenuMock1()]);

    get items$(): Observable<DotMenu[]> {
        return this._items$.asObservable();
    }

    onNavigationEnd(): Observable<NavigationEnd> {
        return this._routeEvents.asObservable().pipe(skip(1));
    }

    setOpen() {
        this._items$.next([
            {
                ...dotMenuMock(),
                isOpen: true,
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
                        active: true,
                    },
                    {
                        ...dotMenuMock().menuItems[1],
                        active: false,
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
    let hostComp: HostTestComponent;
    let hostFixture: ComponentFixture<HostTestComponent>;
    let hostDe: DebugElement;

    let comp: DotNavigationComponent;
    let compDe: DebugElement;
    let navItem: DebugElement;

    let dotNavigationService;

    beforeEach(() => {
        DOTTestBed.configureTestingModule({
            declarations: [HostTestComponent, DotNavigationComponent, DotSubNavComponent, DotNavItemComponent],
            imports: [DotNavIconModule, DotIconModule, RouterTestingModule, BrowserAnimationsModule],
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

        hostFixture = DOTTestBed.createComponent(HostTestComponent);
        hostDe = hostFixture.debugElement;
        hostComp = hostFixture.componentInstance;

        compDe = hostDe.query(By.css('dot-main-nav'));
        comp = compDe.componentInstance;
        dotNavigationService = compDe.injector.get(DotNavigationService);

        spyOn(dotNavigationService, 'goTo');
        spyOn(dotNavigationService, 'reloadCurrentPortlet');
        spyOn(dotNavigationService, 'setOpen').and.callThrough();

        hostFixture.detectChanges();
        navItem = compDe.query(By.css('dot-nav-item'));
    });

    it('should have all menus closed', () => {
        const items: DebugElement[] = compDe.queryAll(By.css('.dot-nav__list-item'));

        items.forEach((item: DebugElement) => {
            expect(item.nativeElement.classList.contains('dot-nav__list-item--active')).toBe(false);
        });
    });

    it('should have dot-nav-item print correctly', () => {
        const items: DebugElement[] = compDe.queryAll(By.css('dot-nav-item'));
        expect(items.length).toBe(2);
        expect(items[0].componentInstance.data).toEqual(dotMenuMock());
        expect(items[1].componentInstance.data).toEqual(dotMenuMock1());
    });

    describe('menuClick event', () => {
        it('should emit change', () => {
            let changeResult = false;

            comp.change.subscribe((e) => {
                changeResult = true;
            });

            navItem.triggerEventHandler('menuClick', { originalEvent: {}, data: dotMenuMock() });
            expect(changeResult).toBe(true);
        });

        it('should open menu', () => {
            navItem.triggerEventHandler('menuClick', { originalEvent: {}, data: dotMenuMock() });
            expect(dotNavigationService.setOpen).toHaveBeenCalledWith('123');

            hostFixture.detectChanges();

            const firstItem: DebugElement = compDe.query(By.css('.dot-nav__list-item'));
            expect(firstItem.nativeElement.classList.contains('dot-nav__list-item--active')).toBe(true);
        });

        it('should expand menu', () => {
            hostFixture.detectChanges();
            hostComp.collapsed = false;
            hostFixture.detectChanges();

            const firstItem: DebugElement = compDe.query(By.css('.dot-nav__list-item'));
            expect(firstItem.nativeElement.classList.contains('dot-nav__list-item--active')).toBe(true);
            const firstMenuLink: DebugElement = firstItem.query(By.css('.dot-nav-sub__link'));
            expect(firstMenuLink.nativeElement.classList.contains('dot-nav-sub__link--actuve')).toBe(false);
        });

        it('should NOT navigate to porlet', () => {
            hostComp.collapsed = false;
            hostFixture.detectChanges();

            navItem.triggerEventHandler('menuClick', { originalEvent: {}, data: dotMenuMock() });
            expect(dotNavigationService.goTo).not.toHaveBeenCalled();
        });

        it('should navigate to portlet', () => {
            navItem.triggerEventHandler('menuClick', { originalEvent: {}, data: dotMenuMock() });
            hostFixture.detectChanges();

            expect(dotNavigationService.goTo).toHaveBeenCalledWith('url/link1');
        });
    });

    it('should reload portlet', () => {
        const stopProp = jasmine.createSpy();

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
        const stopProp = jasmine.createSpy();

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
