import { Component } from '@angular/core';
import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';
import { TabViewModule } from 'primeng/tabview';
import { MockResizeObserver } from '../../utils/mocks';
import { TabViewInsertDirective } from './tab-view-insert.directive';

// Mock component
@Component({
    template: `
        <p-tabView>
            <ng-template
                dotTabViewInsert
                [dotTabViewPrepend]="prependContent"
                [dotTabViewAppend]="appendContent"></ng-template>
            <p-tabPanel header="Tab 1">Content 1</p-tabPanel>
            <p-tabPanel header="Tab 2">Content 2</p-tabPanel>
        </p-tabView>

        <ng-template #prependContent>
            <div data-testid="prepend-content">Prepend Content</div>
        </ng-template>

        <ng-template #appendContent>
            <div data-testid="append-content">Append Content</div>
        </ng-template>
    `
})
class TestComponent {}

describe('TabViewInsertDirective', () => {
    let spectator: Spectator<TestComponent>;
    const createComponent = createComponentFactory({
        component: TestComponent,
        imports: [TabViewModule, TabViewInsertDirective]
    });

    beforeEach(() => {
        window.ResizeObserver = MockResizeObserver;
        spectator = createComponent();
    });

    afterEach(() => {
        delete (window as any).ResizeObserver;
    });

    it('should prepend content', () => {
        spectator.detectChanges();
        const prependContent = spectator.query('[data-testid="prepend-content"]');
        expect(prependContent).toHaveText('Prepend Content');
    });

    it('should append content', () => {
        spectator.detectChanges();
        const appendContent = spectator.query('[data-testid="append-content"]');
        expect(appendContent).toHaveText('Append Content');
    });

    it('should position prepend content before tabs', () => {
        const tabViewNavContent = spectator.query('.p-tabview-nav-content');
        const prependContent = spectator.query('[data-testid="prepend-content"]');
        expect(tabViewNavContent?.firstChild).toBe(prependContent);
        expect(prependContent?.previousElementSibling).toBeNull();
    });

    it('should position append content after tabs', () => {
        const tabViewNavContent = spectator.query('.p-tabview-nav-content');
        const appendContent = spectator.query('[data-testid="append-content"]');
        expect(tabViewNavContent?.lastChild).toBe(appendContent);
    });
});
