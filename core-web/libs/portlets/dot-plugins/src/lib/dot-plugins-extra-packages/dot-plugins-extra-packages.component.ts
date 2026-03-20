import { EMPTY } from 'rxjs';

import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DynamicDialogRef } from 'primeng/dynamicdialog';
import { TextareaModule } from 'primeng/textarea';

import { catchError, take } from 'rxjs/operators';

import {
    DotHttpErrorManagerService,
    DotMessageService,
    DotOsgiService,
    OSGI_EXTRA_PACKAGES_RESET
} from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';

@Component({
    selector: 'dot-plugins-extra-packages',
    standalone: true,
    imports: [FormsModule, TextareaModule, ButtonModule, ConfirmDialogModule, DotMessagePipe],
    templateUrl: './dot-plugins-extra-packages.component.html',
    providers: [ConfirmationService],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotPluginsExtraPackagesComponent implements OnInit {
    readonly #ref = inject(DynamicDialogRef);
    readonly #osgiService = inject(DotOsgiService);
    readonly #httpErrorManager = inject(DotHttpErrorManagerService);
    readonly #confirmationService = inject(ConfirmationService);
    readonly #dotMessageService = inject(DotMessageService);

    extraPackages = signal('');
    saving = signal(false);
    resetting = signal(false);

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

    confirmReset(): void {
        this.#confirmationService.confirm({
            message: this.#dotMessageService.get('plugins.extra-packages.reset.confirm.message'),
            header: this.#dotMessageService.get('plugins.extra-packages.reset'),
            acceptLabel: this.#dotMessageService.get('Ok'),
            rejectLabel: this.#dotMessageService.get('Cancel'),
            acceptButtonStyleClass: 'p-button-outlined',
            rejectButtonStyleClass: 'p-button-primary',
            defaultFocus: 'reject',
            closable: true,
            closeOnEscape: true,
            accept: () => this.#doReset()
        });
    }

    #doReset(): void {
        this.resetting.set(true);
        this.#osgiService
            .updateExtraPackages(OSGI_EXTRA_PACKAGES_RESET)
            .pipe(
                take(1),
                catchError((error) => {
                    this.#httpErrorManager.handle(error);
                    this.resetting.set(false);
                    return EMPTY;
                })
            )
            .subscribe(() => {
                this.resetting.set(false);
                this.#ref.close('restart');
            });
    }
}
