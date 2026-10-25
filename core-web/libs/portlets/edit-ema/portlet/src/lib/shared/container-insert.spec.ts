import { describe, expect, it, vi } from 'vitest';

import { MessageService } from 'primeng/api';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { notifyContainerInsertRefused } from './container-insert';
import { CONTAINER_INSERT_ERROR } from './enums';

const dotMessageService = new MockDotMessageService({
    'editpage.content.add.already.title': 'Content already added',
    'editpage.content.add.already.message': 'This content is already added to this container',
    'editpage.content.container.limit.title': 'Container full',
    'editpage.content.container.limit.message': 'This container holds up to {0} contentlets'
}) as unknown as DotMessageService;

describe('notifyContainerInsertRefused', () => {
    const createMessageService = () => ({ add: vi.fn() }) as unknown as MessageService;

    it('should tell the user how many contentlets a full container holds', () => {
        const messageService = createMessageService();

        notifyContainerInsertRefused(
            {
                didInsert: false,
                errorCode: CONTAINER_INSERT_ERROR.CONTAINER_LIMIT_REACHED,
                maxContentlets: 3
            },
            messageService,
            dotMessageService
        );

        expect(messageService.add).toHaveBeenCalledWith({
            severity: 'warn',
            summary: 'Container full',
            detail: 'This container holds up to 3 contentlets',
            life: 3000
        });
    });

    it('should tell the user the container already holds the contentlet', () => {
        const messageService = createMessageService();

        notifyContainerInsertRefused(
            { didInsert: false, errorCode: CONTAINER_INSERT_ERROR.DUPLICATE_CONTENT },
            messageService,
            dotMessageService
        );

        expect(messageService.add).toHaveBeenCalledWith({
            severity: 'info',
            summary: 'Content already added',
            detail: 'This content is already added to this container',
            life: 2000
        });
    });
});
