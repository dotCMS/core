import { DOTTestBed } from '../../../../test/dot-test-bed';
import { DotCrumbtrailService } from './dot-crumbtrail.service';
import { Injectable } from '@angular/core';
import { DotNavigationService } from '../../dot-navigation/services/dot-navigation.service';
import { Router, ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs/Observable';
import { Subject } from 'rxjs/Subject';
import { DotMenu } from '../../../../shared/models/navigation';

@Injectable()
class MockDotNavigationService {
    readonly navigationEnd: Subject<Event> = new Subject();

    onNavigationEnd(): Observable<Event> {
        return this.navigationEnd.asObservable();
    }

    get items$(): Observable<DotMenu[]> {
        return Observable.of([
            {
                active: false,
                id: 'menu',
                isOpen: false,
                menuItems: [
                    {
                        active: false,
                        ajax: false,
                        angular: false,
                        id: 'first_porlet',
                        label: 'First Potlet Label',
                        url: '/url/fisrt_portet',
                        menuLink: '/menulink/first_portet'
                    },
                    {
                        active: false,
                        ajax: false,
                        angular: false,
                        id: 'porlet',
                        label: 'Potlet Label',
                        url: '/url/portet',
                        menuLink: '/menulink/portet'
                    }
                ],
                name: 'menu',
                tabDescription: '',
                tabIcon: '',
                tabName: 'Menu Label',
                url: '/url/menu'
            }
        ]);
    }
}

@Injectable()
class MockRouter {
    url = '/c/embedded-dashboard';
}

@Injectable()
class MockActivatedRoute {}

describe('DotCrumbtrailService', () => {
    const dotNavigationServiceMock: MockDotNavigationService = new MockDotNavigationService();
    const mockRouter = new MockRouter();
    const mockActivatedRoute = new MockActivatedRoute();

    let service: DotCrumbtrailService;

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
    });

    it('should take the current url', () => {
        service.crumbTrail$.subscribe((crumbs) => {
            console.log('CRUMBS', crumbs);
        });
    });
});
