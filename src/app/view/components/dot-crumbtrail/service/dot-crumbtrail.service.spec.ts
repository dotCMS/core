import { DOTTestBed } from '../../../../test/dot-test-bed';
import { DotCrumbtrailService, DotCrumb } from './dot-crumbtrail.service';
import { Injectable } from '@angular/core';
import { DotNavigationService } from '../../dot-navigation/services/dot-navigation.service';
import { Router, ActivatedRoute, NavigationEnd } from '@angular/router';
import { Observable, Subject, of, BehaviorSubject } from 'rxjs';
import { DotMenu } from '../../../../shared/models/navigation';

@Injectable()
class MockDotNavigationService {
    readonly navigationEnd: Subject<NavigationEnd> = new Subject();

    onNavigationEnd(): Observable<NavigationEnd> {
        return this.navigationEnd.asObservable();
    }

    get items$(): Observable<DotMenu[]> {
        return of([
            {
                active: false,
                id: 'menu',
                isOpen: false,
                menuItems: [
                    {
                        active: false,
                        ajax: false,
                        angular: false,
                        id: 'first_portlet',
                        label: 'First Portlet Label',
                        url: '/url/fisrt_portlet',
                        menuLink: 'menulink/first_portlet'
                    },
                    {
                        active: false,
                        ajax: false,
                        angular: false,
                        id: 'portlet',
                        label: 'Potlet Label',
                        url: '/url/portlet',
                        menuLink: 'menulink/portlet'
                    }
                ],
                name: 'menu',
                tabDescription: '',
                tabIcon: '',
                tabName: 'Menu Label',
                url: '/url/menu'
            },
            {
                active: false,
                id: 'menu_2',
                isOpen: false,
                menuItems: [
                    {
                        active: false,
                        ajax: false,
                        angular: false,
                        id: 'content-types-angular',
                        label: 'Content Types',
                        url: '/content-types-angular',
                        menuLink: 'content-types-angular'
                    }
                ],
                name: 'Types & Tag',
                tabDescription: '',
                tabIcon: '',
                tabName: 'Types & Tag',
                url: '/url/menu_2'
            },
            {
                active: false,
                id: 'site',
                isOpen: false,
                menuItems: [
                    {
                        active: false,
                        ajax: false,
                        angular: false,
                        id: 'site-browser',
                        label: 'Browser',
                        url: '/site-browser',
                        menuLink: 'c/site-browser'
                    }
                ],
                name: 'site',
                tabDescription: '',
                tabIcon: '',
                tabName: 'Site',
                url: '/url/menu_3'
            }
        ]);
    }
}

@Injectable()
class MockRouter {
    url = '/portlet';
}

@Injectable()
class MockActivatedRoute {
    root: any;
}

describe('DotCrumbtrailService', () => {
    const dotNavigationServiceMock: MockDotNavigationService = new MockDotNavigationService();
    const mockRouter = new MockRouter();
    const mockActivatedRoute = new MockActivatedRoute();

    let service: DotCrumbtrailService;
    let firstCrumb: DotCrumb[];
    let secondCrumb: DotCrumb[];

    beforeEach(() => {
        const testbed = DOTTestBed.configureTestingModule({
            providers: [
                DotCrumbtrailService,
                {
                    provide: DotNavigationService,
                    useValue: dotNavigationServiceMock
                },
                {
                    provide: Router,
                    useValue: mockRouter
                },
                {
                    provide: ActivatedRoute,
                    useValue: mockActivatedRoute
                }
            ]
        });

        service = testbed.get(DotCrumbtrailService);

        service.crumbTrail$.subscribe((crumbs) => {
            if (!firstCrumb) {
                firstCrumb = crumbs;
            } else {
                secondCrumb = crumbs;
            }
        });
    });

    it('should take the current url from Router', () => {
        expect(firstCrumb).toEqual([
            {
                label: 'menu',
                url: '#/menulink/first_portlet'
            },
            {
                label: 'Potlet Label',
                url: '#/menulink/portlet'
            }
        ]);
    });

    it('Should take url from NavegationEnd event', () => {
        dotNavigationServiceMock.navigationEnd.next({
            url: '/first_portlet',
            urlAfterRedirects: '/first_portlet',
            id: 1
        });

        expect(secondCrumb).toEqual([
            {
                label: 'menu',
                url: '#/menulink/first_portlet'
            },
            {
                label: 'First Portlet Label',
                url: '#/menulink/first_portlet'
            }
        ]);
    });

    it('Should ignore c prefix', () => {
        dotNavigationServiceMock.navigationEnd.next({
            url: '/first_portlet',
            urlAfterRedirects: '/first_portlet',
            id: 1
        });

        expect(secondCrumb).toEqual([
            {
                label: 'menu',
                url: '#/menulink/first_portlet'
            },
            {
                label: 'First Portlet Label',
                url: '#/menulink/first_portlet'
            }
        ]);
    });

    it('Should exclude URL', () => {
        dotNavigationServiceMock.navigationEnd.next({
            url: '/content-types-angular/create/content',
            urlAfterRedirects: '/content-types-angular/create/content',
            id: 1
        });

        expect(secondCrumb).toEqual([
            {
                label: 'Types & Tag',
                url: '#/content-types-angular'
            },
            {
                label: 'Content Types',
                url: '#/content-types-angular'
            }
        ]);
    });

    it('Should take content types data', () => {
        mockActivatedRoute.root = {
            firstChild: {
                data: new BehaviorSubject({}),
                firstChild: {
                    data: new BehaviorSubject({}),
                    firstChild: {
                        firstChild: null,
                        data: new BehaviorSubject({
                            contentType: {
                                name: 'Content Type Testing'
                            }
                        })
                    }
                }
            }
        };

        dotNavigationServiceMock.navigationEnd.next({
            url: '/content-types-angular/edit/02853fe9-bd7b-48b4-b19d-058b9dad19a8',
            urlAfterRedirects: '/content-types-angular/edit/02853fe9-bd7b-48b4-b19d-058b9dad19a8',
            id: 1
        });

        expect(secondCrumb).toEqual([
            {
                label: 'Types & Tag',
                url: '#/content-types-angular'
            },
            {
                label: 'Content Types',
                url: '#/content-types-angular'
            },
            {
                label: 'Content Type Testing',
                url: ''
            }
        ]);
    });

    it('Should take edit page data', () => {
        mockActivatedRoute.root = {
            firstChild: {
                data: new BehaviorSubject({}),
                firstChild: {
                    data: new BehaviorSubject({}),
                    firstChild: {
                        firstChild: {
                            firstChild: null,
                            data: new BehaviorSubject({})
                        },
                        data: new BehaviorSubject({
                            content: {
                                page: {
                                    title: 'About Us'
                                }
                            }
                        })
                    }
                }
            }
        };

        dotNavigationServiceMock.navigationEnd.next({
            url: '/edit-page/content?url=%2Fabout-us%2Findex&language_id=1',
            urlAfterRedirects: '/edit-page/content?url=%2Fabout-us%2Findex&language_id=1',
            id: 1
        });

        expect(secondCrumb).toEqual([
            {
                label: 'site',
                url: '#/c/site-browser'
            },
            {
                label: 'Browser',
                url: '#/c/site-browser'
            },
            {
                label: 'About Us',
                url: ''
            }
        ]);
    });

    it('Should set DotApps breadcrumb', () => {
        mockActivatedRoute.root = {
            firstChild: {
                data: new BehaviorSubject({}),
                firstChild: {
                    data: new BehaviorSubject({}),
                    firstChild: {
                        firstChild: {
                            firstChild: null,
                            data: new BehaviorSubject({})
                        },
                        data: new BehaviorSubject({
                            data: {
                                name: 'Google Translate'
                            }
                        })
                    }
                }
            }
        };

        dotNavigationServiceMock.navigationEnd.next({
            url: '/apps/google-translate',
            urlAfterRedirects: '/apps/google-translate',
            id: 1
        });

        expect(secondCrumb).toEqual([
            {
                label: 'Google Translate',
                url: ''
            }
        ]);
    });
});
