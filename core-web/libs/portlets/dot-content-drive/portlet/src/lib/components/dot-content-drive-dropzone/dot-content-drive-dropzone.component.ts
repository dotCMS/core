import { patchState, signalState } from '@ngrx/signals';

import {
    ChangeDetectionStrategy,
    Component,
    effect,
    HostBinding,
    HostListener
} from '@angular/core';

import { DotMessagePipe } from '@dotcms/ui';
import { WINDOW } from '@dotcms/utils';

type DotContentDriveDropzoneState = {
    active: boolean;
    files?: FileList;
    invalidFiles?: FileList;
};

@Component({
    selector: 'dot-content-drive-dropzone',
    imports: [DotMessagePipe],
    providers: [
        {
            provide: WINDOW,
            useValue: window
        }
    ],
    templateUrl: './dot-content-drive-dropzone.component.html',
    styleUrl: './dot-content-drive-dropzone.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotContentDriveDropzoneComponent {
    readonly #state = signalState<DotContentDriveDropzoneState>({
        invalidFiles: undefined,
        active: false,
        files: undefined
    });

    @HostBinding('class.active') get active() {
        return this.#state.active();
    }

    uploadFilesEffect = effect(() => {
        const files = this.#state.files();
        if (!files || !files?.length) {
            return;
        }

        alert('uploadFiles');

        // this.uploadFiles(files);
    });

    @HostListener('dragenter', ['$event'])
    onDragEnter(event: DragEvent & { fromElement: HTMLElement }) {
        event.stopPropagation();
        event.preventDefault();

        if (event.fromElement) {
            return;
        }

        patchState(this.#state, {
            active: true
        });

        // if (
        //     validFilesMasks.some((mask) => files.some((file) => file.name.includes(mask)))
        // ) {
        //     this.#state.invalidFiles.set(invalidFiles);
        //     return;
        // } else {
        //     this.#state.invalidFiles.set([]);
        // }// if (
        //     validFilesMasks.some((mask) => files.some((file) => file.name.includes(mask)))
        // ) {
        //     this.#state.invalidFiles.set(invalidFiles);
        //     return;
        // } else {
        //     this.#state.invalidFiles.set([]);
        // }
    }

    @HostListener('dragover', ['$event'])
    onDragOver(event: DragEvent) {
        event.stopPropagation();
        event.preventDefault();
    }

    @HostListener('dragleave', ['$event'])
    onDragLeave(event: DragEvent) {
        event.preventDefault();

        if (event.relatedTarget) {
            return;
        }

        patchState(this.#state, {
            files: undefined,
            active: false,
            invalidFiles: undefined
        });
    }

    @HostListener('dragend', ['$event'])
    onDragEnd(event: DragEvent) {
        event.preventDefault();
        patchState(this.#state, {
            files: undefined,
            active: false,
            invalidFiles: undefined
        });
    }

    @HostListener('drop', ['$event'])
    onDrop(event: DragEvent) {
        event.stopPropagation();
        event.preventDefault();

        const files = event.dataTransfer?.files;

        patchState(this.#state, {
            active: false,
            files
        });
    }
}
