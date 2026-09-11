import { createComponentFactory, Spectator } from '@openng/spectator/vitest';
import { vi } from 'vitest';

import { signal } from '@angular/core';

import type { Editor } from '@tiptap/core';

import { DotMessageService } from '@dotcms/data-access';

import { ImagePropertiesPopoverComponent } from './image-popover.component';

import { EditorPopoverService } from '../../services/editor-popover.service';

/**
 * #37497 — the dialog's second field writes the `dotImage` node's `title` attribute, which the
 * browser renders as the hover tooltip. It was labelled "Tooltip" and its placeholder read
 * "Add a caption…", so authors typed caption text into a field that renders no caption.
 *
 * There is no `tooltip` property anywhere in the schema — `image.extension.ts` declares `title` —
 * so the keys were renamed `field.tooltip.*` → `field.title.*` to match what is actually stored.
 *
 * `DotMessageService` is stubbed to echo the key, so the rendered text IS the key: these tests
 * assert which key each control is bound to, not what the copy happens to say today.
 */
describe('ImagePropertiesPopoverComponent — message keys match the bound attribute (#37497)', () => {
    let spectator: Spectator<ImagePropertiesPopoverComponent>;

    const KEY = 'dot.block.editor.dialog.image-properties.field.title';

    /**
     * The `<dot-editor-popover>` shell positions itself off `clientRectFn()` whenever the id
     * matches, so the stub must supply one — a bare `{ id }` makes the shell's effect throw
     * `active.clientRectFn is not a function`, which Vitest reports as an unhandled error and
     * fails the run even while every assertion passes.
     */
    const activePopover = signal<{ id: string; clientRectFn: () => DOMRect } | null>(null);

    const createComponent = createComponentFactory({
        component: ImagePropertiesPopoverComponent,
        providers: [
            { provide: DotMessageService, useValue: { get: (key: string) => key } },
            {
                provide: EditorPopoverService,
                useValue: {
                    activePopover,
                    imagePropertiesPayload: signal(null),
                    isOpen: (id: string) => activePopover()?.id === id,
                    close: vi.fn()
                }
            }
        ]
    });

    beforeEach(() => {
        activePopover.set({ id: 'image-properties', clientRectFn: () => new DOMRect() });
        spectator = createComponent({ props: { editor: {} as Editor } });
    });

    it('binds the title input to the form control that writes the `title` attribute', () => {
        spectator.component.form.controls.title.setValue('A tooltip');
        spectator.detectChanges();

        expect(spectator.query<HTMLInputElement>('#edit-img-title').value).toBe('A tooltip');
    });

    it('labels that input with the `field.title.label` key', () => {
        expect(spectator.query('label[for="edit-img-title"]')).toHaveText(`${KEY}.label`);
    });

    it('describes it with the `field.title.hint` key', () => {
        expect(spectator.query('#edit-img-title-hint')).toHaveText(`${KEY}.hint`);
    });

    it('places the `field.title.placeholder` key on that same input', () => {
        expect(spectator.query<HTMLInputElement>('#edit-img-title').placeholder).toBe(
            `${KEY}.placeholder`
        );
    });

    it('no longer references any `field.tooltip.*` key', () => {
        expect(spectator.element.innerHTML).not.toContain('field.tooltip');
    });

    it('keeps the url and alt fields on their own keys', () => {
        const base = 'dot.block.editor.dialog.image-properties.field';

        expect(spectator.query<HTMLInputElement>('#edit-img-url').placeholder).toBe(
            `${base}.url.placeholder`
        );
        expect(spectator.query<HTMLInputElement>('#edit-img-alt').placeholder).toBe(
            `${base}.alt.placeholder`
        );
    });
});
