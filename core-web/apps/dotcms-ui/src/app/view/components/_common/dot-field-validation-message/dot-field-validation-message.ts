/*
- TODO: maybe crawl the html to find the form parent and save one @Input
*/

import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    Input,
    OnDestroy
} from '@angular/core';
import { UntypedFormControl, ValidationErrors } from '@angular/forms';
import { takeUntil } from 'rxjs/operators';
import { Subject } from 'rxjs';
import { DotMessageService } from '@services/dot-message/dot-messages.service';

type DefaultsNGValidatorsTypes = 'maxlength';
// TODO: add the defaults messages for defaults Angular validators
const NG_DEFAULT_VALIDATORS_ERRORS_MSG: Record<DefaultsNGValidatorsTypes, string> = {
    maxlength: 'contentType.errors.input.maxlength'
};

@Component({
    selector: 'dot-field-validation-message',
    templateUrl: './dot-field-validation-message.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotFieldValidationMessageComponent implements OnDestroy {
    defaultMessage: string;
    errorMsg = '';
    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor(
        private readonly cd: ChangeDetectorRef,
        private readonly dotMessageService: DotMessageService
    ) {}

    @Input()
    set message(msg: string) {
        this.defaultMessage = msg;
        this.cd.markForCheck();
    }

    _field: UntypedFormControl;

    @Input()
    set field(control: UntypedFormControl) {
        this._field = control;
        control.statusChanges.pipe(takeUntil(this.destroy$)).subscribe(() => {
            this.errorMsg = this.getErrors(control.errors);
            this.cd.markForCheck();
        });
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    getErrors(errors: ValidationErrors) {
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
                const { requiredLength, actualLength } = value;
                switch (key) {
                    case 'maxlength':
                        errorTranslated = this.dotMessageService.get(
                            NG_DEFAULT_VALIDATORS_ERRORS_MSG[key],
                            requiredLength,
                            actualLength
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
