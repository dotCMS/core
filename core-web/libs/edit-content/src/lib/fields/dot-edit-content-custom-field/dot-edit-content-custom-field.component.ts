import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    ElementRef,
    Input,
    OnInit,
    ViewChild,
    inject
} from '@angular/core';
import { ControlContainer, FormGroupDirective } from '@angular/forms';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';
import { ActivatedRoute } from '@angular/router';

import { DotCMSContentTypeField } from '@dotcms/dotcms-models';

@Component({
    selector: 'dot-edit-content-custom-field',
    standalone: true,
    templateUrl: './dot-edit-content-custom-field.component.html',
    styleUrls: ['./dot-edit-content-custom-field.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotEditContentCustomFieldComponent implements OnInit {
    @Input() field!: DotCMSContentTypeField;

    @ViewChild('iframe') iframe!: ElementRef;

    private activatedRoute = inject(ActivatedRoute);
    private controlContainer = inject(ControlContainer);
    private sanitizer = inject(DomSanitizer);
    private changeDetectorRef = inject(ChangeDetectorRef);

    public contentType = this.activatedRoute.snapshot.params['contentType'];

    src!: SafeUrl;

    ngOnInit() {
        this.src = this.sanitizer.bypassSecurityTrustResourceUrl(
            `/html/legacy_custom_field/legacy-custom-field.jsp?variable=${this.contentType}&field=${this.field.variable}`
        );
    }

    onIframeLoad() {
        this.iframe.nativeElement.contentWindow.window.form = this.form;
    }

    get form() {
        return (this.controlContainer as FormGroupDirective).form;
    }
}
