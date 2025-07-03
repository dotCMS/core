import { createComponentFactory, Spectator, byTestId } from '@ngneat/spectator';
import { Observable, Subject } from 'rxjs';

import { Injectable } from '@angular/core';

import { DotCollapseBreadcrumbComponent } from '@dotcms/ui';

import { DotCrumbtrailComponent } from './dot-crumbtrail.component';
import { DotCrumb, DotCrumbtrailService } from './service/dot-crumbtrail.service';

@Injectable()
class MockDotCrumbtrailService {
    private crumbTrail: Subject<DotCrumb[]> = new Subject();

    get crumbTrail$(): Observable<DotCrumb[]> {
        return this.crumbTrail.asObservable();
    }

    trigger(crumbs: DotCrumb[]): void {
        this.crumbTrail.next(crumbs);
    }
}

describe('DotCrumbtrailComponent', () => {
    let spectator: Spectator<DotCrumbtrailComponent>;
    const mockService = new MockDotCrumbtrailService();

    const createComponent = createComponentFactory({
        component: DotCrumbtrailComponent,
        imports: [DotCollapseBreadcrumbComponent],
        providers: [
            {
                provide: DotCrumbtrailService,
                useValue: mockService
            }
        ],
        detectChanges: false
    });

    beforeEach(() => {
        spectator = createComponent();
    });

    it('should have breadcrumb parent container', () => {
        spectator.detectChanges();
        const breadcrumbParent = spectator.query(byTestId('breadcrumb-crumbs'));
        expect(breadcrumbParent).toBeTruthy();
    });

    it('should use dot-collapse-breadcrumb component', () => {
        spectator.detectChanges();
        const breadcrumbMenu = spectator.query(DotCollapseBreadcrumbComponent);
        expect(breadcrumbMenu).toBeTruthy();
    });

    it('should have breadcrumb last item container', () => {
        spectator.detectChanges();
        const breadcrumbLast = spectator.query(byTestId('breadcrumb-title'));
        expect(breadcrumbLast).toBeTruthy();
    });

    it('should display collapsed breadcrumbs when multiple items are provided', () => {
        const crumbs = [
            { label: 'First', url: '/first' },
            { label: 'Second', url: '/second' },
            { label: 'Last', url: '/last' }
        ];

        mockService.trigger(crumbs);
        spectator.detectChanges();

        const breadcrumbMenu = spectator.query(DotCollapseBreadcrumbComponent);
        expect(breadcrumbMenu.$model()).toEqual([
            { label: 'First', url: '/first' },
            { label: 'Second', url: '/second' }
        ]);
    });

    it('should display last breadcrumb label when multiple items are provided', () => {
        const crumbs = [
            { label: 'First', url: '/first' },
            { label: 'Second', url: '/second' },
            { label: 'Last', url: '/last' }
        ];

        mockService.trigger(crumbs);
        spectator.detectChanges();

        const breadcrumbLast = spectator.query(byTestId('breadcrumb-title'));
        expect(breadcrumbLast.textContent.trim()).toBe('Last');
    });

    it('should display empty collapsed breadcrumbs when only one item is provided', () => {
        const crumbs = [{ label: 'Single Item', url: '/single' }];

        mockService.trigger(crumbs);
        spectator.detectChanges();

        const breadcrumbMenu = spectator.query(DotCollapseBreadcrumbComponent);
        expect(breadcrumbMenu.$model()).toEqual([]);
    });

    it('should display single item as last breadcrumb when only one item is provided', () => {
        const crumbs = [{ label: 'Single Item', url: '/single' }];

        mockService.trigger(crumbs);
        spectator.detectChanges();

        const breadcrumbLast = spectator.query(byTestId('breadcrumb-title'));
        expect(breadcrumbLast.textContent.trim()).toBe('Single Item');
    });

    it('should display empty last breadcrumb when no items are provided', () => {
        const crumbs: DotCrumb[] = [];

        mockService.trigger(crumbs);
        spectator.detectChanges();

        const breadcrumbLast = spectator.query(byTestId('breadcrumb-title'));
        expect(breadcrumbLast.textContent.trim()).toBe('');
    });

    it('should display empty collapsed breadcrumbs when no items are provided', () => {
        const crumbs: DotCrumb[] = [];

        mockService.trigger(crumbs);
        spectator.detectChanges();

        const breadcrumbMenu = spectator.query(DotCollapseBreadcrumbComponent);
        expect(breadcrumbMenu.$model()).toEqual([]);
    });

    it('should handle breadcrumbs with target and url properties', () => {
        const crumbs = [
            { label: 'First', target: '_self', url: '/first' },
            { label: 'Second', target: '_blank', url: '/second' },
            { label: 'Last', url: '/last' }
        ];

        mockService.trigger(crumbs);
        spectator.detectChanges();

        const breadcrumbMenu = spectator.query(DotCollapseBreadcrumbComponent);
        expect(breadcrumbMenu.$model()).toEqual([
            { label: 'First', target: '_self', url: '/first' },
            { label: 'Second', target: '_blank', url: '/second' }
        ]);

        const breadcrumbLast = spectator.query(byTestId('breadcrumb-last'));
        expect(breadcrumbLast.textContent.trim()).toBe('Last');
    });
});
