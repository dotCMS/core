import { Component, EventEmitter, Input, Output } from '@angular/core';
import { take, tap } from 'rxjs/operators';

import { DotCMSContentlet } from '@dotcms/dotcms-models';
import {
    DotImageService,
    FileStatus
} from '../image-uploader/services/dot-image/dot-image.service';

@Component({
    selector: 'dot-floating-button',
    templateUrl: './floating-button.component.html',
    styleUrls: ['./floating-button.component.scss']
})
export class FloatingButtonComponent {
    @Input() url = '';
    @Input() label = '';
    @Input() isLoading = false;

    @Output() dotAsset: EventEmitter<DotCMSContentlet> = new EventEmitter();

    public status = FileStatus;

    get title() {
        if (!this.label) return '';

        return this.label[0].toUpperCase() + this.label?.substring(1).toLowerCase();
    }

    get isCompleted() {
        return this.label === this.status.COMPLETED;
    }

    constructor(private readonly dotImageService: DotImageService) {}

    importImage() {
        this.isLoading = true;
        this.dotImageService
            .publishContent({
                data: this.url,
                statusCallback: this.updateButtonLabel.bind(this)
            })
            .pipe(
                take(1),
                tap(() => (this.isLoading = false))
            )
            .subscribe(
                (data) => {
                    const contentlet = data[0];
                    this.label = this.status.COMPLETED;
                    this.dotAsset.emit(contentlet[Object.keys(contentlet)[0]]);
                },
                (error) => {
                    this.label = this.status.ERROR;
                    this.dotAsset.error(error);
                }
            );
    }

    private updateButtonLabel(status: string) {
        this.label = status;
    }
}
