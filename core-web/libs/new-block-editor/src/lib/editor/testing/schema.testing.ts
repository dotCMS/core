import { type Injector, runInInjectionContext } from '@angular/core';

import { getSchema } from '@tiptap/core';
import { type Schema } from '@tiptap/pm/model';

import { type DotMessageService } from '@dotcms/data-access';

import { type SlashMenuService } from '../components/slash-menu/slash-menu.service';
import { createEditorExtensions } from '../extensions/editor-extensions';

/**
 * Builds the editor's REAL ProseMirror schema for tests.
 *
 * Never hand-write a schema here. The whole #36985 defect class is about the difference between
 * what the schema declares and what is stored, so a stub schema would test nothing: it would
 * agree with whatever fixture it was written against.
 *
 * `createEditorExtensions` resolves `EditorPopoverService` from the injector when tables are
 * enabled; a bare `{}` stub is enough when only the schema is wanted.
 */
export function buildEditorSchema(
    injector: Injector,
    options: { allowedBlocks?: string[] } = {}
): Schema {
    const dotMessageService = { get: (key: string) => key } as unknown as DotMessageService;
    const menuService = {} as unknown as SlashMenuService;

    return runInInjectionContext(injector, () =>
        getSchema(
            createEditorExtensions(menuService, options.allowedBlocks, injector, dotMessageService)
        )
    );
}
