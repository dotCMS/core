import { BehaviorSubject, Observable } from 'rxjs/Rx';
import { Component, Input, Output, EventEmitter, ViewEncapsulation, ViewChild } from '@angular/core';
import { FormGroup, FormBuilder, Validators } from '@angular/forms';
import { OnInit } from '@angular/core/src/metadata/lifecycle_hooks';
import { DotMessageService } from '../../../../api/services/dot-messages-service';
import { LoggerService } from 'dotcms-js/dotcms-js';
import { AddToBundleService } from '../../../../api/services/add-to-bundle/add-to-bundle.service';
import { DotBundle } from '../../../../shared/models/dot-bundle/dot-bundle';

@Component({
    encapsulation: ViewEncapsulation.None,
    selector: 'dot-add-to-bundle',
    templateUrl: 'dot-add-to-bundle.component.html'
})
export class DotAddToBundleComponent implements OnInit {
    form: FormGroup;
    bundle$: Observable<DotBundle[]>;
    placeholder: string;

    @Input() assetIdentifier: string;
    @Output() cancel = new EventEmitter<boolean>();
    @ViewChild('formEl') formEl: HTMLFormElement;

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

        this.bundle$ = this.addToBundleService.getBundles();

        this.dotMessageService.getMessages(keys).subscribe(messages => {
            this.addToBundleService.getBundles().subscribe(bundles => {
                this.placeholder = bundles.length
                    ? messages['contenttypes.content.add_to_bundle.select']
                    : messages['contenttypes.content.add_to_bundle.type'];
            });
        });

        this.initForm();
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
    submitBundle($event): void {
        if (this.form.valid) {
            this.addToBundleService.addToBundle(this.assetIdentifier, this.setBundleData()).subscribe((result: any) => {
                if (!result.errors) {
                    this.close();
                } else {
                    this.loggerService.debug(result.errorMessages);
                }
            });
            this.form.reset();
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
        if (typeof(this.form.value.addBundle) === 'string') {
            return {
                id: this.form.value.addBundle,
                name: this.form.value.addBundle
            };
        } else {
            return this.form.value.addBundle;
        }
    }
}
