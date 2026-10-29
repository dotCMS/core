import { createComponentFactory, mockProvider, Spectator } from '@openng/spectator/vitest';
import { MockComponent } from 'ng-mocks';
import { Subject } from 'rxjs';
import { Mock, vi } from 'vitest';

import { reflectComponentType, signal } from '@angular/core';

import { DialogService } from 'primeng/dynamicdialog';

import { DotMessageService } from '@dotcms/data-access';
import { DotCMSContentlet } from '@dotcms/dotcms-models';
import { DotSidePanelRef } from '@dotcms/ui';
import { createFakeContentlet } from '@dotcms/utils-testing';

import { DotEditContentSidePanelComponent } from './dot-edit-content-side-panel.component';

import {
    AngularImageEditorLauncher,
    IMAGE_EDITOR_LAUNCHER
} from '../../fields/shared/image-editor-launcher';
import { DotEditContentRequest } from '../../models/dot-edit-content-request.model';
import { DotEditContentOpenerService } from '../../opener/dot-edit-content-opener.service';
import { OverlayEditContentHost } from '../../services/host/overlay-edit-content-host';
import { DotRelatedContentNavigationStore } from '../../store/dot-related-content-navigation.store';
import { DotEditContentLayoutComponent } from '../dot-edit-content-layout/dot-edit-content.layout.component';

/**
 * A stand-in for the form bridge's global stack (#38303, US6). Each push opens a numbered frame and
 * returns it; a pop removes the frame it names, or the top one when it names none, as the real
 * LIFO stack does. `frames` is what is still open, the active one last.
 */
const formBridge = vi.hoisted(() => {
    const frames: number[] = [];
    let last = 0;

    return {
        frames,
        reset: () => {
            frames.length = 0;
            last = 0;
        },
        push: vi.fn(() => {
            last += 1;
            frames.push(last);

            return last;
        }),
        pop: vi.fn((frame?: number) => {
            const index = frame === undefined ? frames.length - 1 : frames.indexOf(frame);

            if (index >= 0) {
                frames.splice(index, 1);
            }
        })
    };
});

vi.mock('@dotcms/edit-content-bridge', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@dotcms/edit-content-bridge')>()),
    pushFormBridge: formBridge.push,
    popFormBridge: formBridge.pop
}));

/** The panel's ref, reduced to what the content uses. */
const panelRef = () => ({
    close: vi.fn(),
    contentStyleClass: signal('p-0!'),
    closed: new Subject<void>()
});

describe('DotEditContentSidePanelComponent', () => {
    let spectator: Spectator<DotEditContentSidePanelComponent>;
    let saved$: Subject<DotCMSContentlet>;
    let left$: Subject<void>;
    let languageChanged$: Subject<number>;
    let panel: { close: Mock; closed: Subject<void> };

    const EDIT_REQUEST: DotEditContentRequest = {
        mode: 'edit',
        inode: 'inode-1',
        identifier: 'id-1',
        contentType: 'Blog',
        title: 'My Content',
        variantName: 'variant-a'
    };

    const createComponent = createComponentFactory({
        component: DotEditContentSidePanelComponent,
        // Swap the heavy editor for a stub; feed a mock host so we control `saved$`.
        overrideComponents: [
            [
                DotEditContentSidePanelComponent,
                {
                    set: {
                        imports: [MockComponent(DotEditContentLayoutComponent)],
                        providers: [{ provide: OverlayEditContentHost, useValue: undefined }]
                    }
                }
            ]
        ],
        providers: [mockProvider(DotMessageService, { get: (key: string) => key })]
    });

    beforeEach(() => {
        saved$ = new Subject<DotCMSContentlet>();
        left$ = new Subject<void>();
        languageChanged$ = new Subject<number>();
        panel = panelRef();

        spectator = createComponent({
            providers: [
                {
                    provide: OverlayEditContentHost,
                    useValue: {
                        saved$: saved$.asObservable(),
                        left$: left$.asObservable(),
                        languageChanged$: languageChanged$.asObservable()
                    }
                },
                { provide: DotSidePanelRef, useValue: panel }
            ],
            detectChanges: false
        });
    });

    /** Opens the panel on a request and lets the host subscriptions (afterNextRender) run. */
    const openRequest = async (request: DotEditContentRequest = EDIT_REQUEST) => {
        spectator.setInput('request', request);
        spectator.detectChanges();
        await spectator.fixture.whenStable();
    };

    /** Makes the editor's unsaved-changes guard answer as the author would. */
    const guardAnswers = (answer: 'proceed' | 'keep') =>
        vi
            .spyOn(spectator.query(DotEditContentLayoutComponent)!, 'confirmClose')
            .mockImplementation((onProceed: () => void) => {
                if (answer === 'proceed') {
                    onProceed();
                }
            });

    it('should route a close request through the editor guard and close the panel when it proceeds', async () => {
        await openRequest();
        const confirmClose = guardAnswers('proceed');

        spectator.component.requestClose();

        expect(confirmClose).toHaveBeenCalledWith(expect.any(Function));
        expect(panel.close).toHaveBeenCalledTimes(1);
    });

    it('should keep the panel open when the editor guard cancels (unsaved changes kept)', async () => {
        await openRequest();
        guardAnswers('keep');

        spectator.component.requestClose();

        expect(panel.close).not.toHaveBeenCalled();
    });

    it('should forward the host `saved$` stream to the `saved` output', async () => {
        await openRequest();
        const savedSpy = vi.fn();
        spectator.output('saved').subscribe(savedSpy);

        const contentlet = { inode: 'inode-1' } as DotCMSContentlet;
        saved$.next(contentlet);

        expect(savedSpy).toHaveBeenCalledWith(contentlet);
    });

    it('should forward the host language switch to the `languageChanged` output', async () => {
        await openRequest();
        const languageSpy = vi.fn();
        spectator.output('languageChanged').subscribe(languageSpy);

        languageChanged$.next(2);

        expect(languageSpy).toHaveBeenCalledWith(2);
    });

    /**
     * #38303 (layer 6) — the panel takes a request and reports through its outputs only. The
     * dialog-era `data` input, the only way `onContentSaved` and `onCancel` reached the panel, is
     * gone with the dialog-era data type.
     */
    describe('without dialog data', () => {
        it('should take a request and no data input', () => {
            const inputs = reflectComponentType(DotEditContentSidePanelComponent)?.inputs.map(
                ({ propName }) => propName
            );

            expect(inputs).toContain('request');
            expect(inputs).not.toContain('data');
        });

        it('should report each save, in order, as it happens', async () => {
            await openRequest();
            const saved = vi.fn();
            spectator.output('saved').subscribe(saved);
            const first = createFakeContentlet({ inode: 'inode-2' });
            const second = createFakeContentlet({ inode: 'inode-3' });

            saved$.next(first);
            saved$.next(second);

            expect(saved.mock.calls).toEqual([[first], [second]]);
        });

        it('should report the close once, with no last-saved payload', async () => {
            await openRequest();
            const closed = vi.fn();
            spectator.output('closed').subscribe(closed);
            saved$.next(createFakeContentlet({ inode: 'inode-2' }));
            guardAnswers('proceed');

            spectator.component.requestClose();
            panel.closed.next();
            panel.closed.complete();

            expect(closed.mock.calls).toEqual([[undefined]]);
        });
    });

    /**
     * The one-request contract (#38303). The panel takes a `DotEditContentRequest`, reports a
     * delete and a close as events, and hands the overlay host an identity built from the request.
     */
    describe('with a request', () => {
        it('renders the editor once the request is set', async () => {
            spectator.setInput('request', null);
            spectator.detectChanges();
            expect(spectator.query(DotEditContentLayoutComponent)).toBeNull();

            await openRequest();

            expect(spectator.query(DotEditContentLayoutComponent)).not.toBeNull();
        });

        it('reports deleted and then closes, without the guard, when the host says the content left', async () => {
            await openRequest();
            const deleted = vi.fn();
            const saved = vi.fn();
            spectator.output('deleted').subscribe(deleted);
            spectator.output('saved').subscribe(saved);
            const confirmClose = vi.spyOn(
                spectator.query(DotEditContentLayoutComponent)!,
                'confirmClose'
            );

            left$.next();

            expect(deleted).toHaveBeenCalledTimes(1);
            expect(saved).not.toHaveBeenCalled();
            expect(confirmClose).not.toHaveBeenCalled();
            expect(panel.close).toHaveBeenCalledTimes(1);
        });

        it('reports closed once when the panel closes, for any reason', async () => {
            await openRequest();
            const closed = vi.fn();
            spectator.output('closed').subscribe(closed);

            panel.closed.next();
            panel.closed.complete();

            expect(closed).toHaveBeenCalledTimes(1);
        });

        it('reports every save, so the opener refreshes while the panel stays open', async () => {
            await openRequest();
            const saved = vi.fn();
            spectator.output('saved').subscribe(saved);

            saved$.next(createFakeContentlet({ inode: 'inode-2' }));
            saved$.next(createFakeContentlet({ inode: 'inode-3' }));

            expect(saved).toHaveBeenCalledTimes(2);
        });

        it('keeps the panel open after a save', async () => {
            await openRequest();

            saved$.next(createFakeContentlet({ inode: 'inode-2' }));

            expect(panel.close).not.toHaveBeenCalled();
        });
    });
});

/**
 * Regression lock for #37398, as its own top-level suite on purpose.
 *
 * The suite above overrides this component with `set: { providers: [...] }`, which REPLACES the
 * component's own `providers` — so `IMAGE_EDITOR_LAUNCHER` would be absent there for a reason that
 * has nothing to do with the component. This factory overrides only `imports` (stubbing the heavy
 * layout), leaving the real provider list intact, so the assertion is about the component rather
 * than about the test setup.
 *
 * `DotFileFieldComponent` injects the token as `{ optional: true }`, so a host losing this provider
 * does not fail loudly — Image/File fields quietly open the legacy Dojo editor instead of the new
 * one. This turns that into a test failure rather than a user-visible downgrade.
 */
describe('DotEditContentSidePanelComponent — image editor host capability', () => {
    let spectator: Spectator<DotEditContentSidePanelComponent>;

    const createComponent = createComponentFactory({
        component: DotEditContentSidePanelComponent,
        overrideComponents: [
            [
                DotEditContentSidePanelComponent,
                {
                    remove: { imports: [DotEditContentLayoutComponent] },
                    add: { imports: [MockComponent(DotEditContentLayoutComponent)] }
                }
            ]
        ],
        providers: [mockProvider(DotMessageService, { get: (key: string) => key })]
    });

    beforeEach(() => {
        spectator = createComponent({
            providers: [
                {
                    provide: OverlayEditContentHost,
                    useValue: { saved$: new Subject(), left$: new Subject<void>() }
                },
                { provide: DotSidePanelRef, useValue: panelRef() }
            ],
            detectChanges: false
        });
    });

    it('should provide IMAGE_EDITOR_LAUNCHER so fields use the new editor, not legacy Dojo', () => {
        const launcher = spectator.inject(IMAGE_EDITOR_LAUNCHER, true);

        expect(launcher).toBeDefined();
        expect(launcher).toBeInstanceOf(AngularImageEditorLauncher);
    });

    it('should provide the DialogService the launcher opens the editor through', () => {
        expect(spectator.inject(DialogService, true)).toBeTruthy();
    });
});

/**
 * #38303 (US6, plan risk "form bridge LIFO stack") — custom fields reach the form through one global
 * bridge, kept on a stack: each editor panel pushes a frame when it opens and pops it when it
 * closes. A panel stacked over another must get its own frame, give the parent's back when it
 * closes, and keep its own when the parent underneath closes first.
 */
/**
 * #38303 (layer 6) — the editor's identity comes from the request alone. This suite keeps the
 * component's own providers (only the heavy layout is stubbed), so the real `OverlayEditContentHost`
 * resolves the identity exactly as the editor would on construction.
 */
describe('DotEditContentSidePanelComponent — identity from the request', () => {
    let spectator: Spectator<DotEditContentSidePanelComponent>;

    const createComponent = createComponentFactory({
        component: DotEditContentSidePanelComponent,
        overrideComponents: [
            [
                DotEditContentSidePanelComponent,
                {
                    remove: { imports: [DotEditContentLayoutComponent] },
                    add: { imports: [MockComponent(DotEditContentLayoutComponent)] }
                }
            ]
        ],
        providers: [
            mockProvider(DotMessageService, { get: (key: string) => key }),
            // The real host injects both; neither is exercised here.
            mockProvider(DotEditContentOpenerService),
            mockProvider(DotRelatedContentNavigationStore, { titleCache: () => ({}) }),
            { provide: DotSidePanelRef, useFactory: panelRef }
        ],
        detectChanges: false
    });

    /** Mounts the panel on a request and returns what the editor's host resolves from it. */
    const identityFor = (request: DotEditContentRequest) => {
        spectator = createComponent();
        spectator.setInput('request', request);
        spectator.detectChanges();

        return spectator.inject(OverlayEditContentHost, true).resolveIdentity();
    };

    it('should resolve an edit to the inode, the language and the variant the request names', () => {
        expect(
            identityFor({
                mode: 'edit',
                inode: 'inode-1',
                identifier: 'id-1',
                contentType: 'Blog',
                languageId: 3,
                variantName: 'variant-a'
            })
        ).toEqual(
            expect.objectContaining({
                inode: 'inode-1',
                languageId: 3,
                variantName: 'variant-a'
            })
        );
    });

    it('should resolve a create to the content type, the folder and the language it starts in', () => {
        expect(
            identityFor({
                mode: 'new',
                contentType: 'Blog',
                folderPath: '/news/',
                languageId: 2
            })
        ).toEqual(
            expect.objectContaining({
                inode: undefined,
                contentTypeId: 'Blog',
                folderPath: '/news/',
                languageId: 2
            })
        );
    });
});

describe('DotEditContentSidePanelComponent — form bridge frames when panels stack', () => {
    const createComponent = createComponentFactory({
        component: DotEditContentSidePanelComponent,
        overrideComponents: [
            [
                DotEditContentSidePanelComponent,
                {
                    set: {
                        imports: [MockComponent(DotEditContentLayoutComponent)],
                        providers: [
                            {
                                provide: OverlayEditContentHost,
                                useFactory: () => ({
                                    saved$: new Subject<DotCMSContentlet>(),
                                    left$: new Subject<void>(),
                                    languageChanged$: new Subject<number>()
                                })
                            }
                        ]
                    }
                }
            ]
        ],
        providers: [
            mockProvider(DotMessageService, { get: (key: string) => key }),
            { provide: DotSidePanelRef, useFactory: panelRef }
        ],
        detectChanges: false
    });

    /** Opens an editor panel, as the outlet does when the opener asks for one. */
    const openPanel = (): Spectator<DotEditContentSidePanelComponent> => {
        const panel = createComponent();
        panel.setInput('request', { mode: 'edit', inode: 'inode-1', contentType: 'Blog' });
        panel.detectChanges();

        return panel;
    };

    /** The frame the push at `index` opened (0 for the first panel, 1 for the stacked one). */
    const frameOf = (index: number): number => formBridge.push.mock.results[index].value as number;

    beforeEach(() => {
        formBridge.reset();
        formBridge.push.mockClear();
        formBridge.pop.mockClear();
    });

    it('should give a panel its own frame while it is open', () => {
        openPanel();

        expect(formBridge.push.mock.calls.length).toBe(1);
        expect(formBridge.frames).toEqual([frameOf(0)]);
    });

    it('should push a second frame for a panel stacked on top', () => {
        openPanel();
        openPanel();

        expect(formBridge.push.mock.calls.length).toBe(2);
        expect(formBridge.frames).toEqual([frameOf(0), frameOf(1)]);
    });

    it("should give the parent's frame back when the stacked panel closes", () => {
        openPanel();
        const stacked = openPanel();

        stacked.fixture.destroy();

        expect(formBridge.pop.mock.calls.length).toBe(1);
        expect(formBridge.frames).toEqual([frameOf(0)]);
    });

    // The out-of-order close: the panel underneath goes first. Popping the top frame here would
    // take the stacked panel's bridge and leave its custom fields on a destroyed one.
    it('should keep the stacked panel its own frame when the parent underneath closes first', () => {
        const parent = openPanel();
        openPanel();

        parent.fixture.destroy();

        expect(formBridge.pop.mock.calls.length).toBe(1);
        expect(formBridge.frames).toEqual([frameOf(1)]);
    });

    it('should leave no frame behind once both panels have closed, in either order', () => {
        const parent = openPanel();
        const stacked = openPanel();

        parent.fixture.destroy();
        stacked.fixture.destroy();

        expect(formBridge.pop.mock.calls.length).toBe(2);
        expect(formBridge.frames).toEqual([]);
    });
});
