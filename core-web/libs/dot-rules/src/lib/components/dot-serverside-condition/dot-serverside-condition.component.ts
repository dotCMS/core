import { Observable, of, from } from 'rxjs';

import { AsyncPipe } from '@angular/common';
import {
    Component,
    Input,
    Output,
    EventEmitter,
    ChangeDetectionStrategy,
    inject,
    OnChanges,
    SimpleChanges
} from '@angular/core';
import { ReactiveFormsModule, FormsModule, UntypedFormControl } from '@angular/forms';

import { DatePickerModule } from 'primeng/datepicker';
import { InputTextModule } from 'primeng/inputtext';
import { MultiSelectModule } from 'primeng/multiselect';
import { SelectModule } from 'primeng/select';

import { map, mergeMap, toArray, startWith, shareReplay } from 'rxjs/operators';

import { CoreWebService, LoggerService } from '@dotcms/dotcms-js';
import { isEmpty } from '@dotcms/utils';

import { ConditionModel, ParameterModel } from '../../services/Rule';
import { ServerSideFieldModel } from '../../services/ServerSideFieldModel';
import { I18nService } from '../../services/system/locale/I18n';
import {
    ParameterDefinition,
    CwDropdownInputModel,
    CwRestDropdownInputModel
} from '../../services/util/CwInputModel';
import { Verify } from '../../services/validation/Verify';

interface InputConfig {
    control: UntypedFormControl;
    name: string;
    placeholder?: Observable<string>;
    required?: boolean;
    value?: string;
    type?: string;
    flex?: number;
    argIndex?: number;
    options$?: Observable<{ label: string; value: string }[]>;
    allowAdditions?: boolean;
    maxSelections?: number;
    minSelections?: number;
    modelValue?: string | string[];
    visible?: boolean;
}

interface ComparisonOption {
    rightHandArgCount?: number;
}

@Component({
    selector: 'dot-serverside-condition',
    templateUrl: './dot-serverside-condition.component.html',
    imports: [
        AsyncPipe,
        ReactiveFormsModule,
        FormsModule,
        DatePickerModule,
        InputTextModule,
        SelectModule,
        MultiSelectModule
    ],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotServersideConditionComponent implements OnChanges {
    private loggerService = inject(LoggerService);
    private coreWebService = inject(CoreWebService);

    @Input() componentInstance: ServerSideFieldModel;
    @Output()
    parameterValueChange: EventEmitter<{ name: string; value: string }> = new EventEmitter(false);
    islast = null;

    _inputs: Array<InputConfig | { flex: number; type: string }>;
    _rhArgCount: number | null;
    private _resources: I18nService;

    private _errorMessageFormatters = {
        minLength: 'Input must be at least ${len} characters long.',
        noQuotes: 'Input cannot contain quote [" or \'] characters.',
        noDoubleQuotes: 'Input cannot contain quote ["] characters.',
        required: 'Required'
    };

    constructor() {
        const resources = inject(I18nService);

        this._resources = resources;
        this._inputs = [];
    }

    private static getRightHandArgCount(
        selectedComparison: ComparisonOption | undefined
    ): number | null {
        let argCount: number | null = null;
        if (selectedComparison) {
            argCount = Verify.isNumber(selectedComparison.rightHandArgCount)
                ? selectedComparison.rightHandArgCount
                : 1;
        }

        return argCount;
    }

    private static isComparisonParameter(
        input: InputConfig | { flex: number; type: string }
    ): boolean {
        return input && 'name' in input && input.name === 'comparison';
    }

    private isConditionalFieldWithLessThanThreeFields(
        size: number,
        field: ServerSideFieldModel | undefined
    ): boolean {
        return size <= 2 && field instanceof ConditionModel;
    }

    ngOnChanges(changes: SimpleChanges): void {
        let paramDefs = null;
        if (changes['componentInstance']) {
            this._rhArgCount = null;
            paramDefs = this.componentInstance.type.parameters;
        }

        if (paramDefs) {
            this._inputs = [];
            Object.keys(paramDefs).forEach((key) => {
                const paramDef = this.componentInstance.getParameterDef(key);
                const param = this.componentInstance.getParameter(key);

                const input = this.getInputFor(paramDef.inputType.type, param, paramDef);
                this._inputs[paramDef.priority] = input;
            });

            // Cleans _inputs array from empty(undefined) elements
            this._inputs = this._inputs.filter((i) => i);

            if (
                this.isConditionalFieldWithLessThanThreeFields(
                    this._inputs.length,
                    changes['componentInstance'].currentValue
                )
            ) {
                this._inputs = [{ flex: 40, type: 'spacer' }, ...this._inputs];
            }

            let comparison: InputConfig | undefined;
            let comparisonIdx: number | null = null;
            this._inputs.forEach((input, idx) => {
                if (DotServersideConditionComponent.isComparisonParameter(input)) {
                    comparison = input as InputConfig;
                    this.applyRhsCount(comparison.value);
                    comparisonIdx = idx;
                } else if (comparisonIdx !== null && 'argIndex' in input) {
                    if (this._rhArgCount !== null) {
                        (input as InputConfig).argIndex = idx - comparisonIdx - 1;
                    }
                }
            });
            if (comparison) {
                this.applyRhsCount(comparison.value);
            }
        }
    }

    /**
     * Brute force error messages from lookup table for now.
     */
    getErrorMessage(input): string {
        const control = input.control;
        let message = '';
        Object.keys(control.errors || {}).forEach((key) => {
            const err = control.errors[key];
            message += this._errorMessageFormatters[key];
            if (Object.keys(err).length) {
                // placeholder
            }
        });

        return message;
    }

    onBlur(input): void {
        if (input.control.dirty) {
            this.setParameterValue(input.name, input.control.value, input.control.valid, true);
        }
    }

    onDropdownChange(input: InputConfig, value: string): void {
        input.control.setValue(value);
        input.control.markAsDirty();
        this.setParameterValue(input.name, value, input.control.valid, true);
    }

    onDateChange(input: InputConfig, value: Date): void {
        const isoValue = this.convertToISOFormat(value);
        input.control.setValue(isoValue);
        input.control.markAsDirty();
        this.setParameterValue(input.name, isoValue, input.control.valid, true);
    }

    onRestDropdownChange(input: InputConfig, value: string | string[]): void {
        const finalValue = Array.isArray(value) ? value.join(',') : value;
        input.control.setValue(finalValue);
        input.control.markAsDirty();
        this.setParameterValue(input.name, finalValue, input.control.valid, true);
    }

    setParameterValue(name: string, value: string, _valid: boolean, _isBlur = false): void {
        this.parameterValueChange.emit({ name, value });
        if (name === 'comparison') {
            this.applyRhsCount(value);
        }
    }

    getInputFor(type: string, param: ParameterModel, paramDef: ParameterDefinition): InputConfig {
        const i18nBaseKey = paramDef.i18nBaseKey || this.componentInstance.type.i18nKey;
        /* Save a potentially large number of requests by loading parent key: */
        this._resources.get(i18nBaseKey).subscribe({
            next: () => {
                // Pre-loading i18n resources
            }
        });

        let input: InputConfig;
        if (type === 'text' || type === 'number') {
            input = this.getTextInput(param, paramDef, i18nBaseKey);
            this.loggerService.info(
                'DotServersideConditionComponent',
                'getInputFor',
                type,
                paramDef
            );
        } else if (type === 'datetime') {
            input = this.getDateTimeInput(param, paramDef, i18nBaseKey);
        } else if (type === 'restDropdown') {
            input = this.getRestDropdownInput(param, paramDef, i18nBaseKey);
        } else if (type === 'dropdown') {
            input = this.getDropdownInput(param, paramDef, i18nBaseKey);
        }

        input.type = type;

        return input;
    }

    getDateModelValue(input: InputConfig): Date {
        const value = input.value;
        if (isEmpty(value)) {
            return this.getDefaultDate();
        }

        return new Date(value);
    }

    private getDefaultDate(): Date {
        const d = new Date();
        d.setHours(0, 0, 0, 0);
        d.setMonth(d.getMonth() + 1);
        d.setDate(1);

        return d;
    }

    private convertToISOFormat(value: Date): string {
        const offset = new Date().getTimezoneOffset() * 60000;

        return new Date(value.getTime() - offset).toISOString().slice(0, -5);
    }

    private getTextInput(
        param: ParameterModel,
        paramDef: ParameterDefinition,
        i18nBaseKey: string
    ): InputConfig {
        const rsrcKey = i18nBaseKey + '.inputs.' + paramDef.key;
        const placeholderKey = rsrcKey + '.placeholder';
        const control = ServerSideFieldModel.createNgControl(this.componentInstance, param.key);

        return {
            control: control,
            name: param.key,
            placeholder: this._resources.get(placeholderKey, paramDef.key),
            required: paramDef.inputType.dataType['minLength'] > 0
        };
    }

    private getDateTimeInput(
        param: ParameterModel,
        paramDef: ParameterDefinition,
        _i18nBaseKey: string
    ): InputConfig {
        return {
            control: ServerSideFieldModel.createNgControl(this.componentInstance, param.key),
            name: param.key,
            required: paramDef.inputType.dataType['minLength'] > 0,
            value: this.componentInstance.getParameterValue(param.key),
            visible: true
        };
    }

    private getRestDropdownInput(
        param: ParameterModel,
        paramDef: ParameterDefinition,
        i18nBaseKey: string
    ): InputConfig {
        const inputType: CwRestDropdownInputModel = <CwRestDropdownInputModel>paramDef.inputType;
        const rsrcKey = i18nBaseKey + '.inputs.' + paramDef.key;
        const placeholderKey = rsrcKey + '.placeholder';

        let currentValue = this.componentInstance.getParameterValue(param.key);
        if (
            currentValue &&
            (currentValue.indexOf('"') !== -1 || currentValue.indexOf("'") !== -1)
        ) {
            currentValue = currentValue.replace(/["']/g, '');
            this.componentInstance.setParameter(param.key, currentValue);
        }

        const control = ServerSideFieldModel.createNgControl(this.componentInstance, param.key);

        // Fetch options from REST endpoint
        const options$ = this.coreWebService.request({ url: inputType.optionUrl }).pipe(
            map((res: Record<string, unknown> | unknown[]) =>
                this.jsonEntriesToOptions(
                    res,
                    inputType.optionValueField || 'key',
                    inputType.optionLabelField || 'value'
                )
            ),
            startWith([]),
            shareReplay(1)
        );

        const input: InputConfig = {
            allowAdditions: inputType.allowAdditions,
            control: control,
            maxSelections: inputType.maxSelections,
            minSelections: inputType.minSelections,
            name: param.key,
            options$: options$,
            placeholder: this._resources.get(placeholderKey, paramDef.key),
            required: inputType.minSelections > 0,
            value: currentValue,
            modelValue:
                inputType.maxSelections > 1 && currentValue ? currentValue.split(',') : currentValue
        };

        if (!input.value) {
            const selected = inputType.selected;
            input.value =
                selected !== null ? (Array.isArray(selected) ? selected.join(',') : selected) : '';
        }

        return input;
    }

    private jsonEntriesToOptions(
        res: Record<string, unknown> | unknown[],
        optionValueField: string,
        optionLabelField: string
    ): { value: string; label: string }[] {
        const valuesJson = res;
        let ary: { value: string; label: string }[] = [];

        if (Verify.isArray(valuesJson)) {
            ary = (valuesJson as Record<string, unknown>[]).map((valueJson) =>
                this.jsonEntryToOption(valueJson, null, optionValueField, optionLabelField)
            );
        } else {
            ary = Object.keys(valuesJson).map((key) => {
                return this.jsonEntryToOption(
                    valuesJson[key] as Record<string, unknown>,
                    key,
                    optionValueField,
                    optionLabelField
                );
            });
        }

        return ary;
    }

    private jsonEntryToOption(
        json: Record<string, unknown>,
        key: string | null = null,
        optionValueField: string,
        optionLabelField: string
    ): { value: string; label: string } {
        const opt: { value: string; label: string } = { value: '', label: '' };

        if (!json[optionValueField] && optionValueField === 'key' && key != null) {
            opt.value = key;
        } else {
            opt.value = json[optionValueField] as string;
        }

        opt.label = json[optionLabelField] as string;

        return opt;
    }

    private getDropdownInput(
        param: ParameterModel,
        paramDef: ParameterDefinition,
        i18nBaseKey: string
    ): InputConfig {
        const inputType: CwDropdownInputModel = <CwDropdownInputModel>paramDef.inputType;
        const opts = [];
        const options = inputType.options;
        let rsrcKey = i18nBaseKey + '.inputs.' + paramDef.key;
        const placeholderKey = rsrcKey + '.placeholder';
        if (param.key === 'comparison') {
            rsrcKey = 'api.sites.ruleengine.rules.inputs.comparison';
        } else {
            rsrcKey = rsrcKey + '.options';
        }

        const currentValue = this.componentInstance.getParameterValue(param.key);
        let needsCustomAttribute = currentValue !== null;

        Object.keys(options).forEach((key: string) => {
            const option = options[key];
            if (needsCustomAttribute && key === currentValue) {
                needsCustomAttribute = false;
            }

            let labelKey = rsrcKey + '.' + option.i18nKey;
            // hack for country
            if (param.key === 'country') {
                labelKey = i18nBaseKey + '.' + option.i18nKey + '.name';
            }

            opts.push({
                icon: option.icon,
                label: this._resources.get(labelKey, option.i18nKey),
                rightHandArgCount: option.rightHandArgCount,
                value: key
            });
        });

        if (needsCustomAttribute) {
            opts.push({
                label: of(currentValue),
                value: currentValue
            });
        }

        // Convert options with Observable labels to resolved options
        const options$ = from(opts).pipe(
            mergeMap((item: { label: Observable<string> | string; value: string }) => {
                if (item.label && (item.label as Observable<string>).pipe) {
                    return (item.label as Observable<string>).pipe(
                        map((text: string) => ({
                            label: text,
                            value: item.value
                        }))
                    );
                }

                return of({
                    label: item.label as string,
                    value: item.value
                });
            }),
            toArray(),
            startWith([]),
            shareReplay(1)
        );

        const input: InputConfig = {
            allowAdditions: inputType.allowAdditions,
            control: ServerSideFieldModel.createNgControl(this.componentInstance, param.key),
            maxSelections: inputType.maxSelections,
            minSelections: inputType.minSelections,
            name: param.key,
            options$: options$,
            placeholder: this._resources.get(placeholderKey, paramDef.key),
            required: inputType.minSelections > 0,
            value: currentValue
        };

        if (!input.value) {
            const selected = inputType.selected;
            input.value =
                selected !== null ? (Array.isArray(selected) ? selected.join(',') : selected) : '';
        }

        return input;
    }

    private applyRhsCount(selectedComparison: string): void {
        const comparisonDef = this.componentInstance.getParameterDef('comparison');
        const comparisonType: CwDropdownInputModel = <CwDropdownInputModel>comparisonDef.inputType;
        const selectedComparisonDef = comparisonType.options[selectedComparison];
        this._rhArgCount =
            DotServersideConditionComponent.getRightHandArgCount(selectedComparisonDef);
    }
}
