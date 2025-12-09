import { Subject } from 'rxjs';

import { AsyncPipe } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    EventEmitter,
    OnDestroy,
    OnInit,
    Output,
    inject
} from '@angular/core';
import {
    FormGroup,
    FormControl,
    FormsModule,
    ReactiveFormsModule,
    Validators
} from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';

import { filter, takeUntil, tap } from 'rxjs/operators';

import { DotCMSTempFile } from '@dotcms/dotcms-models';
import { DotFieldValidationMessageComponent, DotMessagePipe } from '@dotcms/ui';

import { DotBinaryFieldUrlModeStore } from './store/dot-binary-field-url-mode.store';

import { DotBinaryFieldValidatorService } from '../../service/dot-binary-field-validator/dot-binary-field-validator.service';

@Component({
    selector: 'dot-binary-field-url-mode',
    imports: [
        FormsModule,
        ReactiveFormsModule,
        ButtonModule,
        InputTextModule,
        DotMessagePipe,
        DotFieldValidationMessageComponent,
        AsyncPipe
    ],
    providers: [DotBinaryFieldUrlModeStore],
    templateUrl: './dot-binary-field-url-mode.component.html',
    styleUrls: ['./dot-binary-field-url-mode.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotBinaryFieldUrlModeComponent implements OnInit, OnDestroy {
    @Output() tempFileUploaded: EventEmitter<DotCMSTempFile> = new EventEmitter<DotCMSTempFile>();
    @Output() cancel: EventEmitter<void> = new EventEmitter<void>();

    private readonly store = inject(DotBinaryFieldUrlModeStore);
    private readonly dotBinaryFieldValidatorService = inject(DotBinaryFieldValidatorService);

    // Form
    private readonly validators = [
        Validators.required,
        Validators.pattern(/^(ftp|http|https):\/\/[^ "]+$/)
    ];
    readonly form = new FormGroup({
        url: new FormControl('', this.validators)
    });

    // Observables
    readonly vm$ = this.store.vm$.pipe(tap(({ isLoading }) => this.toggleForm(isLoading)));
    readonly tempFileChanged$ = this.store.tempFile$;

    private readonly destroy$ = new Subject<void>();
    private abortController: AbortController;

    get acceptTypes(): string {
        return this.dotBinaryFieldValidatorService.accept.join(',');
    }

    ngOnInit(): void {
        this.tempFileChanged$
            .pipe(
                takeUntil(this.destroy$),
                filter((tempFile) => tempFile !== null)
            )
            .subscribe((tempFile) => {
                this.tempFileUploaded.emit(tempFile);
            });
    }

    ngOnDestroy(): void {
        this.destroy$.next();
        this.destroy$.complete();
        this.abortController?.abort(); // Abort fetch request if component is destroyed
    }

    /**
     * Submit form
     *
     * @return {*}  {void}
     * @memberof DotBinaryFieldUrlModeComponent
     */
    onSubmit(): void {
        if (this.form.invalid) {
            return;
        }

        const url = this.form.get('url').value;
        this.abortController = new AbortController();

        this.store.uploadFileByUrl({ url, signal: this.abortController.signal });
        this.form.reset({ url }); // Reset form to initial state
    }

    /**
     * Cancel upload
     *
     * @memberof DotBinaryFieldUrlModeComponent
     */
    cancelUpload(): void {
        this.abortController?.abort();
        // TODO: The 'emit' function requires a mandatory void argument
        this.cancel.emit();
    }

    /**
     * Handle focus event and clear server error message
     *
     * @memberof DotBinaryFieldUrlModeComponent
     */
    handleFocus(): void {
        this.store.setError(''); // Clear server  error message when user focus on input
    }

    private toggleForm(isLoading: boolean): void {
        isLoading ? this.form.disable() : this.form.enable();
    }
}
