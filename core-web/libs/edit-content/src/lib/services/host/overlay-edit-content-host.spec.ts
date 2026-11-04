import { createServiceFactory, mockProvider, SpectatorService } from '@openng/spectator/vitest';
import { Mock, vi } from 'vitest';

import { Injector } from '@angular/core';

import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotCMSContentlet } from '@dotcms/dotcms-models';
import { DotSidePanelRef } from '@dotcms/ui';
import { createFakeContentlet } from '@dotcms/utils-testing';

import { EditContentHost } from './edit-content-host.model';
import { OverlayEditContentHost } from './overlay-edit-content-host';

import { DotEditContentRequest } from '../../models/dot-edit-content-request.model';
import { DotOpenContentEvent } from '../../models/dot-open-content-event.model';
import {
    DotEditContentOpenerService,
    DotEditContentOpenOptions
} from '../../opener/dot-edit-content-opener.service';
import { DotRelatedContentNavigationStore } from '../../store/dot-related-content-navigation.store';

/**
 * Asks a host to open content, as a field would (#38303). Fails with a clear message while the
 * host has no `openContent`.
 *
 * @param host The host under test.
 * @param event What the field hands it.
 */
const openContent = (host: EditContentHost, event: DotOpenContentEvent): void => {
    const withOpenContent = host as EditContentHost & {
        openContent?: (event: DotOpenContentEvent) => void;
    };

    expect(typeof withOpenContent.openContent, 'the host can open content').toBe('function');
    withOpenContent.openContent?.(event);
};

describe('OverlayEditContentHost', () => {
    let spectator: SpectatorService<OverlayEditContentHost>;
    let host: OverlayEditContentHost;
    let relatedNav: { appendToTrail: Mock; titleCache: Mock; registerTitle: Mock };
    let config: DynamicDialogConfig;

    const createHost = createServiceFactory({
        service: OverlayEditContentHost,
        providers: [
            mockProvider(DotRelatedContentNavigationStore, {
                appendToTrail: vi.fn().mockReturnValue(['inode-a', 'inode-b']),
                titleCache: vi.fn().mockReturnValue({ 'inode-a': 'A', 'inode-b': 'B' }),
                registerTitle: vi.fn()
            }),
            mockProvider(DynamicDialogConfig, { data: undefined }),
            mockProvider(DynamicDialogRef, { close: vi.fn() }),
            mockProvider(DotEditContentOpenerService, {
                open: vi.fn(() => new DotSidePanelRef(() => undefined))
            })
        ]
    });

    beforeEach(() => {
        spectator = createHost();
        host = spectator.service;
        // Shared across tests (providers are evaluated once).
        (spectator.inject(DotEditContentOpenerService).open as Mock).mockClear();
        relatedNav = spectator.inject(
            DotRelatedContentNavigationStore
        ) as unknown as typeof relatedNav;
        config = spectator.inject(DynamicDialogConfig);
    });

    it('reports that it navigates in place', () => {
        expect(host.inPlaceNavigation).toBe(true);
        expect(host.inPlaceNavigation$).toBeDefined();
    });

    it('starts with an empty per-instance trail (does not touch the shared store)', () => {
        expect(host.trail()).toEqual([]);
    });

    describe('resolveIdentity', () => {
        it('returns empty when the dialog config has no data', () => {
            config.data = undefined;
            expect(host.resolveIdentity()).toEqual({
                inode: undefined,
                contentTypeId: undefined
            });
        });

        // The UVE opens the editor on an experiment variant (#38303, FR-022).
        it('carries the experiment variant from the dialog config', () => {
            config.data = { contentletInode: 'inode-9', variantName: 'variant-a' };
            expect(host.resolveIdentity()).toEqual(
                expect.objectContaining({ inode: 'inode-9', variantName: 'variant-a' })
            );
        });

        it('maps contentletInode and contentTypeId from the dialog config', () => {
            config.data = { contentletInode: 'inode-9', contentTypeId: 'Blog' };
            expect(host.resolveIdentity()).toEqual({
                inode: 'inode-9',
                contentTypeId: 'Blog'
            });
        });

        // Content Drive starts a create in the language the list is filtered by (#37759, FR-003).
        it('carries the language the opener asked the content to start in', () => {
            config.data = { mode: 'new', contentTypeId: 'Blog', languageId: 2 };

            expect(host.resolveIdentity()).toEqual(
                expect.objectContaining({ contentTypeId: 'Blog', languageId: 2 })
            );
        });
    });

    describe('leaveDeletedContent', () => {
        it('closes a DialogService dialog through its ref', () => {
            host.leaveDeletedContent('SimpleWidget');

            expect(spectator.inject(DynamicDialogRef).close).toHaveBeenCalled();
        });

        it('emits left$ so overlays without a dialog ref (side panel) can close', () => {
            const leftSpy = vi.fn();
            host.left$.subscribe(leftSpy);

            host.leaveDeletedContent('SimpleWidget');

            expect(leftSpy).toHaveBeenCalledTimes(1);
        });

        it('stops reporting saves once the content is deleted', () => {
            const savedSpy = vi.fn();
            host.saved$.subscribe(savedSpy);

            host.leaveDeletedContent('SimpleWidget');
            host.reportSaved({ inode: 'x', title: 't' } as DotCMSContentlet);

            expect(savedSpy).not.toHaveBeenCalled();
        });
    });

    describe('saved$', () => {
        it('emits the contentlet reported via reportSaved', () =>
            new Promise<void>((done) => {
                const contentlet = { inode: 'x', title: 't' } as DotCMSContentlet;
                host.saved$.subscribe((c) => {
                    expect(c).toBe(contentlet);
                    done();
                });

                host.reportSaved(contentlet);
            }));
    });

    // Chrome intents overlay another context, so they are safe no-ops.
    it('treats title/breadcrumb as no-ops', () => {
        expect(() => host.setContentTitle('x')).not.toThrow();
        expect(() => host.addBreadcrumb({ label: 'x', url: '/y' })).not.toThrow();
    });

    describe('goToRestoredVersion', () => {
        it('emits an in-place reload for the restored inode (no route to navigate)', () =>
            new Promise<void>((done) => {
                host.inPlaceNavigation$.subscribe((request) => {
                    expect(request).toEqual({ inode: 'inode-restored' });
                    done();
                });

                host.goToRestoredVersion('inode-restored');
            }));
    });

    describe('goToSavedContent', () => {
        it('repoints the trail current crumb to the new inode after a save', () => {
            // A→B trail; save B into a new inode B'. The last crumb must follow.
            host.setTrail(['inode-a', 'inode-b']);

            host.goToSavedContent({ inode: 'inode-b2', title: 'B saved' }, 'inode-b');

            expect(relatedNav.registerTitle).toHaveBeenCalledWith('inode-b2', 'B saved');
            expect(host.trail().map((c) => c.inode)).toEqual(['inode-a', 'inode-b2']);
        });

        it('does nothing when the inode did not change', () => {
            host.setTrail(['inode-a', 'inode-b']);

            host.goToSavedContent({ inode: 'inode-b', title: 'B' }, 'inode-b');

            expect(host.trail().map((c) => c.inode)).toEqual(['inode-a', 'inode-b']);
        });
    });

    describe('goToRelatedContent', () => {
        it('emits a reload request carrying the DEFERRED trail (not committed yet)', () =>
            new Promise<void>((done) => {
                host.inPlaceNavigation$.subscribe((request) => {
                    expect(request).toEqual({ inode: 'inode-b', trail: ['inode-a', 'inode-b'] });
                    // Trail is NOT committed until setTrail is called by the layout.
                    expect(host.trail()).toEqual([]);
                    done();
                });

                host.goToRelatedContent(
                    { inode: 'inode-a', title: 'A' },
                    { inode: 'inode-b', title: 'B' }
                );

                expect(relatedNav.appendToTrail).toHaveBeenCalledWith(
                    [],
                    { inode: 'inode-a', title: 'A' },
                    { inode: 'inode-b', title: 'B' }
                );
            }));
    });

    describe('goToCrumb', () => {
        it('emits a reload request with the trimmed trail (deferred)', () =>
            new Promise<void>((done) => {
                host.inPlaceNavigation$.subscribe((request) => {
                    expect(request).toEqual({ inode: 'inode-a', trail: ['inode-a'] });
                    done();
                });

                host.goToCrumb('inode-a', ['inode-a']);
            }));
    });

    describe('reloadContent (locale switch)', () => {
        it('emits a reload request WITHOUT a trail, carrying the language', () =>
            new Promise<void>((done) => {
                host.inPlaceNavigation$.subscribe((request) => {
                    expect(request).toEqual({ inode: 'inode-5', languageId: 2 });
                    expect(request.trail).toBeUndefined();
                    done();
                });

                host.reloadContent('inode-5', 2);
            }));

        // The layout may still keep the old version (unsaved changes, "keep editing"), so asking
        // for the reload reports nothing yet (#37759, FR-020, T125).
        it('does not report the language when only asked to reload', () => {
            const spy = vi.fn();
            host.languageChanged$.subscribe(spy);

            host.reloadContent('inode-5', 2);

            expect(spy).not.toHaveBeenCalled();
        });
    });

    // The layout reports the language once the reload runs; the opener names it in its URL
    // (#37759, FR-020, FR-028, T125).
    describe('reportLanguage', () => {
        it('emits the language on languageChanged$', () => {
            const spy = vi.fn();
            host.languageChanged$.subscribe(spy);

            host.reportLanguage(2);

            expect(spy).toHaveBeenCalledWith(2);
        });
    });

    describe('setTrail', () => {
        it('commits the trail as this host instance current trail', () => {
            host.setTrail(['inode-a', 'inode-b']);
            expect(host.trail()).toEqual([
                { inode: 'inode-a', title: 'A' },
                { inode: 'inode-b', title: 'B' }
            ]);
        });
    });

    /**
     * A field asks the panel's host to open other content (#38303, R8). Inside a panel, a related
     * item the Relationship field names replaces the panel's content in place, with a breadcrumb;
     * anything else stacks a panel over this one. The discriminator is the event's `source`:
     * `'relationship' | 'block-editor'`.
     */
    describe('openContent', () => {
        /** The opener's `open`, as the mock records it. */
        const opened = () => spectator.inject(DotEditContentOpenerService).open as Mock;

        /** The options the host handed the opener on its only `open`. */
        const openOptions = (): DotEditContentOpenOptions => {
            expect(opened().mock.calls.length, 'the host stacks a panel through the opener').toBe(
                1
            );

            return opened().mock.calls[0][1] as DotEditContentOpenOptions;
        };

        describe('an existing item a Relationship field names', () => {
            it('opens it in place, as related content, and stacks no panel', () => {
                const goToRelatedContent = vi.spyOn(host, 'goToRelatedContent');

                openContent(host, {
                    request: { mode: 'edit', inode: 'inode-b', title: 'B' },
                    source: 'relationship',
                    from: { inode: 'inode-a', title: 'A' },
                    onSaved: vi.fn()
                });

                expect(goToRelatedContent).toHaveBeenCalledTimes(1);
                // The origin is the field's: inside a panel only the field knows what it shows.
                expect(goToRelatedContent.mock.calls[0][0]).toEqual({
                    inode: 'inode-a',
                    title: 'A'
                });
                expect(goToRelatedContent.mock.calls[0][1]).toEqual({
                    inode: 'inode-b',
                    title: 'B'
                });
                expect(opened().mock.calls.length).toBe(0);
            });

            it('starts a fresh trail in place when the field names no origin', () => {
                const goToRelatedContent = vi.spyOn(host, 'goToRelatedContent');
                const goToCrumb = vi.spyOn(host, 'goToCrumb');

                openContent(host, {
                    request: { mode: 'edit', inode: 'inode-b', title: 'B' },
                    source: 'relationship',
                    onSaved: vi.fn()
                });

                expect(goToCrumb).toHaveBeenCalledWith('inode-b', ['inode-b']);
                expect(goToRelatedContent).not.toHaveBeenCalled();
                expect(opened().mock.calls.length).toBe(0);
            });
        });

        describe('content a Relationship field asks to create', () => {
            const REQUEST: DotEditContentRequest = {
                mode: 'new',
                contentType: 'Blog',
                title: 'New Blog',
                relationship: {
                    parentContentletId: 'parent-inode',
                    relationshipName: 'blogs',
                    isParent: true
                }
            };

            it('stacks a panel through the opener with the host injector', () => {
                openContent(host, { request: REQUEST, source: 'relationship', onSaved: vi.fn() });

                expect(opened().mock.calls[0][0]).toEqual(REQUEST);
                // Compared by identity: printing an injector runs the worker out of memory.
                expect(openOptions().injector === spectator.inject(Injector)).toBe(true);
            });

            it('hands every save of the stacked panel to the field', () => {
                const onSaved = vi.fn();
                const contentlet = createFakeContentlet({ identifier: 'created-id' });
                openContent(host, { request: REQUEST, source: 'relationship', onSaved });

                openOptions().saved?.(contentlet);

                expect(onSaved).toHaveBeenCalledWith(contentlet);
            });

            it('never replaces the panel content in place', () => {
                const goToRelatedContent = vi.spyOn(host, 'goToRelatedContent');

                openContent(host, { request: REQUEST, source: 'relationship', onSaved: vi.fn() });

                expect(goToRelatedContent).not.toHaveBeenCalled();
            });
        });

        describe('content a block editor names', () => {
            it('stacks a panel through the opener and never replaces the panel content', () => {
                const goToRelatedContent = vi.spyOn(host, 'goToRelatedContent');
                const request: DotEditContentRequest = {
                    mode: 'edit',
                    inode: 'inode-c',
                    contentType: 'Blog'
                };

                openContent(host, { request, source: 'block-editor' });

                expect(opened().mock.calls[0][0]).toEqual(request);
                expect(openOptions().injector === spectator.inject(Injector)).toBe(true);
                expect(goToRelatedContent).not.toHaveBeenCalled();
            });
        });

        // Only an explicit Relationship edit replaces in place; anything unnamed stacks, which
        // never discards the panel's own content.
        describe('an edit that names no source', () => {
            it('stacks a panel through the opener', () => {
                const goToRelatedContent = vi.spyOn(host, 'goToRelatedContent');
                const request: DotEditContentRequest = { mode: 'edit', inode: 'inode-d' };

                openContent(host, { request });

                expect(opened().mock.calls[0][0]).toEqual(request);
                expect(goToRelatedContent).not.toHaveBeenCalled();
            });
        });
    });
});

describe('OverlayEditContentHost without a dialog ref (side panel injector)', () => {
    const createHostWithoutRef = createServiceFactory({
        service: OverlayEditContentHost,
        providers: [
            mockProvider(DotRelatedContentNavigationStore, {
                appendToTrail: vi.fn(),
                titleCache: vi.fn().mockReturnValue({}),
                registerTitle: vi.fn()
            }),
            mockProvider(DynamicDialogConfig, { data: undefined }),
            mockProvider(DotEditContentOpenerService)
        ]
    });

    it('still emits left$ and does not throw', () => {
        const hostWithoutRef = createHostWithoutRef().service;
        const leftSpy = vi.fn();
        hostWithoutRef.left$.subscribe(leftSpy);

        expect(() => hostWithoutRef.leaveDeletedContent('SimpleWidget')).not.toThrow();
        expect(leftSpy).toHaveBeenCalledTimes(1);
    });
});
