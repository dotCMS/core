import { Observable, Subject } from 'rxjs';

import { Injectable } from '@angular/core';

import { DotWizardInput } from '@dotcms/dotcms-models';

type WizardOutput = { [key: string]: string | string[] };

@Injectable({
    providedIn: 'root'
})
export class DotWizardService {
    private input: Subject<DotWizardInput> = new Subject<DotWizardInput>();
    private currentOutput: Subject<WizardOutput> | null = null;

    get showDialog$(): Observable<DotWizardInput> {
        return this.input.asObservable();
    }

    /**
     * Emit the data collected by the wizard and close the current stream.
     * Called by the wizard component when the user accepts/sends.
     */
    output$(form: WizardOutput): void {
        this.currentOutput?.next(form);
        this.currentOutput?.complete();
        this.currentOutput = null;
    }

    /**
     * Close the current stream without emitting. Called by the wizard
     * component when the user cancels or dismisses the dialog so that
     * pending subscriptions unsubscribe instead of leaking into the next
     * wizard invocation.
     */
    cancel(): void {
        this.currentOutput?.complete();
        this.currentOutput = null;
    }

    /**
     * Show the wizard with the given input and return an observable that
     * emits once when the user submits and completes, or completes without
     * emitting if the user cancels. Each call returns an isolated stream.
     */
    open<T = { [key: string]: string }>(data: DotWizardInput): Observable<T> {
        this.currentOutput?.complete();
        this.currentOutput = new Subject<WizardOutput>();
        const output$ = this.currentOutput.asObservable() as unknown as Observable<T>;
        this.input.next(data);

        return output$;
    }
}
