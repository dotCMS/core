import { NgClass, NgStyle } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    ElementRef,
    HostListener,
    Inject,
    Input,
    NgZone,
    OnInit,
    ViewChild,
    inject
} from '@angular/core';
import { ControlContainer, FormGroupDirective } from '@angular/forms';

import { ButtonModule } from 'primeng/button';

import { DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { DotIconModule, SafeUrlPipe } from '@dotcms/ui';
import { WINDOW } from '@dotcms/utils';

@Component({
    selector: 'dot-edit-content-custom-field',
    standalone: true,
    imports: [SafeUrlPipe, NgStyle, NgClass, DotIconModule, ButtonModule],
    templateUrl: './dot-edit-content-custom-field.component.html',
    styleUrls: ['./dot-edit-content-custom-field.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [
        {
            provide: WINDOW,
            useValue: window
        }
    ]
})
export class DotEditContentCustomFieldComponent implements OnInit {
    @Input() field!: DotCMSContentTypeField;
    @Input() contentType!: string;

    @ViewChild('iframe') iframe!: ElementRef<HTMLIFrameElement>;

    private controlContainer = inject(ControlContainer);
    private zone = inject(NgZone);

    variables!: { [key: string]: string };
    isFullscreen = false;
    src!: string;

    constructor(@Inject(WINDOW) private window: Window) {}

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
        if (event.data.type === 'toggleFullscreen') {
            this.isFullscreen = !this.isFullscreen;
        }
    }

    /**
     * Event handler for when the iframe has finished loading.
     * Sets the form property of the iframe's content window.
     */
    onIframeLoad() {
        const iframeWindow = this.iframe.nativeElement.contentWindow as Window;
        iframeWindow['form'] = this.zone.run(() => this.form);
        iframeWindow.postMessage({ type: 'dotcms:form:loaded' }, this.window.parent.origin);
    }

    /**
     * Get the form control associated with the custom field component.
     * @returns The form group.
     */
    get form() {
        return (this.controlContainer as FormGroupDirective).form;
    }
}
