import {
    ChangeDetectionStrategy,
    Component,
    EventEmitter,
    Output,
    ViewChild,
    ElementRef,
    Input
} from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';

import { EditorAssetTypes } from '@dotcms/dotcms-models';

const regexURL =
    '^((http|https)://)[-a-zA-Z0-9@:%._\\+~#?&//=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%._\\+~#?&//=]*)$';

@Component({
    selector: 'dot-external-asset',
    templateUrl: './dot-external-asset.component.html',
    styleUrls: ['./dot-external-asset.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExternalAssetComponent {
    @ViewChild('input') input!: ElementRef;
    @Output() addImage = new EventEmitter();

    @Input()
    assetType: EditorAssetTypes;

    form: FormGroup;

    constructor(private fb: FormBuilder) {
        this.form = this.fb.group({
            url: ['', [Validators.required, Validators.pattern(regexURL)]]
        });

        requestAnimationFrame(() => this.input.nativeElement.focus());
    }

    onSubmit({ url }) {
        this.addImage.emit(url);
    }
}
