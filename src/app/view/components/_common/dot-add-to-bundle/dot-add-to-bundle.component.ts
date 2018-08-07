import { Observable } from 'rxjs/Observable';
import { Component, Input, Output, EventEmitter, ViewEncapsulation, ViewChild, AfterViewInit } from '@angular/core';
import { FormGroup, FormBuilder, Validators } from '@angular/forms';
import { OnInit } from '@angular/core/src/metadata/lifecycle_hooks';
import { DotMessageService } from '../../../../api/services/dot-messages-service';
import { LoggerService } from 'dotcms-js/dotcms-js';
import { AddToBundleService } from '../../../../api/services/add-to-bundle/add-to-bundle.service';
import { DotBundle } from '../../../../shared/models/dot-bundle/dot-bundle';
import { Dropdown } from 'primeng/primeng';
import { mergeMap } from 'rxjs/operators';

const LAST_BUNDLE_USED = 'lastBundleUsed';

@Component({
    encapsulation: ViewEncapsulation.None,
    selector: 'dot-add-to-bundle',
    templateUrl: 'dot-add-to-bundle.component.html'
})
export class DotAddToBundleComponent implements OnInit, AfterViewInit {
    form: FormGroup;
    bundle$: Observable<DotBundle[]>;
    placeholder: string;

    @Input() assetIdentifier: string;
    @Output() cancel = new EventEmitter<boolean>();
    @ViewChild('formEl') formEl: HTMLFormElement;
    @ViewChild('addBundleDropdown') addBundleDropdown: Dropdown;

    constructor(
        private addToBundleService: AddToBundleService,
        public fb: FormBuilder,
        public dotMessageService: DotMessageService,
        public loggerService: LoggerService
    ) {}

    ngOnInit() {
        const keys = [
            'contenttypes.content.add_to_bundle',
            'contenttypes.content.add_to_bundle.select',
            'contenttypes.content.add_to_bundle.type',
            'contenttypes.content.add_to_bundle.errormsg',
            'contenttypes.content.add_to_bundle.form.cancel',
            'contenttypes.content.add_to_bundle.form.add'
        ];

        this.initForm();

        this.bundle$ = this.dotMessageService.getMessages(keys).pipe(
            mergeMap(messages => {
                return this.addToBundleService.getBundles().pipe(
                    mergeMap((bundles: DotBundle[]) => {
                        setTimeout(() => {
                            this.placeholder = bundles.length
                                ? messages['contenttypes.content.add_to_bundle.select']
                                : messages['contenttypes.content.add_to_bundle.type'];
                        });
                        this.form.get('addBundle').setValue(this.getDefaultBundle(bundles) ? this.getDefaultBundle(bundles).name : '');
                        return Observable.of(bundles);
                    })
                );
            })
        );
    }

    ngAfterViewInit(): void {
        setTimeout(() => {
            this.addBundleDropdown.editableInputViewChild.nativeElement.focus();
        });
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
            this.addToBundleService.addToBundle(this.assetIdentifier, this.setBundleData()).subscribe((result: any) => {
                if (!result.errors) {
                    sessionStorage.setItem(LAST_BUNDLE_USED, JSON.stringify(this.setBundleData()));
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
        return lastBundle ? bundles.find(bundle => bundle.name === lastBundle.name) : null;
    }
}
