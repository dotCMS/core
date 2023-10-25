import { BehaviorSubject } from 'rxjs';

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

    editedImage(): BehaviorSubject<DotCMSTempFile> {
        return this.subject;
    }

    openImageEditor({ inode, tempId, variable }: ImageEditorProps): void {
        const customEvent = new CustomEvent(`binaryField-open-image-editor-${variable}`, {
            detail: {
                inode,
                tempId,
                variable
            }
        });
        this.variable = variable;
        document.dispatchEvent(customEvent);
        this.listenToEditedImage();
    }

    listenToEditedImage(): void {
        document.addEventListener(
            `binaryField-tempfile-${this.variable}`,
            this.handleNewImage.bind(this)
        );
    }

    removeListener(): void {
        document.removeEventListener(
            `binaryField-tempfile-${this.variable}`,
            this.handleNewImage.bind(this)
        );
    }

    private handleNewImage({ detail }): void {
        this.subject.next(detail.tempFile);
        this.removeListener();
    }
}
