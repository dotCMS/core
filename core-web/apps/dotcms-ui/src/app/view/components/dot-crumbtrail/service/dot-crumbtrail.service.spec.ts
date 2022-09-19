/* eslint-disable @typescript-eslint/no-explicit-any */

import { DotCrumbtrailService, DotCrumb } from './dot-crumbtrail.service';
import { Injectable } from '@angular/core';
import { DotNavigationService } from '../../dot-navigation/services/dot-navigation.service';
import { Router, ActivatedRoute, NavigationEnd } from '@angular/router';
import { Observable, Subject, of, BehaviorSubject } from 'rxjs';
import { DotMenu } from '../../../../shared/models/navigation';
import { TestBed } from '@angular/core/testing';

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
        const testbed = TestBed.configureTestingModule({
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
                target: '_self',
                url: '#/menulink/first_portlet'
            },
            {
                label: 'Potlet Label',
                target: '_self',
                url: '#/menulink/portlet'
            }
        ]);
    });

    it('Should take url from NavegationEnd event', () => {
        const mockNavigationEnd = new NavigationEnd(1, '/first_portlet', '/first_portlet');
        dotNavigationServiceMock.navigationEnd.next(mockNavigationEnd);

        expect(secondCrumb).toEqual([
            {
                label: 'menu',
                target: '_self',
                url: '#/menulink/first_portlet'
            },
            {
                label: 'First Portlet Label',
                target: '_self',
                url: '#/menulink/first_portlet'
            }
        ]);
    });

    it('Should ignore c prefix', () => {
        const mockNavigationEnd = new NavigationEnd(1, '/first_portlet', '/first_portlet');
        dotNavigationServiceMock.navigationEnd.next(mockNavigationEnd);

        expect(secondCrumb).toEqual([
            {
                label: 'menu',
                target: '_self',
                url: '#/menulink/first_portlet'
            },
            {
                label: 'First Portlet Label',
                target: '_self',
                url: '#/menulink/first_portlet'
            }
        ]);
    });

    it('Should exclude URL', () => {
        const mockNavigationEnd = new NavigationEnd(
            1,
            '/content-types-angular/create/content',
            '/content-types-angular/create/content'
        );
        dotNavigationServiceMock.navigationEnd.next(mockNavigationEnd);

        expect(secondCrumb).toEqual([
            {
                label: 'Types & Tag',
                target: '_self',
                url: '#/content-types-angular'
            },
            {
                label: 'Content Types',
                target: '_self',
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

        const mockNavigationEnd = new NavigationEnd(
            1,
            '/content-types-angular/edit/02853fe9-bd7b-48b4-b19d-058b9dad19a8',
            '/content-types-angular/edit/02853fe9-bd7b-48b4-b19d-058b9dad19a8'
        );
        dotNavigationServiceMock.navigationEnd.next(mockNavigationEnd);

        expect(secondCrumb).toEqual([
            {
                label: 'Types & Tag',
                target: '_self',
                url: '#/content-types-angular'
            },
            {
                label: 'Content Types',
                target: '_self',
                url: '#/content-types-angular'
            },
            {
                label: 'Content Type Testing',
                target: '_self',
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

        const mockNavigationEnd = new NavigationEnd(
            1,
            '/edit-page/content?url=%2Fabout-us%2Findex&language_id=1',
            '/edit-page/content?url=%2Fabout-us%2Findex&language_id=1'
        );
        dotNavigationServiceMock.navigationEnd.next(mockNavigationEnd);

        expect(secondCrumb).toEqual([
            {
                label: 'site',
                target: '_self',
                url: '#/c/site-browser'
            },
            {
                label: 'Browser',
                target: '_self',
                url: '#/c/site-browser'
            },
            {
                label: 'About Us',
                target: '_self',
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

        const mockNavigationEnd = new NavigationEnd(
            1,
            '/apps/google-translate',
            '/apps/google-translate'
        );
        dotNavigationServiceMock.navigationEnd.next(mockNavigationEnd);

        expect(secondCrumb).toEqual([
            {
                label: 'Google Translate',
                target: '_self',
                url: ''
            }
        ]);
    });

    it('Should set Templates breadcrumb', () => {
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
                            template: {
                                title: 'Template-01'
                            }
                        })
                    }
                }
            }
        };

        const mockNavigationEnd = new NavigationEnd(
            1,
            'templates/edit/7173cb7a-5d08-4c75-82b3-a7788848c263',
            'templates/edit/7173cb7a-5d08-4c75-82b3-a7788848c263'
        );
        dotNavigationServiceMock.navigationEnd.next(mockNavigationEnd);

        expect(secondCrumb).toEqual([
            {
                label: 'Template-01',
                target: '_self',
                url: ''
            }
        ]);
    });

    it('Should get URL segment if resolver data is not available', () => {
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
                        data: new BehaviorSubject({})
                    }
                }
            }
        };

        const mockNavigationEnd = new NavigationEnd(1, 'templates/new', 'templates/new');
        dotNavigationServiceMock.navigationEnd.next(mockNavigationEnd);

        expect(secondCrumb).toEqual([
            {
                label: 'new',
                target: '_self',
                url: ''
            }
        ]);
    });
});
