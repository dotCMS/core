import { Spectator, SpyObject, createComponentFactory } from '@ngneat/spectator';
import { BehaviorSubject, Observable } from 'rxjs';

import { Injectable } from '@angular/core';

import { BreadcrumbModule, Breadcrumb } from 'primeng/breadcrumb';

import { DotCrumbtrailComponent } from './dot-crumbtrail.component';
import { DotCrumb, DotCrumbtrailService } from './service/dot-crumbtrail.service';

@Injectable()
class MockDotCrumbtrailService {
    private crumbTrail = new BehaviorSubject([
        {
            label: 'label',
            url: 'url'
        }
    ]);

    get crumbTrail$(): Observable<DotCrumb[]> {
        return this.crumbTrail.asObservable();
    }
}

const mockDotCrumbtrailService = new MockDotCrumbtrailService();

describe('DotCrumbtrailComponent', () => {
    let spectator: Spectator<DotCrumbtrailComponent>;
    let dotCrumbtrailService: SpyObject<DotCrumbtrailService>;

    const createComponent = createComponentFactory({
        component: DotCrumbtrailComponent,
        imports: [BreadcrumbModule],
        componentProviders: [
            {
                provide: DotCrumbtrailService,
                useValue: mockDotCrumbtrailService
            }
        ],
        detectChanges: false
    });

    beforeEach(() => {
        spectator = createComponent();
        dotCrumbtrailService = spectator.inject(DotCrumbtrailService, true);
    });

    it('should has a p-breadcrumb', () => {
        spectator.detectChanges();
        const pBreadCrumb = spectator.query(Breadcrumb);
        expect(pBreadCrumb).not.toBeNull();
    });

    it('should listen crumbTrail event from service', () => {
        spectator.detectChanges();

        const pBreadCrumb = spectator.query(Breadcrumb);

        dotCrumbtrailService.crumbTrail$.subscribe((crumbs) => {
            expect(pBreadCrumb.model).toBe(crumbs);
        });
    });
});
