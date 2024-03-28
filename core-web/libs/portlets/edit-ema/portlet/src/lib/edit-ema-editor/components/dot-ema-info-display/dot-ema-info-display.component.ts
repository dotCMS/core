import { NgIf } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    Input,
    OnChanges,
    inject,
    signal
} from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { ButtonModule } from 'primeng/button';

import { take } from 'rxjs/operators';

import { DotExperimentsService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';

import { EditEmaStore } from '../../../dot-ema-shell/store/dot-ema.store';
import { EDITOR_MODE } from '../../../shared/enums';
import { EditorData } from '../../../shared/models';

interface InfoOptions {
    icon: string;
    info: {
        message: string;
        args: string[];
    };
    action?: () => void;
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

    protected options = signal<InfoOptions>(undefined);

    protected readonly store = inject(EditEmaStore);
    protected readonly router = inject(Router);
    protected readonly activatedRoute = inject(ActivatedRoute);
    protected readonly experimentsService = inject(DotExperimentsService);

    protected readonly editorMode = EDITOR_MODE;

    ngOnChanges() {
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
                action: () => this.goToEdit()
            });
        } else if (
            this.editorData.mode === this.editorMode.VARIANT &&
            this.editorData.canEditPage
        ) {
            const info = this.editorData.variantInfo.canEditVariant
                ? 'editpage.editing.variant'
                : 'editpage.viewing.variant';
            const experimentId = this.activatedRoute.snapshot.queryParams['experimentId'];
            const variantId = this.activatedRoute.snapshot.queryParams['variantName'];

            this.experimentsService
                .getById(experimentId)
                .pipe(take(1))
                .subscribe((experiment) => {
                    const name =
                        experiment.trafficProportion.variants.find(
                            (variant) => variant.id === variantId
                        )?.name ?? 'Unknown Variant';

                    this.options.set({
                        info: {
                            message: info,
                            args: [name]
                        },
                        icon: 'pi pi-file-edit',
                        action: () =>
                            this.router.navigate(
                                [
                                    '/edit-page/experiments/',
                                    this.editorData.variantInfo.pageId,
                                    this.activatedRoute.snapshot.queryParams['experimentId'],
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
                            )
                    });
                });
        }
    }

    protected goToEdit() {
        this.store.updateEditorData({
            mode: this.editorMode.EDIT
        });
    }
}
