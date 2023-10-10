import { Subject } from 'rxjs';

import { CommonModule } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    EventEmitter,
    Input,
    OnDestroy,
    OnInit,
    Output
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

import { takeUntil, tap } from 'rxjs/operators';

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

    private readonly destroy$ = new Subject<void>();
    private readonly validators = [
        Validators.required,
        Validators.pattern(/^(ftp|http|https):\/\/[^ "]+$/)
    ];

    readonly invalidError = 'dot.binary.field.action.import.from.url.error.message';
    readonly vm$ = this.store.vm$.pipe(tap(({ isLoading }) => this.toggleForm(isLoading)));
    readonly tempFileChanged$ = this.store.tempFile$;
    readonly form = new FormGroup({
        url: new FormControl('', this.validators)
    });

    private abortController: AbortController;

    constructor(private readonly store: DotBinaryFieldUrlModeStore) {
        this.tempFileChanged$.pipe(takeUntil(this.destroy$)).subscribe((tempFile) => {
            this.tempFileUploaded.emit(tempFile);
        });
    }

    ngOnInit(): void {
        this.store.setMaxFileSize(this.maxFileSize);
    }

    ngOnDestroy(): void {
        this.destroy$.next();
        this.destroy$.complete();
        this.abortController?.abort(); // Abort fetch request if component is destroyed
    }

    onSubmit(): void {
        const control = this.form.get('url');

        if (this.form.invalid) {
            return;
        }

        const url = control.value;
        this.abortController = new AbortController();

        this.store.uploadFileByUrl({ url, signal: this.abortController.signal });
        this.form.reset({ url }); // Reset form to initial state
    }

    cancelUpload(): void {
        this.abortController?.abort();
        this.cancel.emit();
    }

    resetError(isError: boolean): void {
        if (isError) {
            this.store.setError('');
        }
    }

    private toggleForm(isLoading: boolean): void {
        isLoading ? this.form.disable() : this.form.enable();
    }
}
