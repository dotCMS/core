// import { DotSafeUrlPipe } from 'libs/ui/src/lib/pipes/safe-url/safe-url.pipe.ts';

import {
    ChangeDetectionStrategy,
    Component,
    ElementRef,
    Input,
    OnInit,
    ViewChild,
    inject
} from '@angular/core';
import { ControlContainer, FormGroupDirective } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';

import { DotCMSContentTypeField } from '@dotcms/dotcms-models';

@Component({
    selector: 'dot-edit-content-custom-field',
    standalone: true,
    // imports: [DotSafeUrlPipe],
    templateUrl: './dot-edit-content-custom-field.component.html',
    styleUrls: ['./dot-edit-content-custom-field.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotEditContentCustomFieldComponent implements OnInit {
    @Input() field!: DotCMSContentTypeField;

    @ViewChild('iframe') iframe!: ElementRef<HTMLIFrameElement>;

    private activatedRoute = inject(ActivatedRoute);
    private controlContainer = inject(ControlContainer);

    contentType = this.activatedRoute.snapshot.params['contentType'];

    src!: string;

    ngOnInit() {
        this.src = `/html/legacy_custom_field/legacy-custom-field.jsp?variable=${this.contentType}&field=${this.field.variable}`;
    }

    onIframeLoad() {
        const iframeWindow = this.iframe.nativeElement.contentWindow as Window;
        iframeWindow['form'] = this.form;
    }

    get form() {
        return (this.controlContainer as FormGroupDirective).form;
    }
}
