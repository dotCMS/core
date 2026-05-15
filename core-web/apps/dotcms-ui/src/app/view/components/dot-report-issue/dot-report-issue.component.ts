import { DOCUMENT } from '@angular/common';

import {
    ChangeDetectionStrategy,
    Component,
    effect,
    inject,
    model,
    output,
    signal,
    viewChild
} from '@angular/core';
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
import { DialogModule } from 'primeng/dialog';
import { FileSelectEvent, FileUpload, FileUploadModule } from 'primeng/fileupload';
import { TextareaModule } from 'primeng/textarea';

import { take } from 'rxjs/operators';

import {
    DotGlobalMessageService,
    DotHttpErrorManagerService,
    DotMessageService
} from '@dotcms/data-access';
import { DotFieldRequiredDirective, DotMessagePipe } from '@dotcms/ui';

import {
    DotReportIssuePayload,
    DotReportIssueService
} from '../../../api/services/dot-report-issue.service';
import { LOCATION_TOKEN } from '../../../providers';

const ALLOWED_SCREENSHOT_TYPES = new Set(['image/png', 'image/jpeg', 'image/webp']);
const MAX_SCREENSHOT_SIZE_BYTES = 10 * 1024 * 1024;

@Component({
    selector: 'dot-report-issue',
    templateUrl: './dot-report-issue.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [
        ReactiveFormsModule,
        DialogModule,
        ButtonModule,
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
    private readonly location = inject<Location>(LOCATION_TOKEN);
    private readonly document = inject(DOCUMENT);
    private readonly screenshotUploadRef = viewChild<FileUpload>('screenshotUpload');

    readonly form: FormGroup = this.fb.group({
        description: ['', [Validators.required, this.trimmedRequiredValidator()]],
        screenshot: [null as File | null, [this.screenshotValidator()]]
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

    private isClosing = false;

    constructor() {
        effect(() => {
            if (this.visible()) {
                this.resetForm();
            }
        });
    }

    handleClose(): void {
        if (this.isClosing) {
            return;
        }

        this.isClosing = true;
        this.resetForm();
        this.visible.set(false);
        this.shutdown.emit();

        queueMicrotask(() => {
            this.isClosing = false;
        });
    }

    onScreenshotSelected(event: FileSelectEvent): void {
        const file = event.files?.[0] ?? null;

        this.screenshotFile.set(file);
        this.errorMessage.set('');
        this.form.get('screenshot')?.setValue(file);
        this.form.get('screenshot')?.markAsTouched();
        this.form.get('screenshot')?.updateValueAndValidity();
    }

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

    onScreenshotClear(): void {
        this.removeScreenshot();
    }

    save(): void {
        this.hasSubmitted.set(true);

        if (this.form.invalid) {
            this.form.markAllAsTouched();
            return;
        }

        this.errorMessage.set('');
        this.isSubmitting.set(true);
        const payload = this.buildPayload();

        this.dotReportIssueService
            .reportIssue(payload)
            .pipe(take(1))
            .subscribe({
                next: () => {
                    this.isSubmitting.set(false);
                    this.dotGlobalMessageService.success(
                        this.dotMessageService.get('report-an-issue.success')
                    );
                    this.handleClose();
                },
                error: (error) => {
                    this.isSubmitting.set(false);
                    this.errorMessage.set(this.getRequestErrorMessage(error));
                    this.dotHttpErrorManagerService.handle(error).pipe(take(1)).subscribe();
                }
            });
    }

    getScreenshotErrorMessage(): string | null {
        const screenshotControl = this.form.get('screenshot');
        const errors = screenshotControl?.errors;

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
    }

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
            screenshot: this.screenshotFile()
        };
    }

    private resetForm(): void {
        this.isSubmitting.set(false);
        this.hasSubmitted.set(false);
        this.errorMessage.set('');
        this.screenshotFile.set(null);
        this.form.reset({
            description: '',
            screenshot: null
        });
    }

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

    private trimmedRequiredValidator(): ValidatorFn {
        return (control: AbstractControl): ValidationErrors | null => {
            const value = control.value;

            if (typeof value !== 'string') {
                return null;
            }

            return value.trim().length ? null : { required: true };
        };
    }

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
