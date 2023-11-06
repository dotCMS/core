import { BehaviorSubject, Observable } from 'rxjs';

import { Injectable } from '@angular/core';

import { DotCMSTempFile } from '@dotcms/dotcms-models';

interface ImageEditorProps {
    inode: string;
    tempId: string;
    variable: string;
}

@Injectable()
export class DotBinaryFieldEditImageService {
    private subject: BehaviorSubject<DotCMSTempFile> = new BehaviorSubject(null);
    private variable: string;

    editedImage(): Observable<DotCMSTempFile> {
        return this.subject.asObservable();
    }

    /**
     * Open the dojo image editor modal and listen to the edited image
     *
     * @param {ImageEditorProps} { inode, tempId, variable }
     * @memberof DotBinaryFieldEditImageService
     */
    openImageEditor({ inode, tempId, variable }: ImageEditorProps): void {
        this.variable = variable;
        const customEvent = new CustomEvent(`binaryField-open-image-editor-${variable}`, {
            detail: {
                inode,
                tempId,
                variable
            }
        });
        document.dispatchEvent(customEvent);
        this.listenToEditedImage();
        this.listenToCloseImageEditor();
    }

    /**
     * Listen to the edited image
     *
     * @memberof DotBinaryFieldEditImageService
     */
    listenToEditedImage(): void {
        document.addEventListener(
            `binaryField-tempfile-${this.variable}`,
            this.handleNewImage.bind(this)
        );
    }

    /**
     * Listen to the close image editor event
     *
     * @memberof DotBinaryFieldEditImageService
     */
    listenToCloseImageEditor(): void {
        document.addEventListener(
            `binaryField-close-image-editor-${this.variable}`,
            this.removeListener.bind(this)
        );
    }

    /**
     * Remove the listener to the edited image
     *
     * @memberof DotBinaryFieldEditImageService
     */
    removeListener(): void {
        document.removeEventListener(
            `binaryField-tempfile-${this.variable}`,
            this.handleNewImage.bind(this)
        );

        document.removeEventListener(
            `binaryField-close-image-editor-${this.variable}`,
            this.removeListener.bind(this)
        );
    }

    /**
     * Handle the edited image
     *
     * @private
     * @param {*} { detail }
     * @memberof DotBinaryFieldEditImageService
     */
    private handleNewImage({ detail }): void {
        this.subject.next(detail.tempFile);
        this.removeListener();
    }
}
