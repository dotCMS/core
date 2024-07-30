import { byTestId, createRoutingFactory, Spectator } from '@ngneat/spectator';

import { ActivatedRoute } from '@angular/router';

import { ButtonModule } from 'primeng/button';
import { MenuModule } from 'primeng/menu';

import { DotCollapseBreadcrumbComponent } from './dot-collapse-breadcrumb.component';
import { MAX_ITEMS } from './dot-collapse-breadcrumb.costants';

describe('DotCollapseBreadcrumbComponent', () => {
    let spectator: Spectator<DotCollapseBreadcrumbComponent>;

    const createComponent = createRoutingFactory({
        component: DotCollapseBreadcrumbComponent,
        providers: [ActivatedRoute],
        imports: [MenuModule, ButtonModule]
    });

    beforeEach(() => {
        spectator = createComponent();
    });

    it('should be created', () => {
        spectator.detectChanges();
        expect(spectator.component).toBeTruthy();
    });

    describe('MaxItems', () => {
        it('should have the default', () => {
            spectator.detectChanges();
            expect(spectator.component.$maxItems()).toBe(MAX_ITEMS);
        });

        it('should show the options without btn collapse', () => {
            spectator.setInput('maxItems', 5);
            spectator.setInput('model', [
                { label: 'Electronics', url: '' },
                { label: 'Computer', url: '' },
                { label: 'Accessories', url: '' },
                { label: 'Keyboard', url: '' },
                { label: 'Wireless', url: '' }
            ]);
            spectator.detectChanges();
            expect(spectator.query(byTestId('btn-collapse'))).toBeNull();
            expect(spectator.queryAll('.p-menuitem-link').length).toBe(5);
        });

        it('should show maxItems options with btn collapse', () => {
            spectator.setInput('maxItems', 4);
            spectator.setInput('model', [
                { label: 'Electronics', url: '' },
                { label: 'Computer', url: '' },
                { label: 'Accessories', url: '' },
                { label: 'Keyboard', url: '' },
                { label: 'Wireless', url: '' }
            ]);
            spectator.detectChanges();
            expect(spectator.query(byTestId('btn-collapse'))).toBeTruthy();
            expect(spectator.queryAll('.p-menuitem-link').length).toBe(4);
        });
    });

    it('should call itemClick when click home ', () => {
        spectator.setInput('maxItems', 5);
        spectator.setInput('model', [
            { label: 'Electronics', url: '' },
            { label: 'Computer', url: '' },
            { label: 'Accessories', url: '' },
            { label: 'Keyboard', url: '' },
            { label: 'Wireless', url: '' }
        ]);
        spectator.detectChanges();

        const itemClickSpy = spyOn(spectator.component.onItemClick, 'emit');
        const firstEl = spectator.query('.p-menuitem-link');
        spectator.click(firstEl);
        spectator.detectChanges();

        expect(itemClickSpy).toHaveBeenCalled();
    });

    it('should call itemClick when click home with routerLink', () => {
        spectator.setInput('maxItems', 5);
        spectator.setInput('model', [
            { label: 'Electronics', routerLink: '/' },
            { label: 'Computer', routerLink: '/' },
            { label: 'Accessories', routerLink: '/' },
            { label: 'Keyboard', routerLink: '/' },
            { label: 'Wireless', routerLink: '/' }
        ]);
        spectator.detectChanges();

        const itemClickSpy = spyOn(spectator.component.onItemClick, 'emit');
        const firstEl = spectator.query('.p-menuitem-link');
        spectator.click(firstEl);
        spectator.detectChanges();

        expect(itemClickSpy).toHaveBeenCalled();
    });
});
