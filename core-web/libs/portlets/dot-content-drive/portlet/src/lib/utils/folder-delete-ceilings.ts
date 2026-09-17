import { DotFolderBulkDeleteCeilings } from '@dotcms/dotcms-models';

/**
 * Refusing a bulk folder delete before it is submitted, in the words of the ceiling it crossed.
 *
 * The server enforces the ceiling and is the authority on it; this is the courtesy check in front.
 * Mirrors `upload-ceilings.ts` on purpose — an author who meets both features should meet one
 * behaviour — including its refusal to assume a default when the instance advertises none.
 *
 * NOT YET IMPLEMENTED — stub written so the test set compiles and fails on behaviour. Implemented
 * once the Red gate is confirmed.
 */

/** Too many folders, naming the selection's count and the ceiling. */
export const TOO_MANY_FOLDERS_NAMED_KEY = 'content-drive.delete.refused.too-many-folders-named';

/** A refusal, as the key that explains it and the numbers that go in it. */
export interface DotFolderDeleteCeilingRefusal {
    key: string;
    args: string[];
}

/**
 * Whether this selection crosses the ceiling the server would refuse it for.
 *
 * A ceiling that is absent, zero or negative is treated as no ceiling: absent means an instance
 * older than the field or a configuration request that never landed, and zero or below is how the
 * platform spells unbounded elsewhere. A guessed default would be worse than no check — the client
 * would refuse selections the instance accepts, and no author could see why.
 *
 * @returns the refusal, or `undefined` when the selection may be submitted
 */
export function refuseOverPathCeiling(
    _paths: string[],
    _ceilings: Partial<DotFolderBulkDeleteCeilings> | null | undefined
): DotFolderDeleteCeilingRefusal | undefined {
    throw new Error('refuseOverPathCeiling is not implemented yet');
}
