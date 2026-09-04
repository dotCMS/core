import { DotCMSContentlet } from '@dotcms/dotcms-models';

export const getRelationshipFromContentlet = (params: {
    // Nullable: resolution functions run for new content too, and the `!contentlet` guard below
    // was already written for it.
    contentlet: DotCMSContentlet | null;
    variable: string;
}): DotCMSContentlet[] => {
    const { contentlet, variable } = params;

    if (!contentlet || !variable || !contentlet[variable]) {
        return [];
    }

    const relationship = contentlet[variable];
    const isArray = Array.isArray(relationship);

    if (!isArray && typeof relationship !== 'object') {
        return [];
    }

    return isArray ? relationship : [relationship];
};
