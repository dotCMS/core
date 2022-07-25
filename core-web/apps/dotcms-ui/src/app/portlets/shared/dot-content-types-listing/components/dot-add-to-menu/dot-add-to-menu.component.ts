import { Observable, Subject } from 'rxjs';
import {
    Component,
    Input,
    Output,
    EventEmitter,
    OnInit,
    OnDestroy,
    ViewChild,
    ElementRef
} from '@angular/core';
import { UntypedFormGroup, UntypedFormBuilder, Validators } from '@angular/forms';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { DotDialogActions } from '@components/dot-dialog/dot-dialog.component';
import { DotMenuService } from '@dotcms/app/api/services/dot-menu.service';
import { switchMap, take, takeUntil, tap } from 'rxjs/operators';
import { DotMenu } from '@dotcms/app/shared/models/navigation';
import { DotCMSContentType } from '@dotcms/dotcms-models';
import {
    DotAddToMenuService,
    DotCreateCustomTool
} from '@dotcms/app/api/services/add-to-menu/add-to-menu.service';

@Component({
    selector: 'dot-add-to-menu',
    templateUrl: 'dot-add-to-menu.component.html'
})
export class DotAddToMenuComponent implements OnInit, OnDestroy {
    form: UntypedFormGroup;
    menu$: Observable<DotMenu[]>;
    placeholder = '';
    dialogShow = false;
    dialogActions: DotDialogActions;

    @Input() contentType: DotCMSContentType;
    @Output() cancel = new EventEmitter<boolean>();

    @ViewChild('titleName', { static: true }) titleName: ElementRef;

    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor(
        public fb: UntypedFormBuilder,
        private dotMessageService: DotMessageService,
        private dotMenuService: DotMenuService,
        private dotAddToMenuService: DotAddToMenuService
    ) {}

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

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    /**
     * Close dialog modal and reset form
     * @memberof DotAddToBundleComponent
     */
    close(): void {
        this.cancel.emit(true);
    }

    /**
     * Add to bundle if form is valid
     * @memberof DotAddToBundleComponent
     */
    submit(): void {
        if (this.form.valid) {
            const params: DotCreateCustomTool = {
                portletName: this.form.get('title').value,
                contentTypes: this.contentType.variable,
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
            title: [this.contentType.name, [Validators.required]]
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
