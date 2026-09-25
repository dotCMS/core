import { vi } from 'vitest';

import { MockDotMessageService } from '@dotcms/utils-testing';

import { roleDeleteConfirmation } from './dot-role-delete.utils';

const messages = new MockDotMessageService({
    'roles.confirm.delete.header': 'Delete Role',
    'roles.confirm.delete.message': 'Deleting {0} cannot be undone.',
    'roles.action.delete': 'Delete',
    'roles.action.cancel': 'Cancel'
});

describe('roleDeleteConfirmation', () => {
    it('should name the role and offer a plain Delete, focusing Cancel', () => {
        const config = roleDeleteConfirmation({ name: 'Eco Role' }, messages, vi.fn());

        expect(config).toEqual(
            expect.objectContaining({
                header: 'Delete Role',
                message: 'Deleting Eco Role cannot be undone.',
                acceptLabel: 'Delete',
                rejectLabel: 'Cancel',
                defaultFocus: 'reject',
                closable: true,
                closeOnEscape: true
            })
        );
    });

    it('should not style the accept button as danger', () => {
        const config = roleDeleteConfirmation({ name: 'Eco Role' }, messages, vi.fn());

        expect(config.acceptButtonStyleClass).toBeUndefined();
    });

    it('should run the given callback on accept', () => {
        const onAccept = vi.fn();

        roleDeleteConfirmation({ name: 'Eco Role' }, messages, onAccept).accept?.();

        expect(onAccept).toHaveBeenCalled();
    });
});
