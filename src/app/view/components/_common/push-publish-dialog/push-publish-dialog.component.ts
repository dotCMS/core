import {
    Component,
    Input,
    Output,
    EventEmitter,
    ViewEncapsulation,
    ViewChild
} from '@angular/core';
import { FormGroup, FormBuilder, Validators } from '@angular/forms';
import { OnInit } from '@angular/core/src/metadata/lifecycle_hooks';
import { PushPublishService } from '@services/push-publish/push-publish.service';
import { SelectItem } from 'primeng/primeng';
import { DotMessageService } from '@services/dot-messages-service';
import { LoggerService } from 'dotcms-js';
@Component({
    encapsulation: ViewEncapsulation.None,
    selector: 'dot-push-publish-dialog',
    styleUrls: ['./push-publish-dialog.component.scss'],
    templateUrl: 'push-publish-dialog.component.html'
})
export class PushPublishContentTypesDialogComponent implements OnInit {
    form: FormGroup;
    pushActions: SelectItem[];
    dateFieldMinDate = new Date();

    @Input()
    assetIdentifier: string;
    @Output()
    cancel = new EventEmitter<boolean>();
    @ViewChild('formEl')
    formEl: HTMLFormElement;

    constructor(
        private pushPublishService: PushPublishService,
        public fb: FormBuilder,
        public dotMessageService: DotMessageService,
        public loggerService: LoggerService
    ) {}

    ngOnInit() {
        this.dotMessageService
            .getMessages([
                'contenttypes.content.push_publish',
                'contenttypes.content.push_publish.action.push',
                'contenttypes.content.push_publish.action.remove',
                'contenttypes.content.push_publish.action.pushremove',
                'contenttypes.content.push_publish.I_want_To',
                'contenttypes.content.push_publish.force_push',
                'contenttypes.content.push_publish.publish_date',
                'contenttypes.content.push_publish.expire_date',
                'contenttypes.content.push_publish.push_to',
                'contenttypes.content.push_publish.push_to_errormsg',
                'contenttypes.content.push_publish.form.cancel',
                'contenttypes.content.push_publish.form.push',
                'contenttypes.content.push_publish.publish_date_errormsg',
                'contenttypes.content.push_publish.expire_date_errormsg'
            ])
            .subscribe(() => {
                this.pushActions = [
                    {
                        label: this.dotMessageService.get(
                            'contenttypes.content.push_publish.action.push'
                        ),
                        value: 'publish'
                    },
                    {
                        label: this.dotMessageService.get(
                            'contenttypes.content.push_publish.action.remove'
                        ),
                        value: 'expire'
                    },
                    {
                        label: this.dotMessageService.get(
                            'contenttypes.content.push_publish.action.pushremove'
                        ),
                        value: 'publishexpire'
                    }
                ];

                this.initForm();
            });
    }

    /**
     * Close the dialog and reset the form
     * @memberof PushPublishContentTypesDialogComponent
     */
    close(): void {
        this.cancel.emit(true);
        this.initForm();
    }

    /**
     * When form is submitted
     * If form is valid then call pushPublishService with contentTypeId and form value params
     * @param any $event
     * @memberof PushPublishContentTypesDialogComponent
     */
    submitPushAction(_event): void {
        if (this.form.valid) {
            this.pushPublishService
                .pushPublishContent(this.assetIdentifier, this.form.value)
                .subscribe((result: any) => {
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
            pushActionSelected: [this.pushActions[0].value || '', [Validators.required]],
            publishdate: [new Date(), [Validators.required]],
            expiredate: [new Date(), [Validators.required]],
            environment: ['', [Validators.required]],
            forcePush: false
        });
    }
}
