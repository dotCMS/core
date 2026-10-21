import { Observable, throwError } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { inject, Service } from '@angular/core';

import { map, switchMap } from 'rxjs/operators';

import { DotContentletService, DotWorkflowActionsFireService } from '@dotcms/data-access';
import { DotCMSContentlet, DotCMSTempFile } from '@dotcms/dotcms-models';
import { getFileVersion } from '@dotcms/utils';

import { fileNameOf, SOURCE_EDITOR_BINARY_FIELD } from '../../utils/source-editor.util';

/** A file version's source, and the name the save has to keep. */
export interface DotSourceEditorFile {
    fileName: string;
    source: string;
}

/** What a save writes: the edited source, on top of the version it was opened from. */
export interface DotSourceEditorSave {
    /** The version that was opened; it fixes the file and the language the save goes to. */
    inode: string;
    fileName: string;
    source: string;
}

/** One file in `POST /api/v1/temp`'s answer: the staged file, or the reason it was refused. */
type DotTempFileUpload = Partial<DotCMSTempFile> & { message?: string };

/** The server refused to stage the source, with its reason when it gave one. */
export class DotSourceEditorUploadError extends Error {
    constructor(readonly reason?: string) {
        super(reason ?? 'The server refused the file');
        this.name = 'DotSourceEditorUploadError';
    }
}

/**
 * Reads and writes a File Asset's source for the Edit Code panel.
 *
 * Not auto-provided: the panel lists it in its own providers, since nothing else edits source this
 * way yet.
 */
@Service({ autoProvided: false })
export class DotSourceEditorService {
    readonly #http = inject(HttpClient);
    readonly #contentletService = inject(DotContentletService);
    readonly #workflowActionsFire = inject(DotWorkflowActionsFireService);

    /**
     * Loads one file version and its source.
     *
     * The source is downloaded from the version's own file URL rather than through
     * `DotContentletService.getContentletByInodeWithContent`, which only downloads files the server
     * flags as editable text. That flag follows the MIME type the server detected, and a `.vtl` is
     * not guaranteed to be detected as `text/*`.
     *
     * @param inode the version to open
     */
    load(inode: string): Observable<DotSourceEditorFile> {
        return this.#contentletService.getContentletByInode(inode).pipe(
            switchMap((contentlet) => {
                const url = getFileVersion(contentlet) as string | null;
                const fileName = fileNameOf(contentlet);

                if (!url || !fileName) {
                    return throwError(
                        () => new Error(`File ${inode} has no file to edit the source of`)
                    );
                }

                return this.#http
                    .get(url, { responseType: 'text' })
                    .pipe(map((source) => ({ fileName, source })));
            })
        );
    }

    /**
     * Saves the source as the file's working version, without publishing it.
     *
     * The source is staged as a temp file under the file's own name, so the asset keeps its name,
     * and then set as the file's binary through the default Save action, fired on the version that
     * was opened (`?inode=`), so it lands in that file and language.
     *
     * Whether that is a new version depends on the action. The System Workflow's Save runs "Save
     * Draft content" (`ContentletAPI.saveDraft`): it updates the working version in place when it is
     * an unpublished draft the same user last saved, and creates a new one otherwise.
     *
     * @param save the source and where it goes
     */
    save({ inode, fileName, source }: DotSourceEditorSave): Observable<DotCMSContentlet> {
        const formData = new FormData();
        formData.append('file', new File([source], fileName));

        // Posted directly rather than through `DotTempFileUploadService`: that service reports a
        // failed upload as a status string in `next`, which would read here as a successful one.
        return this.#http.post<{ tempFiles?: DotTempFileUpload[] }>('/api/v1/temp', formData).pipe(
            switchMap(({ tempFiles }) => {
                const [upload] = tempFiles ?? [];

                // A refused file (too large, say) still answers 200, with the reason where the
                // staged file would be. Saving anyway would check in the unchanged version and
                // report the edits as saved.
                if (!upload?.id) {
                    return throwError(() => new DotSourceEditorUploadError(upload?.message));
                }

                return this.#workflowActionsFire.saveContentlet<DotCMSContentlet>({
                    inode,
                    [SOURCE_EDITOR_BINARY_FIELD]: upload.id
                });
            })
        );
    }
}
