import { ComponentFixture } from '@angular/core/testing';
import { DebugElement, Component } from '@angular/core';
import { DOTTestBed } from '../../../test/dot-test-bed';
import { By } from '@angular/platform-browser';
import { RouterTestingModule } from '@angular/router/testing';
import { of } from 'rxjs/observable/of';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { Router, NavigationEnd } from '@angular/router';

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


export const dotMenuMock: DotMenu = {
    active: true,
    id: '123',
    isOpen: false,
    menuItems: [
        {
            active: false,
            ajax: true,
            angular: true,
            id: '123',
            label: 'Label 1',
            url: 'url/one',
            menuLink: 'url/link1'
        },
        {
            active: true,
            ajax: true,
            angular: true,
            id: '456',
            label: 'Label 2',
            url: 'url/two',
            menuLink: 'url/link2'
        }
    ],
    name: 'Menu 1',
    tabDescription: 'Description',
    tabIcon: 'icon',
    tabName: 'Name',
    url: '/url/index'
};

const dotMenuMock1: DotMenu = {
    ...dotMenuMock,
    active: false,
    id: '456',
    name: 'Menu 2',
    url: '/url/456',
    menuItems: [
        {
            ...dotMenuMock.menuItems[0],
            active: false,
            id: '789'
        },
        {
            ...dotMenuMock.menuItems[1],
            active: false,
            id: '000'
        }
    ]
};

const data = [dotMenuMock, dotMenuMock1];

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

    get items$(): Observable<DotMenu[]> {
        return of([...data]);
    }

    onNavigationEnd(): Observable<NavigationEnd> {
        return this._routeEvents.asObservable().pipe(skip(1));
    }

    triggerOnNavigationEnd() {
        return this._routeEvents.next(new NavigationEnd(0, '/url/789', '/url/789'));
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

        hostFixture.detectChanges();
        navItem = compDe.query(By.css('dot-nav-item'));
    });

    it('should have dot-nav-item print correctly', () => {
        const items: DebugElement[] = compDe.queryAll(By.css('dot-nav-item'));
        expect(items.length).toBe(2);

        items.forEach((item: DebugElement, i: number) => {
            expect(item.componentInstance.data).toEqual(data[i]);
        });
    });

    it('should handle menuClick event', () => {
        comp.collapsed = false;
        let changeResult = false;

        comp.change.subscribe((e) => {
            changeResult = true;
        });

        expect(comp.menu.map((menuItem: DotMenu) => menuItem.isOpen)).toEqual([false, false]);

        navItem.triggerEventHandler('menuClick', { originalEvent: {}, data: data[0] });

        expect(changeResult).toBe(true);
        expect(comp.menu.map((menuItem: DotMenu) => menuItem.isOpen)).toEqual([true, false]);
        expect(dotNavigationService.goTo).not.toHaveBeenCalled();
    });

    it('should navigate to portlet', () => {
        navItem.triggerEventHandler('menuClick', { originalEvent: {}, data: data[0] });
        expect(dotNavigationService.goTo).toHaveBeenCalledWith('url/link1');
    });

    it('should reload portlet', () => {
        const stopProp = jasmine.createSpy();

        navItem.triggerEventHandler('itemClick', {
            originalEvent: {
                stopPropagation: stopProp,
                ctrlKey: false,
                metaKey: false
            },
            data: data[0]
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
            data: data[0]
        });

        expect(stopProp).toHaveBeenCalledTimes(1);
        expect(dotNavigationService.reloadCurrentPortlet).not.toHaveBeenCalled();
    });

    it('should expand active menu on uncollapsed', () => {
        expect(comp.menu[0].active).toBe(true);
        expect(comp.menu[0].isOpen).toBe(false);

        hostComp.toggleCollapsed();
        hostFixture.detectChanges();

        expect(comp.menu[0].active).toBe(true);
        expect(comp.menu[0].isOpen).toBe(true);
    });

    it('should active and open the correct menu and menu links when navigation end', () => {
        expect(comp.menu[0].active).toBe(true);
        expect(comp.menu[0].isOpen).toBe(false);
        expect(comp.menu[0].menuItems[0].active).toBe(false);
        expect(comp.menu[0].menuItems[1].active).toBe(true);

        expect(comp.menu[1].active).toBe(false);
        expect(comp.menu[1].isOpen).toBe(false);
        expect(comp.menu[1].menuItems[0].active).toBe(false);
        expect(comp.menu[1].menuItems[1].active).toBe(false);

        dotNavigationService.triggerOnNavigationEnd();

        expect(comp.menu[0].active).toBe(false);
        expect(comp.menu[0].isOpen).toBe(false);
        expect(comp.menu[0].menuItems[0].active).toBe(false);
        expect(comp.menu[0].menuItems[1].active).toBe(false);

        expect(comp.menu[1].active).toBe(true);
        expect(comp.menu[1].isOpen).toBe(true);
        expect(comp.menu[1].menuItems[0].active).toBe(true);
        expect(comp.menu[1].menuItems[1].active).toBe(false);
    });
});
