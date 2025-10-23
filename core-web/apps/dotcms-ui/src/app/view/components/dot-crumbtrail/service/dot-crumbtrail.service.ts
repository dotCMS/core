import { BehaviorSubject, Observable, Subject, merge, of } from 'rxjs';

import { Injectable, inject, computed, signal } from '@angular/core';
import { toObservable } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Data, Event, NavigationEnd, Router } from '@angular/router';

import { MenuItem } from 'primeng/api';

import { filter, map, switchMap, take } from 'rxjs/operators';

import { DotRouterService } from '@dotcms/data-access';
import { DotMenu, DotMenuItem } from '@dotcms/dotcms-models';

import {
    DotNavigationService,
    replaceSectionsMap
} from '../../dot-navigation/services/dot-navigation.service';

@Injectable()
export class DotCrumbtrailService {
    dotNavigationService = inject(DotNavigationService);
    #dotRouterService = inject(DotRouterService);
    private activeRoute = inject(ActivatedRoute);
    private router = inject(Router);
    newBreadcrumb$ = new Subject<MenuItem>();
    $breadcrumbs = signal<MenuItem[]>([]);

    private URL_EXCLUDES = ['/content-types-angular/create/content'];
    private crumbTrail: Subject<DotCrumb[]> = new BehaviorSubject([]);

    private portletsTitlePathFinder = {
        'content-types-angular': 'contentType.name',
        'edit-page': 'content.page.title',
        apps: 'data.name',
        templates: 'template.title'
    };

    readonly $history = this.#dotRouterService.$history;
    history$ = toObservable(this.$history);
    readonly $flattenMenuItems = this.dotNavigationService.$flattenMenuItems;

    readonly $breadcrumbTrail = computed<MenuItem[]>(() => {
        const menu = this.$flattenMenuItems();
        const history = this.$history();
        const mainItemIndex = history.reduce((index, url, currentIndex) => {
            const menuIndex = menu.findIndex((item) => item.menuLink === url);
            if (menuIndex !== -1) {
                return currentIndex;
            }
            return index;
        }, 0);
        const breadcrumbTrail = history.slice(mainItemIndex).reduce((acc, url, index) => {
            const item = menu.find((item) => item.menuLink === url);
            if (item) {
                if (index === 0) {
                    acc.push({
                        label: item?.labelParent,
                        target: '_self',
                        url: `#/${item.menuLink}`
                    });
                }
                acc.push({
                    label: item?.label,
                    target: '_self',
                    url: `#/${item.menuLink}`
                });
            } else {
                if (url.includes('/content?filter=')) {
                    const filter = url.split('/content?filter=')[1];
                    acc.push({
                        label: filter,
                        target: '_self',
                        url: url
                    });
                }
            }
            return acc;
        }, []);
        return breadcrumbTrail;
        /*
        const breadcrumbTrail = history.map(url => {
            const item = menu.find(item => item.menuLink === url);
            return {
                label: item?.labelParent || item?.label,
                target: '_self',
                url: `#/${item.menuLink}`
            };
        });
        */
        return [];
    });

    constructor() {
        merge(
            this.newBreadcrumb$,
            this.router.events.pipe(
                filter((event: Event) => event instanceof NavigationEnd),
                map((event: NavigationEnd) => event.urlAfterRedirects)
            )
        )
            .pipe(
                map((data) => {
                    if (typeof data === 'string') {
                        return {
                            type: 'history',
                            url: data as string
                        };
                    }
                    return {
                        type: 'breadcrumb',
                        item: data as MenuItem
                    };
                })
            )
            .subscribe((data) => {
                const menu = this.$flattenMenuItems();

                if (data.type === 'history') {
                    const item = menu.find((item) => item.menuLink === data.url);
                    if (item) {
                        this.$breadcrumbs.set([
                            {
                                label: item.labelParent,
                                target: '_self'
                            },
                            {
                                label: item.label,
                                target: '_self',
                                url: `/dotAdmin/#/${data.url}`
                            }
                        ]);
                    } else {
                        if (data.url.includes('/content?filter=')) {
                            const filter = data.url.split('/content?filter=')[1];
                            this.$breadcrumbs.update((breadcrumb) => [
                                ...breadcrumb,
                                {
                                    label: filter,
                                    target: '_self',
                                    url: `/dotAdmin/#/${data.url}`
                                }
                            ]);
                        }
                    }
                } else {
                    this.$breadcrumbs.update((breadcrumb) => [...breadcrumb, data.item]);
                }
            });
    }

    get crumbTrail$(): Observable<DotCrumb[]> {
        return this.crumbTrail.asObservable();
    }

    private splitURL(url: string): string[] {
        // Remove query parameters first
        const cleanUrl = this.removeQueryParams(url);

        return cleanUrl.split('/').filter((section: string) => section !== '' && section !== 'c');
    }

    /**
     * Remove query parameters from URL
     * @param url - URL string that may contain query parameters
     * @returns Clean URL without query parameters
     */
    private removeQueryParams(url: string): string {
        // Handle relative URLs by splitting on '?' and taking the first part
        return url.split('?')[0];
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
                                    target: '_self',
                                    url: `#/${menu.menuItems[0].menuLink}`
                                },
                                {
                                    label: menuItem.label,
                                    target: '_self',
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
        const data = this.getData();
        let currentData = data;
        let section = '';

        if (Object.keys(data).length) {
            this.portletsTitlePathFinder[sectionKey].split('.').forEach((key, index, array) => {
                if (index === array.length - 1) {
                    section = currentData[key];
                }

                currentData = currentData[key];
            });

            return section;
        }

        return null;
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
        const sections = this.splitURL(url);
        const portletId = replaceSectionsMap[sections[0]] || sections[0];

        const isEditPage =
            (sections && sections[0] == 'edit-page') || sections[0].includes('edit-ema');

        return this.getMenuLabel(portletId).pipe(
            switchMap(
                (crumbTrail: DotCrumb[]) =>
                    // If it is edit page
                    isEditPage ? this.getPagesCrumbTrail(crumbTrail) : of(crumbTrail) // If it's not edit pages, we return the original breadcrumb
            ),
            map((crumbTrail: DotCrumb[]) => {
                if (this.shouldAddSection(sections, url)) {
                    const sectionLabel = this.getCrumbtrailSection(sections[0]);

                    crumbTrail.push({
                        label: sectionLabel ? sectionLabel : sections[1],
                        target: '_self',
                        url: ''
                    });
                }

                return crumbTrail;
            })
        );
    }

    /**
     * Get the pages crumbtrail.
     * Alternate crumbtrail is used when the page portlet is not enabled.
     *
     * @private
     * @param {DotCrumb[]} alternateCrumbTrail
     * @return {*}  {Observable<DotCrumb[]>}
     * @memberof DotCrumbtrailService
     */
    private getPagesCrumbTrail(alternateCrumbTrail: DotCrumb[] = []): Observable<DotCrumb[]> {
        return this.getMenuLabel('pages').pipe(
            map((pagesCrumbTrail: DotCrumb[]) => {
                // Remove the site-browser from the pages crumbtrail
                const crumbTail = pagesCrumbTrail?.filter(
                    (value) => !value.url.includes('site-browser')
                );

                return crumbTail.length ? crumbTail : alternateCrumbTrail;
            })
        );
    }

    private shouldAddSection(sections: string[], url: string): boolean {
        return sections.length > 1 && this.isPortletTitleAvailable(url);
    }

    private isPortletTitleAvailable(url: string): boolean {
        const sections: string[] = this.splitURL(url);

        return !!this.portletsTitlePathFinder[sections[0]];
    }

    addBreadcrumb(breadcrumb: MenuItem) {
        this.newBreadcrumb$.next(breadcrumb);
    }
}

export interface DotCrumb {
    label: string;
    target?: string;
    url: string;
}
