/*
- TODO: maybe crawl the html to find the form parent and save one @Input
*/
/* eslint-disable @stylistic/padding-line-between-statements */

import { Subject } from 'rxjs';

import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    Input,
    OnDestroy,
    inject
} from '@angular/core';
import { AbstractControl, UntypedFormControl, ValidationErrors } from '@angular/forms';

import { takeUntil } from 'rxjs/operators';

import { DotMessageService } from '@dotcms/data-access';

import { DotMessagePipe } from '../../dot-message/dot-message.pipe';

type DefaultsNGValidatorsTypes = 'maxlength' | 'minlength' | 'required' | 'pattern';

const NG_DEFAULT_VALIDATORS_ERRORS_MSG: Record<DefaultsNGValidatorsTypes, string> = {
    maxlength: 'error.form.validator.maxlength',
    minlength: 'error.form.validator.minlength',
    required: 'error.form.validator.required',
    pattern: 'error.form.validator.pattern'
};

@Component({
    selector: 'dot-field-validation-message',
    templateUrl: './dot-field-validation-message.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [DotMessagePipe]
})
export class DotFieldValidationMessageComponent implements OnDestroy {
    private readonly cd = inject(ChangeDetectorRef);
    private readonly dotMessageService = inject(DotMessageService);

    @Input()
    patternErrorMessage: string;

    defaultMessage: string;
    errorMsg = '';
    private destroy$: Subject<boolean> = new Subject<boolean>();

    /**
     * Manual message when the input has an error.
     * @param {string} msg
     */
    @Input()
    set message(msg: string) {
        this.defaultMessage = msg;
        this.cd.markForCheck();
    }

    _field: UntypedFormControl | AbstractControl;

    /**
     * Form control to check
     * @param control
     */
    @Input()
    set field(control: UntypedFormControl | AbstractControl) {
        if (control) {
            this._field = control;
            control.statusChanges.pipe(takeUntil(this.destroy$)).subscribe(() => {
                this.errorMsg = this.getErrors(control.errors);
                this.cd.detectChanges();
            });
        }
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    /**
     * Return a default Message if are sent, or use NG_DEFAULT_VALIDATORS_ERRORS_MSG map
     * with the DefaultsNGValidatorsTypes of Angular
     *
     * @param {ValidationErrors} errors
     * @private
     */
    private getErrors(errors: ValidationErrors) {
        let errorMsgs = [];
        if (errors) {
            errorMsgs = [
                ...this.getMsgDefaultValidators(errors),
                ...this.getMsgCustomsValidators(errors)
            ];
        }

        return this.defaultMessage ? this.defaultMessage : errorMsgs.slice(0, 1)[0];
    }

    private getMsgDefaultValidators(errors: ValidationErrors) {
        let errorMsgs = [];
        Object.entries(errors).forEach(([key, value]) => {
            if (key in NG_DEFAULT_VALIDATORS_ERRORS_MSG) {
                let errorTranslated = '';
                const { requiredLength, requiredPattern } = value;
                switch (key) {
                    case 'maxlength':
                    case 'minlength':
                        errorTranslated = this.dotMessageService.get(
                            NG_DEFAULT_VALIDATORS_ERRORS_MSG[key],
                            requiredLength
                        );
                        break;

                    case 'pattern':
                        errorTranslated = this.dotMessageService.get(
                            this.patternErrorMessage || NG_DEFAULT_VALIDATORS_ERRORS_MSG[key],
                            requiredPattern
                        );
                        break;

                    case 'required':
                        errorTranslated = this.dotMessageService.get(
                            NG_DEFAULT_VALIDATORS_ERRORS_MSG[key]
                        );
                        break;

                    default:
                        errorTranslated = NG_DEFAULT_VALIDATORS_ERRORS_MSG[key];
                        break;
                }

                errorMsgs = [...errorMsgs, errorTranslated];
            }
        });

        return errorMsgs;
    }

    private getMsgCustomsValidators(errors: ValidationErrors) {
        let errorMsgs = [];
        Object.entries(errors).forEach(([, value]) => {
            if (typeof value === 'string') {
                errorMsgs = [...errorMsgs, value];
            }
        });

        return errorMsgs;
    }
}
