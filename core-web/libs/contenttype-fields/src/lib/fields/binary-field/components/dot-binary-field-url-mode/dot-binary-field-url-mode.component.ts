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

import { distinctUntilChanged, filter, takeUntil } from 'rxjs/operators';

import { DotCMSTempFile } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DotBinaryFieldUrlModeStore } from './store/store/dot-binary-field-url-mode.store';

@Component({
    selector: 'dot-dot-binary-field-url-mode',
    standalone: true,
    imports: [
        CommonModule,
        FormsModule,
        ReactiveFormsModule,
        ButtonModule,
        InputTextModule,
        DotMessagePipe
    ],
    providers: [DotBinaryFieldUrlModeStore],
    templateUrl: './dot-binary-field-url-mode.component.html',
    styleUrls: ['./dot-binary-field-url-mode.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotBinaryFieldUrlModeComponent implements OnInit, OnDestroy {
    @Input() maxFileSize: number;
    @Input() accept: string[];

    @Output() tempFile: EventEmitter<DotCMSTempFile> = new EventEmitter<DotCMSTempFile>();
    @Output() cancel: EventEmitter<void> = new EventEmitter<void>();

    private readonly destroy$ = new Subject<void>();
    private readonly urlError = 'dot.binary.field.action.import.from.url.error.message';
    private readonly validators = [
        Validators.required,
        Validators.pattern(/^(ftp|http|https):\/\/[^ "]+$/)
    ];

    readonly vm$ = this.store.vm$;
    readonly form = new FormGroup({
        url: new FormControl('', this.validators)
    });

    private abortController: AbortController;

    constructor(private readonly store: DotBinaryFieldUrlModeStore) {
        this.store.tempFile$.pipe(takeUntil(this.destroy$)).subscribe((tempFile) => {
            this.tempFile.emit(tempFile);
            this.form.enable();
        });

        this.store.isLoading$.pipe(takeUntil(this.destroy$)).subscribe((isLoading) => {
            isLoading ? this.form.disable() : this.form.enable();
        });
    }

    ngOnInit(): void {
        this.store.setMaxFileSize(this.maxFileSize);

        this.form.statusChanges
            .pipe(
                filter((status) => status !== 'DISABLED'), // Avoid mutating last value
                distinctUntilChanged(), // Only emit when status really change.
                takeUntil(this.destroy$)
            )
            .subscribe((status) => {
                const error = status === 'INVALID' ? this.urlError : '';
                this.store.setError(error);
            });
    }

    ngOnDestroy(): void {
        this.destroy$.next();
        this.destroy$.complete();
        this.abortController?.abort(); // Abort fetch request if component is destroyed
    }

    onSubmit(): void {
        const control = this.form.get('url');

        if (this.form.invalid) {
            this.store.setError(this.urlError);

            return;
        }

        const url = control.value;
        this.abortController = new AbortController();

        this.store.uploadFileByUrl({ url, signal: this.abortController.signal });
        this.form.reset({ url }); // Reset touch and dirty state
    }

    cancelUpload(): void {
        this.abortController?.abort();
        this.cancel.emit();
    }
}
