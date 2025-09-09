import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { Component } from '@angular/core';
import { fakeAsync, tick } from '@angular/core/testing';

import { TabViewModule } from 'primeng/tabview';

import { TabViewInsertDirective } from './tab-view-insert.directive';

// Mock component
@Component({
    standalone: false,
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
        spectator = createComponent();
    });
    afterEach(() => {
        delete window.ResizeObserver;
    });

    it('should prepend content', fakeAsync(() => {
        spectator.detectChanges();
        tick();
        const prependContent = spectator.query(byTestId('prepend-content'));
        expect(prependContent).toHaveText('Prepend Content');
    }));

    it('should append content', () => {
        spectator.detectChanges();
        const appendContent = spectator.query(byTestId('append-content'));
        expect(appendContent).toHaveText('Append Content');
    });

    it('should position content blocks correctly within .p-tabview-nav-content', () => {
        const tabViewNavContent = spectator.query('.p-tabview-nav-content');
        const prependContent = spectator.query(byTestId('tabview-prepend-content'));
        const appendContent = spectator.query(byTestId('tabview-append-content'));
        const tabsUl = spectator.query('.p-tabview-nav');

        expect(tabViewNavContent?.children.length).toBe(3);
        expect(tabViewNavContent?.children[0]).toBe(prependContent);
        expect(tabViewNavContent?.children[1]).toBe(tabsUl);
        expect(tabViewNavContent?.children[2]).toBe(appendContent);
    });

    it('should have prepend content as the first child and append content as the last child', () => {
        const tabViewNavContent = spectator.query('.p-tabview-nav-content');
        const prependContent = spectator.query(byTestId('tabview-prepend-content'));
        const appendContent = spectator.query(byTestId('tabview-append-content'));

        expect(tabViewNavContent?.firstElementChild).toBe(prependContent);
        expect(tabViewNavContent?.lastElementChild).toBe(appendContent);
    });
});
