import { SpectatorService, createServiceFactory, mockProvider } from '@openng/spectator/vitest';
import { firstValueFrom, of, throwError } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { DotContentSearchService } from '@dotcms/data-access';

import { ConstrainedIdentifiersService } from './constrained-identifiers.service';

/**
 * The lookup behind the cardinality guard: which children are already held by another parent.
 *
 * The store's own spec mocks this service, so nothing there can see how it behaves on failure —
 * which is the whole point of these cases.
 *
 * Awaited through `firstValueFrom` rather than a `done` callback: Vitest does not support Jest's
 * callback style, so a `done`-shaped test returns before its assertions run and passes whatever the
 * code does. The first version of this file did exactly that.
 */
describe('ConstrainedIdentifiersService', () => {
    let spectator: SpectatorService<ConstrainedIdentifiersService>;
    const searchMock = vi.fn();

    const createService = createServiceFactory({
        service: ConstrainedIdentifiersService,
        providers: [mockProvider(DotContentSearchService, { get: searchMock })]
    });

    const params = {
        parentContentTypeId: 'parent-type',
        fieldVariable: 'rel',
        currentContentIdentifier: 'me'
    };

    const response = (contentlets: unknown[]) => of({ jsonObjectView: { contentlets } });

    beforeEach(() => {
        searchMock.mockReset();
        spectator = createService();
    });

    it('collects the children other parents already hold', async () => {
        searchMock.mockReturnValue(
            response([
                { identifier: 'other-parent', rel: ['child-1', 'child-2'] },
                { identifier: 'me', rel: ['child-3'] }
            ])
        );

        const claimed = await firstValueFrom(spectator.service.get(params));

        expect([...claimed].sort()).toEqual(['child-1', 'child-2']);
    });

    it('does not count the contentlet being edited as another parent', async () => {
        searchMock.mockReturnValue(response([{ identifier: 'me', rel: ['child-3'] }]));

        const claimed = await firstValueFrom(spectator.service.get(params));

        expect(claimed.size).toBe(0);
    });

    /**
     * Reported in review, and the reason this spec exists.
     *
     * The service used to end in `catchError(() => of(new Set()))`. Every failure — a 500, a 403, a
     * dropped connection, a timeout — became "nothing is claimed", which is indistinguishable from
     * a successful lookup that found nothing. The guard then stayed off for the life of the dialog
     * and an editor could reparent a child with no warning at any layer.
     *
     * The caller decides what a failure means, and says so on screen. It cannot do either if the
     * failure never reaches it.
     */
    it('lets a failed lookup reach the caller instead of reporting an empty set', async () => {
        const failure = new Error('boom');
        searchMock.mockReturnValue(throwError(() => failure));

        await expect(firstValueFrom(spectator.service.get(params))).rejects.toBe(failure);
    });

    it('asks for nothing when the field carries no parent context', async () => {
        const claimed = await firstValueFrom(
            spectator.service.get({ ...params, parentContentTypeId: '' })
        );

        expect(claimed.size).toBe(0);
        expect(searchMock).not.toHaveBeenCalled();
    });
});
