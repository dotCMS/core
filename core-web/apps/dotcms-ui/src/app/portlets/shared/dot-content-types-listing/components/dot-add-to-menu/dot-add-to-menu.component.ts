import { Observable, Subject } from 'rxjs';

import { CommonModule } from '@angular/common';
import {
    Component,
    ElementRef,
    inject,
    input,
    OnChanges,
    OnDestroy,
    OnInit,
    output,
    SimpleChanges,
    viewChild
} from '@angular/core';
import {
    ReactiveFormsModule,
    UntypedFormBuilder,
    UntypedFormGroup,
    Validators
} from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { RadioButtonModule } from 'primeng/radiobutton';
import { SelectModule } from 'primeng/select';

import { switchMap, take, takeUntil, tap } from 'rxjs/operators';

import { DotMessageService } from '@dotcms/data-access';
import { DotCMSContentType, DotDialogActions, DotMenu } from '@dotcms/dotcms-models';
import {
    DotAutofocusDirective,
    DotFieldRequiredDirective,
    DotFieldValidationMessageComponent,
    DotMessagePipe
} from '@dotcms/ui';

import {
    DotAddToMenuService,
    DotCreateCustomTool
} from '../../../../../api/services/add-to-menu/add-to-menu.service';
import { DotMenuService } from '../../../../../api/services/dot-menu.service';

@Component({
    selector: 'dot-add-to-menu',
    templateUrl: 'dot-add-to-menu.component.html',
    imports: [
        CommonModule,
        ReactiveFormsModule,
        DialogModule,
        ButtonModule,
        SelectModule,
        InputTextModule,
        RadioButtonModule,
        DotAutofocusDirective,
        DotFieldValidationMessageComponent,
        DotFieldRequiredDirective,
        DotMessagePipe
    ]
})
export class DotAddToMenuComponent implements OnInit, OnDestroy, OnChanges {
    fb = inject(UntypedFormBuilder);
    private dotMessageService = inject(DotMessageService);
    private dotMenuService = inject(DotMenuService);
    private dotAddToMenuService = inject(DotAddToMenuService);

    form: UntypedFormGroup;
    menu$: Observable<DotMenu[]>;
    placeholder = '';
    dialogShow = false;
    dialogActions: DotDialogActions;

    readonly $contentType = input.required<DotCMSContentType>({ alias: 'contentType' });
    readonly $cancel = output<boolean>();

    readonly $titleName = viewChild.required<ElementRef>('titleName');

    private destroy$: Subject<boolean> = new Subject<boolean>();

    ngOnInit() {
        this.initForm();
        this.setDialogConfig(this.form);
        this.menu$ = this.dotMenuService.loadMenu(true).pipe(
            take(1),
            tap((menu: DotMenu[]) => {
                this.form.patchValue({
                    menuOption: menu[0].id
                });
            })
        );
    }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes.$contentType) {
            this.dialogShow = !!this.$contentType();
            if (this.$contentType()) {
                this.initForm();
            }
        }
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    /**
     * Close dialog modal and reset form
     * @memberof DotAddToBundleComponent
     */
    close(): void {
        this.$cancel.emit(true);
        this.dialogShow = false;
    }

    /**
     * Add to bundle if form is valid
     * @memberof DotAddToBundleComponent
     */
    submit(): void {
        if (this.form.valid) {
            const params: DotCreateCustomTool = {
                portletName: this.form.get('title').value,
                contentTypes: this.$contentType().variable,
                dataViewMode: this.form.get('defaultView').value
            };

            this.dotAddToMenuService
                .createCustomTool(params)
                .pipe(
                    take(1),
                    switchMap(() => {
                        return this.dotAddToMenuService
                            .addToLayout({
                                portletName: params.portletName,
                                dataViewMode: this.form.get('defaultView').value,
                                layoutId: this.form.get('menuOption').value
                            })
                            .pipe(take(1));
                    })
                )
                .subscribe(() => {
                    this.close();
                });
        }
    }

    private initForm(): void {
        this.form = this.fb.group({
            defaultView: ['list', [Validators.required]],
            menuOption: ['', [Validators.required]],
            title: [this.$contentType().name, [Validators.required]]
        });
    }

    private setDialogConfig(form: UntypedFormGroup): void {
        this.dialogActions = {
            accept: {
                action: () => {
                    this.submit();
                },
                label: this.dotMessageService.get('add'),
                disabled: !form.valid
            },
            cancel: {
                action: () => {
                    this.close();
                },
                label: this.dotMessageService.get('cancel')
            }
        };

        form.valueChanges.pipe(takeUntil(this.destroy$)).subscribe(() => {
            this.dialogActions = {
                ...this.dialogActions,
                accept: {
                    ...this.dialogActions.accept,
                    disabled: !this.form.valid
                }
            };
        });
    }
}
