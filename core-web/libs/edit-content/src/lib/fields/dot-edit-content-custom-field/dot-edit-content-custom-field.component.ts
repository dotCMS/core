import { JsonPipe, NgStyle } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    ElementRef,
    HostListener,
    NgZone,
    OnDestroy,
    OnInit,
    computed,
    inject,
    input,
    signal,
    viewChild
} from '@angular/core';
import { ControlContainer, FormGroupDirective } from '@angular/forms';

import { ButtonModule } from 'primeng/button';

import { DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { DotFormBridge } from '@dotcms/edit-content/bridge';
import { DotIconModule, SafeUrlPipe } from '@dotcms/ui';
import { WINDOW } from '@dotcms/utils';

@Component({
    selector: 'dot-edit-content-custom-field',
    standalone: true,
    imports: [SafeUrlPipe, NgStyle, DotIconModule, ButtonModule, JsonPipe],
    templateUrl: './dot-edit-content-custom-field.component.html',
    styleUrls: ['./dot-edit-content-custom-field.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [
        {
            provide: WINDOW,
            useValue: window
        }
    ],
    viewProviders: [
        {
            provide: ControlContainer,
            useFactory: () => inject(ControlContainer, { skipSelf: true })
        }
    ]
})
export class DotEditContentCustomFieldComponent implements OnInit, OnDestroy {
    $field = input.required<DotCMSContentTypeField>({ alias: 'field' });
    $contentType = input.required<string>({ alias: 'contentType' });
    iframe = viewChild.required<ElementRef<HTMLIFrameElement>>('iframe');

    #window = inject(WINDOW);
    private readonly ALLOWED_ORIGINS = [this.#window.location.origin];

    $isFullscreen = signal(false);
    $variables = signal<Record<string, string>>({});

    $src = computed(() => this.buildIframeSrc());

    #formBridge?: DotFormBridge;
    #controlContainer = inject(ControlContainer);
    #zone = inject(NgZone);

    $form = computed(() => (this.#controlContainer as FormGroupDirective).form);

    ngOnInit() {
        this.$variables.set(this.initializeVariables());
        this.initializeFormBridge();
    }

    @HostListener('window:message', ['$event'])
    onMessageFromCustomField({ data, origin }: MessageEvent) {
        if (!this.ALLOWED_ORIGINS.includes(origin)) {
            console.warn('Message received from unauthorized origin:', origin);

            return;
        }

        switch (data.type) {
            case 'toggleFullscreen':
                this.$isFullscreen.update((value) => !value);
                break;
        }
    }

    onIframeLoad() {
        const iframeWindow = this.getIframeWindow();
        if (!iframeWindow) return;

        this.#zone.run(() => this.initializeCustomFieldApi(iframeWindow));
    }

    private buildIframeSrc(): string {
        const params = new URLSearchParams({
            variable: this.$contentType(),
            field: this.$field().variable
        });

        return `/html/legacy_custom_field/legacy-custom-field.jsp?${params}`;
    }

    private initializeVariables(): Record<string, string> {
        return this.$field().fieldVariables.reduce(
            (acc, { key, value }) => {
                acc[key] = value;

                return acc;
            },
            {} as Record<string, string>
        );
    }

    private initializeFormBridge(): void {
        const form = (this.#controlContainer as FormGroupDirective).form;

        this.#formBridge = new DotFormBridge({
            type: 'angular',
            form,
            iframe: this.iframe().nativeElement,
            zone: this.#zone
        });
    }

    private getIframeWindow(): Window | null {
        if (!this.iframe()) {
            console.warn('Iframe not initialized');

            return null;
        }

        const iframeWindow = this.iframe().nativeElement.contentWindow;
        if (!iframeWindow) {
            console.warn('Iframe window not available');

            return null;
        }

        return iframeWindow;
    }

    private initializeCustomFieldApi(iframeWindow: Window): void {
        try {
            const api = this.#formBridge?.createPublicApi();
            if (!api) throw new Error('Form bridge not initialized');

            // Assign API only to iframe
            iframeWindow['DotCustomFieldApi'] = api;

            // Notify that the API is ready
            iframeWindow.postMessage({ type: 'dotcms:form:loaded' }, this.#window.location.origin);
        } catch (error) {
            console.error('Error initializing DotCustomFieldApi:', error);
        }
    }

    ngOnDestroy(): void {
        this.#formBridge?.destroy();
    }
}
