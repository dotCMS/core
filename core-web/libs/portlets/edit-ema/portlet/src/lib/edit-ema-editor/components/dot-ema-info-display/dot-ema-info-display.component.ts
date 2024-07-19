import { NgIf } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnChanges, inject, signal } from '@angular/core';
import { Router } from '@angular/router';

import { ButtonModule } from 'primeng/button';

import { DotMessagePipe } from '@dotcms/ui';

import { EditEmaStore } from '../../../dot-ema-shell/store/dot-ema.store';
import { EDITOR_MODE } from '../../../shared/enums';
import { UVEStore } from '../../../store/dot-uve.store';
import { getIsDefaultVariant } from '../../../utils';

interface InfoOptions {
    icon: string;
    info: {
        message: string;
        args: string[];
    };
    action?: () => void;
    actionIcon?: string;
}

@Component({
    selector: 'dot-ema-info-display',
    standalone: true,
    imports: [NgIf, ButtonModule, DotMessagePipe],
    templateUrl: './dot-ema-info-display.component.html',
    styleUrls: ['./dot-ema-info-display.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotEmaInfoDisplayComponent implements OnChanges {
    protected options = signal<InfoOptions>(undefined);

    protected readonly store = inject(EditEmaStore);

    protected readonly uveStore = inject(UVEStore);
    protected readonly router = inject(Router);

    protected readonly editorMode = EDITOR_MODE;

    ngOnChanges() {
        const pageAPIResponse = this.uveStore.pageAPIResponse();
        const canEditPage = this.uveStore.canEditPage();

        // MOVE ALL OF THIS TO THE STORE
        if (this.uveStore.pageIsLocked()) {
            let message = 'editpage.locked-by';

            if (!pageAPIResponse.page.canLock) {
                message = 'editpage.locked-contact-with';
            }

            this.options.set({
                icon: 'pi pi-lock',
                info: {
                    message,
                    args: [pageAPIResponse.page.lockedByName]
                }
            });

            return;
        }

        if (!canEditPage) {
            this.options.set({
                icon: 'pi pi-exclamation-circle warning',
                info: { message: 'editema.dont.have.edit.permission', args: [] }
            });
        }

        if (this.uveStore.isDevicePreviewState()) {
            const device = this.uveStore.device();
            this.options.set({
                icon: device.icon,
                info: {
                    message: `${device.name} ${device.cssWidth} x ${device.cssHeight}`,
                    args: []
                },
                action: () => {
                    this.uveStore.clearDeviceAndSocialMedia();
                },
                actionIcon: 'pi pi-times'
            });
        } else if (this.uveStore.isSocialMediaPreviewState()) {
            const socialMedia = this.uveStore.socialMedia();

            this.options.set({
                icon: `pi pi-${socialMedia.toLowerCase()}`,
                info: {
                    message: `Viewing <b>${socialMedia}</b> social media preview`,
                    args: []
                },
                action: () => {
                    this.uveStore.clearDeviceAndSocialMedia();
                },
                actionIcon: 'pi pi-times'
            });
        } else if (canEditPage && !getIsDefaultVariant(pageAPIResponse.viewAs.variantId)) {
            const variantId = pageAPIResponse.viewAs.variantId;

            const currentExperiment = this.uveStore.experiment?.();

            const name =
                currentExperiment?.trafficProportion.variants.find(
                    (variant) => variant.id === variantId
                )?.name ?? 'Unknown Variant';

            this.options.set({
                info: {
                    message: canEditPage ? 'editpage.editing.variant' : 'editpage.viewing.variant',
                    args: [name]
                },
                icon: 'pi pi-file-edit',
                action: () => {
                    this.router.navigate(
                        [
                            '/edit-page/experiments/',
                            currentExperiment.pageId,
                            currentExperiment.id,
                            'configuration'
                        ],
                        {
                            queryParams: {
                                mode: null,
                                variantName: null,
                                experimentId: null
                            },
                            queryParamsHandling: 'merge'
                        }
                    );
                },
                actionIcon: 'pi pi-arrow-left'
            });
        }
    }
}
