import { Injectable } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { DotWizardInput } from '@models/dot-wizard-input/dot-wizard-input.model';

@Injectable()
export class DotWizardService {
    private input: Subject<DotWizardInput> = new Subject<DotWizardInput>();
    private output: Subject<Record<string, unknown>> = new Subject<{ [key: string]: string }>();

    get showDialog$(): Observable<DotWizardInput> {
        return this.input.asObservable();
    }

    /**
     * Notify the data collected in wizard.
     * @param {{ [key: string]: string | string[] }} form
     * @memberof DotWizardService
     */
    output$(form: { [key: string]: string | string[] }): void {
        this.output.next(form);
    }

    /**
     * Send the wizard data to in input subscription and waits for the output
     * @param {DotWizardInput} data
     * @returns Observable<{ [key: string]: string }>
     * @memberof DotWizardService
     */
    open<T = { [key: string]: string }>(data: DotWizardInput): Observable<T> {
        this.input.next(data);

        return this.output.asObservable() as Observable<T>;
    }
}
