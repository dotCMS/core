import { createComponentFactory, Spectator } from '@openng/spectator/vitest';
import { vi } from 'vitest';

import { Injector } from '@angular/core';

import { ConfirmationService } from 'primeng/api';

import { DotMessageService } from '@dotcms/data-access';

import { createTestEditor } from './editor.testing';

import { EditorToolbarStore } from '../components/toolbar/editor-toolbar.store';
import { ToolbarComponent } from '../components/toolbar/toolbar.component';
import { ContentletEditUrlService } from '../services/contentlet-edit-url.service';
import { EditorModalService } from '../services/editor-modal.service';
import { EditorPopoverService } from '../services/editor-popover.service';
import { EditorStore } from '../store/editor.store';

/**
 * Shared harness for "this toolbar button is never gated" specs.
 *
 * These come one per key that #37175 registered unconditionally but left gated on the authoring
 * side — `emoji` (#37340) and `link` (#36351) so far. They differ only in which icon they look
 * for, so the scaffolding lives here instead of being copied per issue.
 *
 * Call it from inside a `describe` — it registers its own `afterEach`.
 */
export function createToolbarHarness(): {
    buildWith: (allowedBlocks: string[] | undefined) => void;
    buttonWithIcon: (icon: string) => Element | undefined;
} {
    let spectator: Spectator<ToolbarComponent>;

    const createComponent = createComponentFactory({
        component: ToolbarComponent,
        shallow: true,
        providers: [
            { provide: DotMessageService, useValue: { get: (key: string) => key } },
            { provide: EditorPopoverService, useValue: { isOpen: () => false, toggle: vi.fn() } },
            { provide: EditorModalService, useValue: {} },
            { provide: ContentletEditUrlService, useValue: {} },
            { provide: ConfirmationService, useValue: {} },
            {
                provide: EditorToolbarStore,
                /**
                 * A Proxy rather than a hand-listed stub. The toolbar reads a wide surface of
                 * mark/block/alignment signals, and enumerating them would make these specs a
                 * maintenance burden for every future toolbar button while testing none of them.
                 * Everything answers "inactive"; the one thing under test is whether a given
                 * button renders at all.
                 */
                useValue: new Proxy(
                    {},
                    {
                        get: (_target, property) =>
                            property === 'connect'
                                ? vi.fn().mockReturnValue(() => undefined)
                                : () => false
                    }
                )
            }
        ]
    });

    /** `undefined` means an unrestricted field; an array is the stored `allowedBlocks` value. */
    const buildWith = (allowedBlocks: string[] | undefined) => {
        spectator = createComponent({
            detectChanges: false,
            providers: [
                {
                    provide: EditorStore,
                    useValue: {
                        // The only store surface the toolbar template reads for gating.
                        isAllowed: (block: string) =>
                            !allowedBlocks || allowedBlocks.includes(block),
                        allowedBlocksSet: () => new Set(allowedBlocks ?? [])
                    }
                }
            ]
        });

        const injector = spectator.inject(Injector);
        spectator.setInput('editor', createTestEditor(injector));
        spectator.detectChanges();
    };

    /**
     * Exact match, not `includes`. The "Add asset by URL" trigger uses the `media_link` icon,
     * which contains `link` as a substring — a loose match would pass on the wrong button and
     * report a gate as removed while it is still in place.
     */
    const buttonWithIcon = (icon: string) =>
        spectator
            .queryAll('button')
            .find(
                (button) =>
                    button.querySelector('.material-symbols-outlined')?.textContent?.trim() === icon
            );

    afterEach(() => {
        spectator?.component?.editor()?.destroy();
    });

    return { buildWith, buttonWithIcon };
}
