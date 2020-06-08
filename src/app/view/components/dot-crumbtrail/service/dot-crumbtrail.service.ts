import { Injectable } from '@angular/core';
import {
    DotNavigationService,
    replaceSectionsMap
} from '../../dot-navigation/services/dot-navigation.service';
import { map, switchMap, filter, take } from 'rxjs/operators';
import { NavigationEnd, Router, ActivatedRoute, Data } from '@angular/router';
import { DotMenu, DotMenuItem } from '../../../../shared/models/navigation';
import { Observable, BehaviorSubject, Subject } from 'rxjs';

@Injectable()
export class DotCrumbtrailService {
    private URL_EXCLUDES = ['/content-types-angular/create/content'];
    private crumbTrail: Subject<DotCrumb[]> = new BehaviorSubject([]);

    private portletsTitlePathFinder = {
        'content-types-angular': 'contentType.name',
        'edit-page': 'content.page.title',
        apps: 'data.app.name'
    };

    constructor(
        public dotNavigationService: DotNavigationService,
        router: Router,
        private activeRoute: ActivatedRoute
    ) {
        this.dotNavigationService
            .onNavigationEnd()
            .pipe(
                map((event: NavigationEnd) => {
                    if (this.URL_EXCLUDES.includes(event.url)) {
                        return this.splitURL(event.url)[0];
                    } else {
                        return event.url;
                    }
                }),
                switchMap(this.getCrumbtrail.bind(this))
            )
            .subscribe((crumbTrail: DotCrumb[]) => this.crumbTrail.next(crumbTrail));

        this.getCrumbtrail(router.url).subscribe((crumbTrail: DotCrumb[]) =>
            this.crumbTrail.next(crumbTrail)
        );
    }

    get crumbTrail$(): Observable<DotCrumb[]> {
        return this.crumbTrail.asObservable();
    }

    private splitURL(url: string): string[] {
        return url.split('/').filter((section: string) => section !== '' && section !== 'c');
    }

    private getMenuLabel(portletId: string): Observable<DotCrumb[]> {
        return this.dotNavigationService.items$.pipe(
            filter((dotMenus: DotMenu[]) => !!dotMenus.length),
            map((dotMenus: DotMenu[]) => {
                let res: DotCrumb[] = [];

                dotMenus.forEach((menu: DotMenu) => {
                    menu.menuItems.forEach((menuItem: DotMenuItem) => {
                        if (menuItem.id === portletId) {
                            res = [
                                {
                                    label: menu.name,
                                    url: `#/${menu.menuItems[0].menuLink}`
                                },
                                {
                                    label: menuItem.label,
                                    url: `#/${menuItem.menuLink}`
                                }
                            ];
                        }
                    });
                });

                return res;
            }),
            take(1)
        );
    }

    private getCrumbtrailSection(sectionKey: string): string {
        const data: Data = this.getData();

        let currentData: any = data;

        this.portletsTitlePathFinder[sectionKey]
            .split('.')
            .forEach((key) => (currentData = currentData[key]));
        return currentData;
    }

    private getData(): Data {
        let data = {};
        let lastChild = this.activeRoute.root;

        do {
            lastChild = lastChild.firstChild;
            data = Object.assign(data, lastChild.data['value']);
        } while (lastChild.firstChild !== null);

        return data;
    }

    private getCrumbtrail(url: string): Observable<DotCrumb[]> {
        const sections: string[] = this.splitURL(url);
        const portletId = replaceSectionsMap[sections[0]] || sections[0];

        return this.getMenuLabel(portletId).pipe(
            map((crumbTrail: DotCrumb[]) => {
                if (sections.length > 1 && this.isPortletTitleAvailable(url)) {
                    const sectionLabel = this.getCrumbtrailSection(sections[0]);

                    crumbTrail.push({
                        label: sectionLabel,
                        url: ''
                    });
                }

                return crumbTrail;
            })
        );
    }

    private isPortletTitleAvailable(url: string): boolean {
        const sections: string[] = this.splitURL(url);
        return !!this.portletsTitlePathFinder[sections[0]];
    }
}

export interface DotCrumb {
    label: string;
    url: string;
}
