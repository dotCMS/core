import {
    DotCMSBaseTypesContentTypes,
    DotCMSContentlet,
    DotContentletCanLock
} from '@dotcms/dotcms-models';
import { DOT_VELOCITY_LANGUAGE_ID } from '@dotcms/ui';

import { DotSourceEditorRequest } from '../models/dot-source-editor.model';

/**
 * File extensions Edit Source opens, each with the Monaco language that highlights it.
 *
 * Velocity only for now. Every other code file dotCMS stores (css, js, html, json, scss…) is one
 * more entry here: Monaco ships those languages, so nothing else has to change. A `Map` rather than
 * an object literal, so an extension such as `constructor` can never resolve to a prototype member.
 */
const SOURCE_EDITOR_LANGUAGES: ReadonlyMap<string, string> = new Map([
    ['vtl', DOT_VELOCITY_LANGUAGE_ID]
]);

/**
 * The binary field the save writes the edited source to. File Assets keep their file in
 * `fileAsset`, which is why only they qualify; a dotAsset (`asset`) would need its own entry.
 */
export const SOURCE_EDITOR_BINARY_FIELD = 'fileAsset';

/**
 * What to open a file in Edit Source with, or `null` when Edit Source does not apply to it.
 *
 * The one rule every opener follows (Content Drive's row menu, the page editor's VTL menu), so they
 * cannot disagree about who may edit what:
 *
 * - the file is a File Asset of a type the editor handles, and
 * - the user can lock it. The server answers that from the two things a save needs: edit permission
 *   on the file, and no lock held by someone else (`ContentletAPI.canLock`; admins always can).
 *
 * @param contentlet the file
 * @param lock the server's answer for that file, from `DotContentletService.canLock`
 */
export function toSourceEditorRequest(
    contentlet: DotCMSContentlet,
    lock: DotContentletCanLock
): DotSourceEditorRequest | null {
    const language = sourceEditorLanguageOf(contentlet);

    if (!language || !lock.canLock) {
        return null;
    }

    return {
        inode: contentlet.inode,
        identifier: contentlet.identifier,
        languageId: contentlet.languageId,
        title: fileNameOf(contentlet) ?? contentlet.title,
        language
    };
}

/**
 * The file's name: the `fileName` field on a File Asset, else the `name` the server adds to file
 * rows. `undefined` when the contentlet carries neither.
 *
 * @param contentlet the file
 */
export function fileNameOf(contentlet: DotCMSContentlet): string | undefined {
    return (
        (contentlet['fileName'] as string | undefined) || (contentlet['name'] as string | undefined)
    );
}

/**
 * The Monaco language to edit a file's source in, or `null` when it is not a File Asset of a type
 * the editor handles.
 *
 * @param contentlet the file
 */
function sourceEditorLanguageOf(contentlet: DotCMSContentlet): string | null {
    if (contentlet.baseType !== DotCMSBaseTypesContentTypes.FILEASSET) {
        return null;
    }

    return SOURCE_EDITOR_LANGUAGES.get(fileExtensionOf(contentlet)) ?? null;
}

/**
 * The file's extension, lower-cased and without the dot. File rows carry `extension` from the
 * server; the file name is the fallback for a row that does not.
 *
 * @param contentlet the file
 */
function fileExtensionOf(contentlet: DotCMSContentlet): string {
    const extension = contentlet['extension'] as string | undefined;

    if (extension) {
        return extension.toLowerCase();
    }

    const fileName = fileNameOf(contentlet) ?? '';
    const dot = fileName.lastIndexOf('.');

    return dot === -1 ? '' : fileName.slice(dot + 1).toLowerCase();
}
