import { Component, computed, inject, input, output, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';

import { debounceTime, distinctUntilChanged, map, startWith } from 'rxjs/operators';

import { DotMessagePipe } from '@dotcms/ui';

@Component({
    selector: 'dot-demo',
    imports: [
        DotMessagePipe,
        InputTextModule,
        ReactiveFormsModule,
        DialogModule,
        IconFieldModule,
        InputIconModule,
        ButtonModule
    ],
    templateUrl: './dot-demo.component.html',
    styleUrls: ['./dot-demo.component.scss']
})
export class DotDemoComponent {
    readonly #title = input.required<string>({ alias: 'title' });
    readonly #description = input<string>('');

    readonly #demoText = signal<string>('');
    readonly $demoText = this.#demoText.asReadonly();

    readonly demoChange = output<string>();
    readonly dialogVisible = output<boolean>();

    readonly searchControl = new FormControl('', { nonNullable: true });

    readonly $searchTerm = toSignal(
        this.searchControl.valueChanges.pipe(
            startWith(this.searchControl.value),
            debounceTime(300),
            map((value) => value.trim().toLowerCase()),
            distinctUntilChanged()
        ),
        { initialValue: '' }
    );

    readonly $title = computed(() => {
        return `Demo: ${this.#title()}`;
    });

    protected onDemoTextChange(value: string): void {
        this.#demoText.set(value);
        this.demoChange.emit(value);
    }

    protected toggleDialog(): void {
        this.dialogVisible.emit(!this.dialogVisible());
    }

    protected resetSearch(): void {
        this.searchControl.setValue('');
    }
}