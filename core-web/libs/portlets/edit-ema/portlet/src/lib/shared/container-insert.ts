import { inject } from '@angular/core';

import { MessageService } from 'primeng/api';

import { DotMessageService } from '@dotcms/data-access';

import { CONTAINER_INSERT_ERROR } from './enums';
import { ActionPayload } from './models';

import { UVEStore } from '../store/dot-uve.store';
import { EditorInsertRefusal } from '../store/features/editor/models';

/** Message keys for the two reasons a container refuses a contentlet. */
const MESSAGE_KEY = {
    DUPLICATE_CONTENT: {
        TITLE: 'editpage.content.add.already.title',
        MESSAGE: 'editpage.content.add.already.message'
    },
    CONTAINER_LIMIT: {
        TITLE: 'editpage.content.container.limit.title',
        MESSAGE: 'editpage.content.container.limit.message'
    }
} as const;

/**
 * Tell the user why a container refused a contentlet: it is full, or it already holds that
 * contentlet. Every flow that inserts into a container shows the same two toasts.
 *
 * @param refusal what the store's `editorInsertContentlet` reported
 * @param messageService where the toast goes
 * @param dotMessageService the translations
 */
export function notifyContainerInsertRefused(
    refusal: EditorInsertRefusal,
    messageService: MessageService,
    dotMessageService: DotMessageService
): void {
    if (refusal.errorCode === CONTAINER_INSERT_ERROR.CONTAINER_LIMIT_REACHED) {
        messageService.add({
            severity: 'warn',
            summary: dotMessageService.get(MESSAGE_KEY.CONTAINER_LIMIT.TITLE),
            detail: dotMessageService.get(
                MESSAGE_KEY.CONTAINER_LIMIT.MESSAGE,
                refusal.maxContentlets.toString()
            ),
            life: 3000
        });

        return;
    }

    messageService.add({
        severity: 'info',
        summary: dotMessageService.get(MESSAGE_KEY.DUPLICATE_CONTENT.TITLE),
        detail: dotMessageService.get(MESSAGE_KEY.DUPLICATE_CONTENT.MESSAGE),
        life: 2000
    });
}

/**
 * Inserts a contentlet into a container position through the store, and tells the user why when
 * the container refuses it. Call it where `inject` works, such as a field initializer.
 *
 * @returns the insert: it takes the container position and the contentlet, and returns whether the
 * contentlet went in
 */
export function injectContainerInsert(): (into: ActionPayload, contentletId: string) => boolean {
    const uveStore = inject(UVEStore);
    const messageService = inject(MessageService);
    const dotMessageService = inject(DotMessageService);

    return (into, contentletId) => {
        const result = uveStore.editorInsertContentlet(into, contentletId);

        // `=== false`: without strictNullChecks, as this library compiles, `!` does not narrow the
        // result to a refusal.
        if (result.didInsert === false) {
            notifyContainerInsertRefused(result, messageService, dotMessageService);
        }

        return result.didInsert;
    };
}
