import { createServiceFactory, SpectatorService } from '@openng/spectator/vitest';

import { DotCMSContentlet } from '@dotcms/dotcms-models';

import {
    DotContentChange,
    DotContentChangeKind,
    DotContentChangeService
} from './dot-content-change.service';

describe('DotContentChangeService', () => {
    let spectator: SpectatorService<DotContentChangeService>;
    const contentlet = { inode: 'inode-1' } as DotCMSContentlet;

    const createService = createServiceFactory(DotContentChangeService);

    beforeEach(() => (spectator = createService()));

    it('carries every change on `changes$`, whatever its kind', () => {
        const seen: DotContentChange[] = [];
        spectator.service.changes$.subscribe((change) => seen.push(change));

        spectator.service.notify({ kind: DotContentChangeKind.SAVED, contentlet, depth: 0 });
        spectator.service.notify({ kind: DotContentChangeKind.LOCKED, contentlet, depth: 0 });

        expect(seen.map(({ kind }) => kind)).toEqual([
            DotContentChangeKind.SAVED,
            DotContentChangeKind.LOCKED
        ]);
    });

    // What a listing refreshes on: a lock is not an edit (#38379).
    it('carries only saves and deletes on `edits$`', () => {
        const seen: DotContentChange[] = [];
        spectator.service.edits$.subscribe((change) => seen.push(change));

        spectator.service.notify({ kind: DotContentChangeKind.SAVED, contentlet, depth: 0 });
        spectator.service.notify({ kind: DotContentChangeKind.LOCKED, contentlet, depth: 0 });
        spectator.service.notify({ kind: DotContentChangeKind.DELETED, contentlet, depth: 1 });

        expect(seen.map(({ kind }) => kind)).toEqual([
            DotContentChangeKind.SAVED,
            DotContentChangeKind.DELETED
        ]);
    });
});
