import { Observable, Subject } from 'rxjs';
import {
    Component,
    Input,
    Output,
    EventEmitter,
    ViewChild,
    AfterViewInit,
    OnInit,
    OnDestroy
} from '@angular/core';
import { UntypedFormGroup, UntypedFormBuilder, Validators } from '@angular/forms';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { LoggerService } from '@dotcms/dotcms-js';
import { AddToBundleService } from '@services/add-to-bundle/add-to-bundle.service';
import { DotBundle } from '@models/dot-bundle/dot-bundle';
import { Dropdown } from 'primeng/dropdown';
import { map, tap, take, takeUntil } from 'rxjs/operators';
import { DotDialogActions } from '@components/dot-dialog/dot-dialog.component';
import { DotAjaxActionResponseView } from '@models/ajax-action-response/dot-ajax-action-response';

const LAST_BUNDLE_USED = 'lastSelectedBundle';

@Component({
    selector: 'dot-add-to-bundle',
    templateUrl: 'dot-add-to-bundle.component.html',
    styleUrls: ['dot-add-to-bundle.component.scss']
})
export class DotAddToBundleComponent implements OnInit, AfterViewInit, OnDestroy {
    form: UntypedFormGroup;
    bundle$: Observable<DotBundle[]>;
    placeholder = '';
    dialogShow = false;
    dialogActions: DotDialogActions;

    @Input() assetIdentifier: string;

    @Output() cancel = new EventEmitter<boolean>();

    @ViewChild('formEl', { static: true }) formEl: HTMLFormElement;

    @ViewChild('addBundleDropdown', { static: true }) addBundleDropdown: Dropdown;

    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor(
        private addToBundleService: AddToBundleService,
        public fb: UntypedFormBuilder,
        private dotMessageService: DotMessageService,
        public loggerService: LoggerService
    ) {}

    ngOnInit() {
        this.initForm();

        this.bundle$ = this.addToBundleService.getBundles().pipe(
            take(1),
            map((bundles: DotBundle[]) => {
                setTimeout(() => {
                    this.placeholder = bundles.length
                        ? this.dotMessageService.get('contenttypes.content.add_to_bundle.select')
                        : this.dotMessageService.get('contenttypes.content.add_to_bundle.type');
                }, 0);

                this.form
                    .get('addBundle')
                    .setValue(
                        this.getDefaultBundle(bundles) ? this.getDefaultBundle(bundles).name : ''
                    );
                return bundles;
            }),
            tap(() => {
                this.setDialogConfig(this.form);
                this.dialogShow = true;
            })
        );
    }

    ngAfterViewInit(): void {
        setTimeout(() => {
            this.addBundleDropdown.editableInputViewChild.nativeElement.focus();
        });
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
        this.initForm();
    }

    /**
     * Add to bundle if form is valid
     * @param {any} $event
     * @memberof DotAddToBundleComponent
     */
    submitBundle(_event): void {
        if (this.form.valid) {
            this.addToBundleService
                .addToBundle(this.assetIdentifier, this.setBundleData())
                .pipe(takeUntil(this.destroy$))
                .subscribe((result: DotAjaxActionResponseView) => {
                    if (!result.errors) {
                        sessionStorage.setItem(
                            LAST_BUNDLE_USED,
                            JSON.stringify(this.setBundleData())
                        );
                        this.form.reset();
                        this.close();
                    } else {
                        this.loggerService.debug(result.errorMessages);
                    }
                });
        }
    }

    /**
     * It submits the form from submit button
     * @memberof PushPublishContentTypesDialogComponent
     */
    submitForm(): void {
        this.formEl.ngSubmit.emit();
    }

    private initForm(): void {
        this.form = this.fb.group({
            addBundle: ['', [Validators.required]]
        });
    }

    private setBundleData(): DotBundle {
        if (typeof this.form.value.addBundle === 'string') {
            return {
                id: this.form.value.addBundle,
                name: this.form.value.addBundle
            };
        } else {
            return this.form.value.addBundle;
        }
    }

    private getDefaultBundle(bundles: DotBundle[]): DotBundle {
        const lastBundle: DotBundle = JSON.parse(sessionStorage.getItem(LAST_BUNDLE_USED));
        // return lastBundle ? this.bundle$.find(bundle => bundle.name === lastBundle.name) : null;
        return lastBundle ? bundles.find((bundle) => bundle.name === lastBundle.name) : null;
    }

    private setDialogConfig(form: UntypedFormGroup): void {
        this.dialogActions = {
            accept: {
                action: () => {
                    this.submitForm();
                },
                label: this.dotMessageService.get('contenttypes.content.add_to_bundle.form.add'),
                disabled: !form.valid
            },
            cancel: {
                action: () => {
                    this.close();
                },
                label: this.dotMessageService.get('contenttypes.content.add_to_bundle.form.cancel')
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
