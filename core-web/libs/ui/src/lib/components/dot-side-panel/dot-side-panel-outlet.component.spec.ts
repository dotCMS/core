import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator
} from '@openng/spectator/vitest';
import { vi } from 'vitest';

import {
    Component,
    EnvironmentInjector,
    InjectionToken,
    Injector,
    createComponent as createAngularComponent,
    createEnvironmentInjector,
    inject,
    input,
    output,
    signal
} from '@angular/core';

import { DotMessageService } from '@dotcms/data-access';
import { DotTitleStatus } from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';

import { DotSidePanelOutletComponent } from './dot-side-panel-outlet.component';

import {
    DotSidePanelContent,
    DotSidePanelRef,
    DotSidePanelService
} from '../../services/dot-side-panel/dot-side-panel.service';

/** A panel's content: an input, an output, its ref, and a guard on close. */
@Component({
    selector: 'dot-test-content',
    template: '<p data-testid="panel-content">{{ fileName() }}</p>'
})
class TestContentComponent implements DotSidePanelContent {
    readonly fileName = input('');
    readonly saved = output<string>();
    readonly panel = inject(DotSidePanelRef);
    readonly requestClose = vi.fn();
}

const ROUTE_SERVICE = new InjectionToken<string>('ROUTE_SERVICE');

/** Content that needs what its opener's route provides, as the Edit Content panel needs the page editor's `MessageService`. */
@Component({
    selector: 'dot-test-route-content',
    template: '<p data-testid="route-content">{{ fromRoute }}</p>'
})
class RouteContentComponent {
    readonly fromRoute = inject(ROUTE_SERVICE);
}

/** An opener the way the router creates one: with its route's injector as its environment. */
@Component({ selector: 'dot-test-opener', template: '' })
class OpenerComponent {
    readonly injector = inject(Injector);
}

describe('DotSidePanelOutletComponent', () => {
    let spectator: Spectator<DotSidePanelOutletComponent>;
    let sidePanels: DotSidePanelService;

    const createComponent = createComponentFactory({
        component: DotSidePanelOutletComponent,
        providers: [
            mockProvider(DotMessageService, { get: (key: string) => key }),
            // Opening a panel collapses the main navigation.
            mockProvider(GlobalStore, {
                isNavigationCollapsed: vi.fn().mockReturnValue(false),
                collapseNavigation: vi.fn(),
                expandNavigation: vi.fn()
            })
        ]
    });

    beforeEach(() => {
        spectator = createComponent();
        sidePanels = spectator.inject(DotSidePanelService);
    });

    // The drawers are appended to the body, so DOM queries search from the document root.
    const all = (testId: string): HTMLElement[] =>
        spectator.queryAll<HTMLElement>(byTestId(testId), { root: true });

    const open = (
        component:
            | TestContentComponentType
            | Promise<TestContentComponentType> = TestContentComponent,
        fileName = signal('header.vtl'),
        saved = vi.fn()
    ): DotSidePanelRef => {
        const ref = sidePanels.open(component, {
            heading: fileName,
            inputs: { fileName },
            outputs: { saved },
            injector: spectator.inject(Injector)
        });
        spectator.detectChanges();

        return ref;
    };

    const content = (): TestContentComponent =>
        spectator.query(TestContentComponent) as TestContentComponent;

    it('renders nothing while no panel is open', () => {
        expect(all('side-panel')).toHaveLength(0);
    });

    it("renders a drawer per open panel, with its heading and its content's inputs", () => {
        const fileName = signal('header.vtl');
        open(TestContentComponent, fileName);
        open(TestContentComponent, signal('footer.vtl'));

        expect(all('side-panel')).toHaveLength(2);
        expect(all('side-panel-title').map((title) => title.textContent?.trim())).toEqual([
            'header.vtl',
            'footer.vtl'
        ]);
        expect(all('panel-content')[0].textContent).toBe('header.vtl');

        fileName.set('renamed.vtl');
        spectator.detectChanges();

        expect(all('side-panel-title')[0].textContent?.trim()).toBe('renamed.vtl');
        expect(all('panel-content')[0].textContent).toBe('renamed.vtl');
    });

    it("gives the content its panel's ref and routes its outputs to the opener", () => {
        const saved = vi.fn();
        const ref = open(TestContentComponent, signal('header.vtl'), saved);

        content().saved.emit('done');

        expect(content().panel).toBe(ref);
        expect(saved).toHaveBeenCalledWith('done');
    });

    it('shows a spinner until lazily loaded content arrives', async () => {
        let resolve: (type: TestContentComponentType) => void = () => undefined;
        open(new Promise((done) => (resolve = done)));

        expect(all('side-panel-loading')).toHaveLength(1);
        expect(all('panel-content')).toHaveLength(0);

        resolve(TestContentComponent);
        await Promise.resolve();
        spectator.detectChanges();

        expect(all('side-panel-loading')).toHaveLength(0);
        expect(all('panel-content')).toHaveLength(1);
    });

    it("sends the close button to the content's guard", () => {
        open();

        spectator.click(all('side-panel-close')[0].querySelector('button') as HTMLElement);

        expect(content().requestClose).toHaveBeenCalled();
        expect(all('side-panel')).toHaveLength(1);
    });

    it('closes only the panel in front on Escape', () => {
        open();
        open(TestContentComponent, signal('footer.vtl'));
        const [behind, inFront] = spectator.queryAll(TestContentComponent);

        document.dispatchEvent(
            new KeyboardEvent('keydown', { key: 'Escape', bubbles: true, cancelable: true })
        );

        expect(inFront.requestClose).toHaveBeenCalledTimes(1);
        expect(behind.requestClose).not.toHaveBeenCalled();
    });

    it("resolves what the opener's route provides", () => {
        const routeInjector = createEnvironmentInjector(
            [{ provide: ROUTE_SERVICE, useValue: 'from the route' }],
            spectator.inject(EnvironmentInjector)
        );
        const opener = createAngularComponent(OpenerComponent, {
            environmentInjector: routeInjector
        });

        sidePanels.open(RouteContentComponent, {
            heading: 'header.vtl',
            injector: opener.instance.injector
        });
        spectator.detectChanges();

        expect(all('route-content')[0]?.textContent).toBe('from the route');

        opener.destroy();
        routeInjector.destroy();
    });

    it('takes the drawer away when the panel closes', () => {
        const ref = open();

        ref.close();
        spectator.detectChanges();

        expect(all('side-panel')).toHaveLength(0);
    });

    it("lays the content out as the content's ref says", () => {
        const ref = open();

        ref.contentStyleClass.set('p-0! flex');
        spectator.detectChanges();

        expect(
            spectator.query('.p-drawer-content', { root: true })?.classList.contains('flex')
        ).toBe(true);
    });

    describe('title status chip', () => {
        it('starts with no status on a new panel ref', () => {
            const ref = open();

            expect(ref.titleStatus()).toBeNull();
            expect(all('title-status-chip')).toHaveLength(0);
        });

        it("shows the chip a content binds on its panel's ref, after that panel's heading", () => {
            const ref = open();

            ref.bindTitleStatus(signal({ state: null }));
            spectator.detectChanges();

            expect(all('title-status-chip')).toHaveLength(1);
            expect(all('title-status-chip')[0].textContent?.trim()).toBe('New');
        });

        it('follows the bound signal and removes the chip when it goes back to null', () => {
            const ref = open();
            const status = signal<DotTitleStatus>({
                state: { live: true, working: true, hasLiveVersion: true }
            });
            ref.bindTitleStatus(status);
            spectator.detectChanges();
            expect(all('title-status-chip')[0].textContent?.trim()).toBe('Published');

            status.set(null);
            spectator.detectChanges();

            expect(all('title-status-chip')).toHaveLength(0);
        });

        it('switches to another source when the content binds a new one', () => {
            const ref = open();
            ref.bindTitleStatus(signal({ state: null }));
            spectator.detectChanges();
            expect(all('title-status-chip')[0].textContent?.trim()).toBe('New');

            ref.bindTitleStatus(
                signal({ state: { live: true, working: true, hasLiveVersion: true } })
            );
            spectator.detectChanges();

            expect(all('title-status-chip')[0].textContent?.trim()).toBe('Published');
        });

        it('leaves other panels without a chip', () => {
            const first = open(TestContentComponent, signal('one.vtl'));
            open(TestContentComponent, signal('two.vtl'));

            first.bindTitleStatus(signal({ state: null }));
            spectator.detectChanges();

            expect(all('title-status-chip')).toHaveLength(1);
            expect(all('side-panel')).toHaveLength(2);
        });
    });
});

type TestContentComponentType = typeof TestContentComponent;
