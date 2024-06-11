import {
    ChangeDetectionStrategy,
    Component,
    EventEmitter,
    Output,
    ViewChild,
    ElementRef,
    Input,
    ChangeDetectorRef
} from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';

import { EditorAssetTypes } from '@dotcms/dotcms-models';

import { handleLoadVideoError } from './utils';

const regexURL =
    '^((http|https)://)[-a-zA-Z0-9@:%._\\+~#?&//=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%._\\+~#?&//=]*)$';

const regexYoutube = /(youtube\.com\/watch\?v=.*)|(youtu\.be\/.*)/;

@Component({
    selector: 'dot-external-asset',
    templateUrl: './dot-external-asset.component.html',
    styleUrls: ['./dot-external-asset.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExternalAssetComponent {
    @ViewChild('input') input!: ElementRef;
    @Output() addAsset = new EventEmitter();

    @Input()
    type: EditorAssetTypes;

    form: FormGroup;
    disableAction = false;

    get placerHolder(): string {
        return `https://example.com/${this.type === 'video' ? 'video.mp4' : 'image.jpg'}`;
    }

    get error(): string {
        return this.form.controls.url?.errors?.message || '';
    }

    get isInvalid(): boolean {
        return this.form.controls.url?.invalid;
    }

    constructor(
        private fb: FormBuilder,
        private cd: ChangeDetectorRef
    ) {
        this.form = this.fb.group({
            url: ['', [Validators.required, Validators.pattern(regexURL)]]
        });

        this.form.valueChanges.subscribe(({ url }) => {
            this.disableAction = false;

            if (this.type === 'video' && !this.isInvalid && !url.match(regexYoutube)) {
                this.tryToPlayVideo(url);
            }
        });

        requestAnimationFrame(() => this.input.nativeElement.focus());
    }

    /**
     * Emit the url of the externa asset
     *
     * @param {{ url: string }} { url }
     * @memberof DotExternalAssetComponent
     */
    onSubmit({ url }: { url: string }) {
        this.addAsset.emit(url);
    }

    /**
     *
     * Try to play a video by url but if it fails return the error
     * @private
     * @param {string} url
     * @memberof DotExternalAssetComponent
     */
    private tryToPlayVideo(url: string): void {
        const video = document.createElement('video');

        this.disableAction = true;

        video.addEventListener('error', (e) => {
            this.form.controls.url.setErrors({ message: handleLoadVideoError(e) });
            this.cd.detectChanges();
        });

        video.addEventListener('canplay', () => {
            this.form.controls.url.setErrors(null);
            this.disableAction = false;
            this.cd.detectChanges();
        });
        video.src = `${url}#t=0.1`;
    }
}
