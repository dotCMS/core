import { createServiceFactory, mockProvider, SpectatorService } from '@openng/spectator/vitest';
import { Mock, vi } from 'vitest';

import { Confirmation, ConfirmationService } from 'primeng/api';

import { DotAlertConfirmService, DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotRoleDeleteService } from './dot-role-delete.service';

import { DotRolesStore } from '../dot-roles-page/store/dot-roles.store';

const MESSAGES = {
    'roles.confirm.delete.header': 'Delete Role',
    'roles.confirm.delete.message': 'Deleting {0} cannot be undone.',
    'roles.action.delete': 'Delete',
    'roles.action.cancel': 'Cancel',
    'roles.delete.rejected': 'This role could not be deleted.'
};

const ROLE = { id: 'r-eco', name: 'Eco Role' };

describe('DotRoleDeleteService', () => {
    let spectator: SpectatorService<DotRoleDeleteService>;

    const createService = createServiceFactory({
        service: DotRoleDeleteService,
        providers: [
            mockProvider(DotRolesStore, { deleteRole: vi.fn() }),
            mockProvider(ConfirmationService, { confirm: vi.fn() }),
            mockProvider(DotAlertConfirmService, { alert: vi.fn() }),
            { provide: DotMessageService, useValue: new MockDotMessageService(MESSAGES) }
        ]
    });

    /** Opens the confirmation and returns what was passed to it. */
    const openConfirm = (): Confirmation => {
        spectator.service.confirmDelete(ROLE);
        const confirmation = spectator.inject(ConfirmationService);

        return (confirmation.confirm as Mock).mock.calls[0][0];
    };

    beforeEach(() => {
        spectator = createService();
        vi.clearAllMocks();
    });

    it('should ask for confirmation naming the role, with a plain Delete accept', () => {
        const store = spectator.inject(DotRolesStore);
        const config = openConfirm();

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
        // Not red: the accept button keeps the default severity.
        expect(config.acceptButtonStyleClass).toBeUndefined();
        expect(store.deleteRole).not.toHaveBeenCalled();
    });

    it('should delete the role when the admin accepts', async () => {
        const store = spectator.inject(DotRolesStore);
        const alertService = spectator.inject(DotAlertConfirmService);
        (store.deleteRole as Mock).mockResolvedValue({ deleted: true });

        await openConfirm().accept?.();

        expect(store.deleteRole).toHaveBeenCalledWith('r-eco');
        expect(alertService.alert).not.toHaveBeenCalled();
    });

    it('should alert when the backend refuses the delete', async () => {
        const store = spectator.inject(DotRolesStore);
        const alertService = spectator.inject(DotAlertConfirmService);
        (store.deleteRole as Mock).mockResolvedValue({ deleted: false });

        await openConfirm().accept?.();

        expect(alertService.alert).toHaveBeenCalledWith({
            header: 'Delete Role',
            message: 'This role could not be deleted.'
        });
    });

    it('should not alert on an HTTP failure — the store already reported it', async () => {
        const store = spectator.inject(DotRolesStore);
        const alertService = spectator.inject(DotAlertConfirmService);
        (store.deleteRole as Mock).mockResolvedValue(null);

        await openConfirm().accept?.();

        expect(alertService.alert).not.toHaveBeenCalled();
    });
});
