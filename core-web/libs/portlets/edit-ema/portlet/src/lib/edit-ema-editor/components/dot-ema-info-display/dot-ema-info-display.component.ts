import { NgIf } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { Router } from '@angular/router';

import { ButtonModule } from 'primeng/button';

import { DotMessagePipe } from '@dotcms/ui';

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
export class DotEmaInfoDisplayComponent {
    protected readonly uveStore = inject(UVEStore);
    protected readonly router = inject(Router);

    protected options = computed<InfoOptions>(() => {
        const pageAPIResponse = this.uveStore.$pageAPIResponse();
        const canEditPage = this.uveStore.$canEditPage();
        const device = this.uveStore.$device();
        const socialMedia = this.uveStore.$socialMedia();

        if (this.uveStore.$pageIsLocked()) {
            let message = 'editpage.locked-by';

            if (!pageAPIResponse.page.canLock) {
                message = 'editpage.locked-contact-with';
            }

            return {
                icon: 'pi pi-lock',
                info: {
                    message,
                    args: [pageAPIResponse.page.lockedByName]
                }
            };
        }

        if (device) {
            return {
                icon: device.icon,
                info: {
                    message: `${device.name} ${device.cssWidth} x ${device.cssHeight}`,
                    args: []
                },
                action: () => {
                    this.uveStore.clearDeviceAndSocialMedia();
                },
                actionIcon: 'pi pi-times'
            };
        } else if (socialMedia) {
            return {
                icon: `pi pi-${socialMedia.toLowerCase()}`,
                info: {
                    message: `Viewing <b>${socialMedia}</b> social media preview`,
                    args: []
                },
                action: () => {
                    this.uveStore.clearDeviceAndSocialMedia();
                },
                actionIcon: 'pi pi-times'
            };
        } else if (canEditPage && !getIsDefaultVariant(pageAPIResponse.viewAs.variantId)) {
            const variantId = pageAPIResponse.viewAs.variantId;

            const currentExperiment = this.uveStore.$experiment?.();

            const name =
                currentExperiment?.trafficProportion.variants.find(
                    (variant) => variant.id === variantId
                )?.name ?? 'Unknown Variant';

            return {
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
            };
        }

        if (!canEditPage) {
            return {
                icon: 'pi pi-exclamation-circle warning',
                info: { message: 'editema.dont.have.edit.permission', args: [] }
            };
        }

        return undefined;
    });
}
