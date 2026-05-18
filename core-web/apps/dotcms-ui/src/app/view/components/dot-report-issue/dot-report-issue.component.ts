import { DOCUMENT } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    computed,
    effect,
    inject,
    model,
    output,
    signal,
    viewChild
} from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import {
    AbstractControl,
    FormBuilder,
    FormGroup,
    ReactiveFormsModule,
    ValidationErrors,
    ValidatorFn,
    Validators
} from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { DialogModule } from 'primeng/dialog';
import { FileSelectEvent, FileUpload, FileUploadModule } from 'primeng/fileupload';
import { TextareaModule } from 'primeng/textarea';

import { map } from 'rxjs/operators';

import {
    DotGlobalMessageService,
    DotHttpErrorManagerService,
    DotMessageService,
    DotPropertiesService
} from '@dotcms/data-access';
import { DotFieldRequiredDirective, DotMessagePipe } from '@dotcms/ui';

import {
    DotReportIssuePayload,
    DotReportIssueService
} from '../../../api/services/dot-report-issue.service';
import { LOCATION_TOKEN } from '../../../providers';

const ALLOWED_SCREENSHOT_TYPES = new Set(['image/png', 'image/jpeg', 'image/webp']);
const MAX_SCREENSHOT_SIZE_BYTES = 10 * 1024 * 1024;

/**
 * Dialog used to collect and submit issue reports from the toolbar user menu.
 */
@Component({
    selector: 'dot-report-issue',
    templateUrl: './dot-report-issue.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [
        ReactiveFormsModule,
        DialogModule,
        ButtonModule,
        CheckboxModule,
        FileUploadModule,
        TextareaModule,
        DotFieldRequiredDirective,
        DotMessagePipe
    ]
})
export class DotReportIssueComponent {
    readonly shutdown = output<void>();
    readonly visible = model<boolean>(false);

    private readonly fb = inject(FormBuilder);
    private readonly dotMessageService = inject(DotMessageService);
    private readonly dotGlobalMessageService = inject(DotGlobalMessageService);
    private readonly dotHttpErrorManagerService = inject(DotHttpErrorManagerService);
    private readonly dotReportIssueService = inject(DotReportIssueService);
    private readonly dotPropertiesService = inject(DotPropertiesService);
    private readonly location = inject<Location>(LOCATION_TOKEN);
    private readonly document = inject(DOCUMENT);
    private readonly screenshotUploadRef = viewChild<FileUpload>('screenshotUpload');

    readonly operatorAllowsPII = toSignal(
        this.dotPropertiesService
            .getKey('boolean:REPORT_ISSUE_INCLUDE_USER_PII')
            .pipe(map((value) => value !== false && value !== 'false')),
        { initialValue: true }
    );

    readonly form: FormGroup = this.fb.group({
        description: ['', [Validators.required, this.trimmedRequiredValidator()]],
        screenshot: [null as File | null, [this.screenshotValidator()]],
        anonymous: [false]
    });

    readonly screenshotFile = signal<File | null>(null);
    readonly isSubmitting = signal(false);
    readonly hasSubmitted = signal(false);
    readonly errorMessage = signal('');

    readonly errorMessages = {
        descriptionRequired: this.dotMessageService.get(
            'error.form.mandatory',
            this.dotMessageService.get('report-an-issue.description')
        ),
        invalidFileType: this.dotMessageService.get('report-an-issue.screenshot.invalid-type'),
        maxFileSize: this.dotMessageService.get('report-an-issue.screenshot.max-size')
    };

    private readonly formStatus = toSignal(this.form.statusChanges, {
        initialValue: this.form.status
    });

    readonly screenshotErrorMessage = computed(() => {
        this.formStatus();
        const errors = this.form.get('screenshot')?.errors;

        if (!errors) {
            return null;
        }

        if (errors['invalidFileType']) {
            return this.errorMessages.invalidFileType;
        }

        if (errors['maxFileSize']) {
            return this.errorMessages.maxFileSize;
        }

        return null;
    });

    constructor() {
        effect(() => {
            if (this.visible()) {
                this.resetForm();
            }
        });

        effect(() => {
            const anonymousControl = this.form.get('anonymous');
            if (!anonymousControl) {
                return;
            }
            if (this.operatorAllowsPII()) {
                anonymousControl.enable({ emitEvent: false });
            } else {
                anonymousControl.setValue(true, { emitEvent: false });
                anonymousControl.disable({ emitEvent: false });
            }
        });
    }

    /**
     * Request the dialog to close.
     */
    requestClose(): void {
        this.visible.set(false);
    }

    /**
     * Reset component state after the dialog has been hidden and notify the parent.
     */
    onDialogHide(): void {
        this.resetForm();
        this.shutdown.emit();
    }

    /**
     * Capture the selected screenshot file and re-run screenshot validation.
     *
     * @param event - PrimeNG file selection event.
     */
    onScreenshotSelected(event: FileSelectEvent): void {
        const file = event.files?.[0] ?? null;

        this.screenshotFile.set(file);
        this.errorMessage.set('');
        this.form.get('screenshot')?.setValue(file);
        this.form.get('screenshot')?.markAsTouched();
        this.form.get('screenshot')?.updateValueAndValidity();
    }

    /**
     * Clear the current screenshot selection from the form and uploader widget.
     */
    removeScreenshot(): void {
        this.screenshotFile.set(null);
        this.errorMessage.set('');
        this.form.get('screenshot')?.setValue(null);
        this.form.get('screenshot')?.markAsTouched();
        this.form.get('screenshot')?.updateValueAndValidity();

        const uploader = this.screenshotUploadRef();
        if (uploader && uploader.files.length > 0) {
            uploader.clear();
        }
    }

    /**
     * Handle the uploader clear event.
     */
    onScreenshotClear(): void {
        this.removeScreenshot();
    }

    /**
     * Validate the form and submit the issue to the backend.
     */
    save(): void {
        this.hasSubmitted.set(true);

        if (this.form.invalid) {
            this.form.markAllAsTouched();
            return;
        }

        this.errorMessage.set('');
        this.isSubmitting.set(true);
        const payload = this.buildPayload();

        this.dotReportIssueService.reportIssue(payload).subscribe({
            next: () => {
                this.isSubmitting.set(false);
                this.dotGlobalMessageService.success(
                    this.dotMessageService.get('report-an-issue.success')
                );
                this.requestClose();
            },
            error: (error) => {
                this.isSubmitting.set(false);
                this.errorMessage.set(this.getRequestErrorMessage(error));
                this.dotHttpErrorManagerService.handle(error).subscribe();
            }
        });
    }

    /**
     * Build the payload sent to the report issue API, including browser metadata.
     *
     * @returns The multipart payload model for the report issue service.
     */
    private buildPayload(): DotReportIssuePayload {
        const description = (this.form.get('description')?.value as string).trim();
        const metadata: Record<string, string> = {
            browser: window.navigator.userAgent,
            platform: window.navigator.platform,
            url: this.location.href,
            viewport: `${window.innerWidth}x${window.innerHeight}`
        };

        if (this.document.referrer) {
            metadata.referrer = this.document.referrer;
        }

        return {
            description,
            metadata,
            screenshot: this.screenshotFile(),
            anonymous: this.form.get('anonymous')?.value === true
        };
    }

    /**
     * Restore the dialog form to its initial state.
     */
    private resetForm(): void {
        this.isSubmitting.set(false);
        this.hasSubmitted.set(false);
        this.errorMessage.set('');
        this.screenshotFile.set(null);
        this.form.reset({
            description: '',
            screenshot: null,
            anonymous: !this.operatorAllowsPII()
        });
    }

    /**
     * Resolve the most useful user-facing error message from a failed request.
     *
     * @param error - Request error returned by Angular's HTTP client.
     * @returns A localized fallback or the best available backend error message.
     */
    private getRequestErrorMessage(error: unknown): string {
        const fallback = this.dotMessageService.get('report-an-issue.error');

        if (typeof error !== 'object' || error === null) {
            return fallback;
        }

        const httpError = error as {
            error?: { message?: string; errors?: Array<{ message?: string }> } | string;
            message?: string;
            statusText?: string;
        };

        if (typeof httpError.error === 'string') {
            try {
                const parsed = JSON.parse(httpError.error) as { message?: string };
                return parsed.message || httpError.message || fallback;
            } catch {
                return httpError.error || httpError.message || fallback;
            }
        }

        if (httpError.error?.errors?.[0]?.message) {
            return httpError.error.errors[0].message;
        }

        if (httpError.error?.message) {
            return httpError.error.message;
        }

        return httpError.message || httpError.statusText || fallback;
    }

    /**
     * Require a non-empty string after trimming whitespace.
     *
     * @returns A validator that fails when the control only contains whitespace.
     */
    private trimmedRequiredValidator(): ValidatorFn {
        return (control: AbstractControl): ValidationErrors | null => {
            const value = control.value;

            if (typeof value !== 'string') {
                return null;
            }

            return value.trim().length ? null : { required: true };
        };
    }

    /**
     * Validate screenshot type and file size before submission.
     *
     * @returns A validator for the optional screenshot control.
     */
    private screenshotValidator(): ValidatorFn {
        return (control: AbstractControl): ValidationErrors | null => {
            const file = control.value as File | null;

            if (!file) {
                return null;
            }

            if (!ALLOWED_SCREENSHOT_TYPES.has(file.type)) {
                return { invalidFileType: true };
            }

            if (file.size > MAX_SCREENSHOT_SIZE_BYTES) {
                return { maxFileSize: true };
            }

            return null;
        };
    }
}
