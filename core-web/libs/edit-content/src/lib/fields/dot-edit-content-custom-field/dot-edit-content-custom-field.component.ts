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

import { DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { SafeUrlPipe } from '@dotcms/ui';

import { DotEditContentService } from '../../services/dot-edit-content.service';

@Component({
    selector: 'dot-edit-content-custom-field',
    standalone: true,
    imports: [SafeUrlPipe],
    templateUrl: './dot-edit-content-custom-field.component.html',
    styleUrls: ['./dot-edit-content-custom-field.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotEditContentCustomFieldComponent implements OnInit {
    @Input() field!: DotCMSContentTypeField;

    @ViewChild('iframe') iframe!: ElementRef<HTMLIFrameElement>;

    private controlContainer = inject(ControlContainer);
    private editContentService = inject(DotEditContentService);

    contentType = this.editContentService.currentContentType;

    src!: string;

    ngOnInit() {
        this.src = `/html/legacy_custom_field/legacy-custom-field.jsp?variable=${this.contentType}&field=${this.field.variable}`;
    }

    /**
     * Event handler for when the iframe has finished loading.
     * Sets the form property of the iframe's content window.
     */
    onIframeLoad() {
        const iframeWindow = this.iframe.nativeElement.contentWindow as Window;
        iframeWindow['form'] = this.form;
    }

    /**
     * Get the form control associated with the custom field component.
     * @returns The form group.
     */
    get form() {
        return (this.controlContainer as FormGroupDirective).form;
    }
}
