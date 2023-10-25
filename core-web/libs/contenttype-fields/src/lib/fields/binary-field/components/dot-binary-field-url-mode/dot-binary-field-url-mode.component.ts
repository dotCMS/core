import { Subject } from 'rxjs';

import { CommonModule } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    EventEmitter,
    Input,
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

@Component({
    selector: 'dot-dot-binary-field-url-mode',
    standalone: true,
    imports: [
        CommonModule,
        FormsModule,
        ReactiveFormsModule,
        ButtonModule,
        InputTextModule,
        DotMessagePipe,
        DotFieldValidationMessageComponent
    ],
    providers: [DotBinaryFieldUrlModeStore],
    templateUrl: './dot-binary-field-url-mode.component.html',
    styleUrls: ['./dot-binary-field-url-mode.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotBinaryFieldUrlModeComponent implements OnInit, OnDestroy {
    @Input() maxFileSize: number;
    @Input() accept: string[];

    @Output() tempFileUploaded: EventEmitter<DotCMSTempFile> = new EventEmitter<DotCMSTempFile>();
    @Output() cancel: EventEmitter<void> = new EventEmitter<void>();

    private readonly store = inject(DotBinaryFieldUrlModeStore);

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

    ngOnInit(): void {
        this.store.setMaxFileSize(this.maxFileSize);
        this.tempFileChanged$
            .pipe(takeUntil(this.destroy$))
            .pipe(filter((tempFile) => tempFile !== null))
            .subscribe((tempFile) => {
                this.tempFileUploaded.emit(tempFile);
            });
    }

    ngOnDestroy(): void {
        this.destroy$.next();
        this.destroy$.complete();
        this.abortController?.abort(); // Abort fetch request if component is destroyed
    }

    onSubmit(): void {
        if (this.form.invalid) {
            return;
        }

        const url = this.form.get('url').value;
        this.abortController = new AbortController();

        this.store.uploadFileByUrl({ url, signal: this.abortController.signal });
        this.form.reset({ url }); // Reset form to initial state
    }

    cancelUpload(): void {
        this.abortController?.abort();
        this.cancel.emit();
    }

    handleFocus(): void {
        this.store.setError(''); // Clear server  error message when user focus on input
    }

    private toggleForm(isLoading: boolean): void {
        isLoading ? this.form.disable() : this.form.enable();
    }
}
