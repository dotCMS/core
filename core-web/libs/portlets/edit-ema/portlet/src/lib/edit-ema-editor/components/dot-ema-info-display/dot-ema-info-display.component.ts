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

import { EditEmaStore } from '../../../dot-ema-shell/store/dot-ema.store';
import { EDITOR_MODE } from '../../../shared/enums';
import { EditorData } from '../../../shared/models';

interface InfoOptions {
    icon: string;
    info: string;
    action: () => void;
}

@Component({
    selector: 'dot-ema-info-display',
    standalone: true,
    imports: [NgIf, ButtonModule],
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

    protected readonly editorMode = EDITOR_MODE;

    ngOnChanges() {
        if (this.editorData.mode === this.editorMode.DEVICE) {
            this.options.set({
                icon: this.editorData.device.icon,
                info: `${this.editorData.device.name} ${this.editorData.device.cssWidth} x ${this.editorData.device.cssHeight}`,
                action: () => this.goToEdit()
            });
        } else if (this.editorData.mode === this.editorMode.VARIANT) {
            this.options.set({
                icon: 'pi pi-file-edit',
                info: 'Editing <b>Some</b> Variant',
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
        }
    }

    protected goToEdit() {
        this.store.updateEditorData({
            mode: this.editorMode.EDIT
        });
    }
}
