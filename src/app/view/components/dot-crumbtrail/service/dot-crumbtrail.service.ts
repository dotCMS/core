import { Injectable } from '@angular/core';
import { DotNavigationService, replaceSectionsMap } from '../../dot-navigation/services/dot-navigation.service';
import { map, switchMap, filter, take } from 'rxjs/operators';
import { NavigationEnd, Router, ActivatedRoute, Data } from '@angular/router';
import { DotMenu, DotMenuItem } from '../../../../shared/models/navigation';
import { Observable } from 'rxjs/Observable';
import { Subject } from 'rxjs/Subject';
import { BehaviorSubject } from 'rxjs/BehaviorSubject';

@Injectable()
export class DotCrumbtrailService {
    private URL_EXCLUDES = ['/content-types-angular/create/content'];
    private crumbTrail: Subject<DotCrumb[]> = new BehaviorSubject([]);

    private dataMatch = {
        'content-types-angular': 'contentType.name',
        'edit-page': 'content.page.title'
    };

    constructor(
        public dotNavigationService: DotNavigationService,
        router: Router,
        private activeRoute: ActivatedRoute
    ) {
        this.dotNavigationService.onNavigationEnd().pipe(
            map((event: NavigationEnd) => event.url),
            filter((url: string) => !this.URL_EXCLUDES.includes(url)),
            switchMap(this.getCrumbtrail.bind(this))
        ).subscribe((crumbTrail: DotCrumb[]) => this.crumbTrail.next(crumbTrail));

        this.getCrumbtrail(router.url).subscribe((crumbTrail: DotCrumb[]) => this.crumbTrail.next(crumbTrail));
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
                                    url:  `#/${menuItem.menuLink}`
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
        this.dataMatch[sectionKey].split('.')
            .forEach(key => currentData = currentData[key]);

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
                if (sections.length > 1 ) {
                    crumbTrail.push({
                        label: this.getCrumbtrailSection(sections[0]),
                        url: ''
                    });
                }

                return crumbTrail;
            })
        );
    }
}

export interface DotCrumb {
    label: string;
    url: string;
}
