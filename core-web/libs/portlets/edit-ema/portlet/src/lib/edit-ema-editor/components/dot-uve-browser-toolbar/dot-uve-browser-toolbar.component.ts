import { ClipboardModule } from '@angular/cdk/clipboard';
import { Component, computed, inject } from '@angular/core';

import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { PopoverModule } from 'primeng/popover';
import { ToolbarModule } from 'primeng/toolbar';
import { TooltipModule } from 'primeng/tooltip';

import { DotMessageService } from '@dotcms/data-access';
import { GlobalStore } from '@dotcms/store';
import { UVE_MODE } from '@dotcms/types';
import { DotMessagePipe } from '@dotcms/ui';
import { WINDOW } from '@dotcms/utils';

import { PageURL } from './types';

import { UVEStore } from '../../../store/dot-uve.store';
import { createFullURL } from '../../../utils';
import { DotUveDeviceControlsComponent } from '../dot-uve-device-controls/dot-uve-device-controls.component';
import { DotUveIframeSizeInputComponent } from '../dot-uve-iframe-size-input/dot-uve-iframe-size-input.component';
import { DotUveZoomControlsComponent } from '../dot-uve-zoom-controls/dot-uve-zoom-controls.component';

/**
 * The browser-style toolbar above the canvas: the palette and edit-panel toggles, the page URL
 * with its copy-URL popover, the device controls, zoom reset, the iframe size input and the zoom
 * controls. Reads and writes the UVE store directly.
 */
@Component({
    selector: 'dot-uve-browser-toolbar',
    imports: [
        ClipboardModule,
        ButtonModule,
        PopoverModule,
        ToolbarModule,
        TooltipModule,
        DotMessagePipe,
        DotUveDeviceControlsComponent,
        DotUveIframeSizeInputComponent,
        DotUveZoomControlsComponent
    ],
    templateUrl: './dot-uve-browser-toolbar.component.html',
    // The toolbar stays a row of the editor content grid, so its sticky top keeps working.
    host: { class: 'contents' }
})
export class DotUveBrowserToolbarComponent {
    protected readonly store = inject(UVEStore);
    readonly #globalStore = inject(GlobalStore);
    readonly #window = inject(WINDOW);
    readonly #messageService = inject(MessageService);
    readonly #dotMessageService = inject(DotMessageService);

    protected readonly UVE_MODE = UVE_MODE;

    protected readonly $pageURLS = computed<PageURL[]>(() => {
        const params = this.store.pageParams();

        // With no page loaded there is no page URL to list.
        if (!params) {
            return [];
        }

        const siteId = this.store.pageAsset()?.site?.identifier;
        const host = params.clientHost || this.#window.location.origin;
        const path = params.url?.replace(/\/index(\.html)?$/, '') || '/';

        const currentSiteHostname = this.#globalStore.siteDetails()?.hostname;

        const urls: PageURL[] = [
            {
                label: 'uve.toolbar.page.live.url',
                value: new URL(path, host).toString()
            },
            {
                label: 'uve.toolbar.page.current.view.url',
                value: createFullURL(params, siteId)
            }
        ];

        if (currentSiteHostname) {
            urls.push({
                label: 'uve.toolbar.page.current.site.url',
                value: new URL(path, `https://${currentSiteHostname}`).toString()
            });
        }

        return urls;
    });

    protected readonly $pageURL = computed((): string => {
        return this.$pageURLS()[0]?.value ?? '/';
    });

    protected togglePalette(): void {
        this.store.setPaletteOpen(!this.store.editorPaletteOpen());
    }

    protected toggleEditPanel(): void {
        this.store.setEditPanelOpen(!this.store.editorEditPanelOpen());
    }

    protected triggerCopyToast(): void {
        this.#messageService.add({
            severity: 'success',
            summary: this.#dotMessageService.get('Copied'),
            life: 3000
        });
    }
}
