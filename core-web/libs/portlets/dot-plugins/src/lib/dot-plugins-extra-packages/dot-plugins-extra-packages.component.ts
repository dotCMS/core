import { EMPTY } from 'rxjs';

import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { DynamicDialogRef } from 'primeng/dynamicdialog';
import { TextareaModule } from 'primeng/textarea';

import { catchError, take } from 'rxjs/operators';

import { DotHttpErrorManagerService, DotOsgiService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';

@Component({
    selector: 'dot-plugins-extra-packages',
    standalone: true,
    imports: [FormsModule, TextareaModule, ButtonModule, DotMessagePipe],
    templateUrl: './dot-plugins-extra-packages.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotPluginsExtraPackagesComponent implements OnInit {
    readonly #ref = inject(DynamicDialogRef);
    readonly #osgiService = inject(DotOsgiService);
    readonly #httpErrorManager = inject(DotHttpErrorManagerService);

    extraPackages = signal('');
    saving = signal(false);

    ngOnInit(): void {
        this.#osgiService
            .getExtraPackages()
            .pipe(
                take(1),
                catchError((error) => {
                    this.#httpErrorManager.handle(error);
                    return EMPTY;
                })
            )
            .subscribe((response) => {
                this.extraPackages.set(response.entity ?? '');
            });
    }

    save(): void {
        const text = this.extraPackages();
        this.saving.set(true);
        this.#osgiService
            .updateExtraPackages(text)
            .pipe(
                take(1),
                catchError((error) => {
                    this.#httpErrorManager.handle(error);
                    this.saving.set(false);
                    return EMPTY;
                })
            )
            .subscribe(() => {
                this.saving.set(false);
                this.#ref.close(true);
            });
    }

    close(): void {
        this.#ref.close(false);
    }
}
