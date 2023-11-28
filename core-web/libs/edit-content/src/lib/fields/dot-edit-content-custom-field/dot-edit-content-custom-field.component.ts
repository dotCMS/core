import { NgClass, NgIf, NgStyle } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    ElementRef,
    HostListener,
    Input,
    NgZone,
    OnInit,
    ViewChild,
    inject
} from '@angular/core';
import { ControlContainer, FormGroupDirective } from '@angular/forms';

import { DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { DotIconModule, SafeUrlPipe } from '@dotcms/ui';

import { DotEditContentService } from '../../services/dot-edit-content.service';

@Component({
    selector: 'dot-edit-content-custom-field',
    standalone: true,
    imports: [SafeUrlPipe, NgStyle, NgClass, DotIconModule, NgIf],
    templateUrl: './dot-edit-content-custom-field.component.html',
    styleUrls: ['./dot-edit-content-custom-field.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotEditContentCustomFieldComponent implements OnInit {
    @Input() field!: DotCMSContentTypeField;

    @ViewChild('iframe') iframe!: ElementRef<HTMLIFrameElement>;

    private controlContainer = inject(ControlContainer);
    private editContentService = inject(DotEditContentService);
    private zone = inject(NgZone);

    private contentType = this.editContentService.currentContentType;
    variables!: { [key: string]: string };
    isFullscreen = false;
    src!: string;

    ngOnInit() {
        this.src = `/html/legacy_custom_field/legacy-custom-field.jsp?variable=${this.contentType}&field=${this.field.variable}`;
        this.variables = this.field.fieldVariables.reduce((result, item) => {
            result[item.key] = item.value;

            return result;
        }, {});
    }

    /**
     * Handles the message received from the custom field.
     * @param event The message event containing the data.
     */
    @HostListener('window:message', ['$event'])
    onMessageFromCustomField(event: MessageEvent) {
        switch (event.data.type) {
            case 'turnOnFullScreen':
                this.isFullscreen = true;
                break;

            case 'turnOffFullScreen':
                this.isFullscreen = false;
                break;

            default:
                break;
        }
    }

    /**
     * Event handler for when the iframe has finished loading.
     * Sets the form property of the iframe's content window.
     */
    onIframeLoad() {
        const iframeWindow = this.iframe.nativeElement.contentWindow as Window;
        iframeWindow['form'] = this.zone.run(() => {
            return this.form;
        });
    }

    /**
     * Get the form control associated with the custom field component.
     * @returns The form group.
     */
    get form() {
        return (this.controlContainer as FormGroupDirective).form;
    }
}
