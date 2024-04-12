import { NgIf } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    Input,
    OnChanges,
    inject,
    signal
} from '@angular/core';
import { Router } from '@angular/router';

import { ButtonModule } from 'primeng/button';

import { DotExperiment } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { EditEmaStore } from '../../../dot-ema-shell/store/dot-ema.store';
import { EDITOR_MODE } from '../../../shared/enums';
import { EditorData } from '../../../shared/models';
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
    @Input() editorData: EditorData;
    @Input() currentExperiment: DotExperiment;

    protected options = signal<InfoOptions>(undefined);

    protected readonly store = inject(EditEmaStore);
    protected readonly router = inject(Router);

    protected readonly editorMode = EDITOR_MODE;

    ngOnChanges() {
        if (this.editorData.page.isLocked) {
            let message = 'editpage.locked-by';

            if (!this.editorData.page.canLock) {
                message = 'editpage.locked-contact-with';
            }

            this.options.set({
                icon: 'pi pi-lock',
                info: {
                    message,
                    args: [this.editorData.page.lockedByUser]
                }
            });

            return;
        }

        if (!this.editorData.canEditPage) {
            this.options.set({
                icon: 'pi pi-exclamation-circle warning',
                info: { message: 'editema.dont.have.edit.permission', args: [] }
            });
        }

        if (this.editorData.mode === this.editorMode.DEVICE) {
            this.options.set({
                icon: this.editorData.device.icon,
                info: {
                    message: `${this.editorData.device.name} ${this.editorData.device.cssWidth} x ${this.editorData.device.cssHeight}`,
                    args: []
                },
                action: () => {
                    this.goToEdit();
                },
                actionIcon: 'pi pi-times'
            });
        } else if (this.editorData.mode === this.editorMode.SOCIAL_MEDIA) {
            this.options.set({
                icon: `pi pi-${this.editorData.socialMedia.toLowerCase()}`,
                info: {
                    message: `Viewing <b>${this.editorData.socialMedia}</b> social media preview`,
                    args: []
                },
                action: () => {
                    this.goToEdit();
                },
                actionIcon: 'pi pi-times'
            });
        } else if (
            this.editorData.canEditPage &&
            (this.editorData.mode === this.editorMode.EDIT_VARIANT ||
                this.editorData.mode === this.editorMode.PREVIEW_VARIANT)
        ) {
            const variantId = this.editorData.variantId;
            const name =
                this.currentExperiment.trafficProportion.variants.find(
                    (variant) => variant.id === variantId
                )?.name ?? 'Unknown Variant';

            this.options.set({
                info: {
                    message:
                        this.editorData.mode === this.editorMode.EDIT_VARIANT
                            ? 'editpage.editing.variant'
                            : 'editpage.viewing.variant',
                    args: [name]
                },
                icon: 'pi pi-file-edit',
                action: () => {
                    this.router.navigate(
                        [
                            '/edit-page/experiments/',
                            this.currentExperiment.pageId,
                            this.currentExperiment.id,
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

    public goToEdit() {
        const isNotDefaultVariant = !getIsDefaultVariant(this.editorData.variantId);

        if (isNotDefaultVariant) {
            this.store.updateEditorData({
                mode: this.editorData.canEditVariant
                    ? this.editorMode.EDIT_VARIANT
                    : this.editorMode.PREVIEW_VARIANT
            });
        } else {
            this.store.updateEditorData({ mode: this.editorMode.EDIT });
        }
    }
}
