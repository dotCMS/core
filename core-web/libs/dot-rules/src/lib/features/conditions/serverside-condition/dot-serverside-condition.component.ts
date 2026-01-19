import { Observable, of, from } from 'rxjs';

import { AsyncPipe } from '@angular/common';
import { Component, ChangeDetectionStrategy, inject, input, output, effect } from '@angular/core';
import { ReactiveFormsModule, FormsModule, UntypedFormControl } from '@angular/forms';

import { DatePickerModule } from 'primeng/datepicker';
import { InputTextModule } from 'primeng/inputtext';
import { MultiSelectModule } from 'primeng/multiselect';
import { SelectModule } from 'primeng/select';

import { map, mergeMap, toArray, startWith, shareReplay } from 'rxjs/operators';

import { CoreWebService, LoggerService } from '@dotcms/dotcms-js';
import { isEmpty } from '@dotcms/utils';

import { ConditionModel, ParameterModel } from '../../../services/api/rule/Rule';
import { ServerSideFieldModel } from '../../../services/api/serverside-field/ServerSideFieldModel';
import { I18nService } from '../../../services/i18n/i18n.service';
import {
    ParameterDefinition,
    DropdownInputModel,
    RestDropdownInputModel
} from '../../../services/models/input.model';
import { Verify } from '../../../services/utils/verify.util';

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
    dateValue?: Date;
}

interface SpacerConfig {
    flex: number;
    type: string;
}

type InputOrSpacer = InputConfig | SpacerConfig;

interface ComparisonOption {
    rightHandArgCount?: number;
}

interface DropdownOption {
    i18nKey?: string;
    icon?: string;
    rightHandArgCount?: number;
    value?: string;
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
export class DotServersideConditionComponent {
    private readonly logger = inject(LoggerService);
    private readonly coreWebService = inject(CoreWebService);
    private readonly i18nService = inject(I18nService);

    // Inputs
    readonly $componentInstance = input.required<ServerSideFieldModel>({
        alias: 'componentInstance'
    });

    // Outputs
    readonly parameterValueChange = output<{ name: string; value: string }>();

    // State
    isLast = null;
    inputs: InputOrSpacer[] = [];
    rightHandArgCount: number | null = null;

    // Track current type to prevent unnecessary rebuilds
    private currentTypeKey: string | null = null;
    private cachedInstance: ServerSideFieldModel | null = null;

    private readonly errorMessages: Record<string, string> = {
        minLength: 'Input must be at least ${len} characters long.',
        noQuotes: 'Input cannot contain quote [" or \'] characters.',
        noDoubleQuotes: 'Input cannot contain quote ["] characters.',
        required: 'Required'
    };

    constructor() {
        effect(() => {
            const instance = this.$componentInstance();
            if (instance?.type?.parameters) {
                // Only rebuild inputs if the type has changed
                const newTypeKey = instance.type?.key;
                if (newTypeKey !== this.currentTypeKey) {
                    this.currentTypeKey = newTypeKey;
                    this.cachedInstance = instance;
                    this.buildInputsFromInstance(instance);
                }
            }
        });
    }

    private static getRightHandArgCount(
        selectedComparison: ComparisonOption | undefined
    ): number | null {
        if (!selectedComparison) {
            return null;
        }
        return Verify.isNumber(selectedComparison.rightHandArgCount)
            ? selectedComparison.rightHandArgCount
            : 1;
    }

    private static isComparisonParameter(input: InputOrSpacer): input is InputConfig {
        return input && 'name' in input && input.name === 'comparison';
    }

    private isConditionWithFewFields(
        inputCount: number,
        field: ServerSideFieldModel | undefined
    ): boolean {
        return inputCount <= 2 && field instanceof ConditionModel;
    }

    private buildInputsFromInstance(instance: ServerSideFieldModel): void {
        this.rightHandArgCount = null;
        const paramDefs = instance.type.parameters;

        this.inputs = [];
        Object.keys(paramDefs).forEach((key) => {
            const paramDef = instance.getParameterDef(key);
            const param = instance.getParameter(key);
            const input = this.createInputConfig(
                instance,
                paramDef.inputType.type,
                param,
                paramDef
            );
            if (input) {
                this.inputs[paramDef.priority] = input;
            }
        });

        // Remove empty (undefined) elements
        this.inputs = this.inputs.filter((i) => i);

        // Add spacer for conditions with few fields
        if (this.isConditionWithFewFields(this.inputs.length, instance)) {
            this.inputs = [{ flex: 40, type: 'spacer' }, ...this.inputs];
        }

        // Process comparison parameter and apply right-hand-side argument count
        let comparison: InputConfig | undefined;
        let comparisonIdx: number | null = null;

        this.inputs.forEach((input, idx) => {
            if (DotServersideConditionComponent.isComparisonParameter(input)) {
                comparison = input;
                this.applyRightHandSideCount(instance, comparison.value);
                comparisonIdx = idx;
            } else if (comparisonIdx !== null && 'argIndex' in input) {
                if (this.rightHandArgCount !== null) {
                    (input as InputConfig).argIndex = idx - comparisonIdx - 1;
                }
            }
        });

        if (comparison) {
            this.applyRightHandSideCount(instance, comparison.value);
        }
    }

    getErrorMessage(input: InputConfig): string {
        const control = input.control;
        let message = '';
        Object.keys(control.errors || {}).forEach((key) => {
            message += this.errorMessages[key] || '';
        });
        return message;
    }

    onBlur(input: InputConfig): void {
        if (input.control.dirty) {
            this.emitParameterValue(input.name, input.control.value, true);
        }
    }

    onDropdownChange(input: InputConfig, value: string): void {
        input.control.setValue(value);
        input.control.markAsDirty();
        this.emitParameterValue(input.name, value, true);
    }

    onDateChange(input: InputConfig, value: Date | null): void {
        if (!value || !(value instanceof Date) || isNaN(value.getTime())) {
            return;
        }
        const isoValue = this.convertToISOFormat(value);
        input.dateValue = value;
        input.value = isoValue;
        input.control.setValue(isoValue);
        input.control.markAsDirty();
        this.emitParameterValue(input.name, isoValue, true);
    }

    onRestDropdownChange(input: InputConfig, value: string | string[] | null): void {
        const finalValue = Array.isArray(value) ? value.join(',') : (value ?? '');
        input.control.setValue(finalValue);
        input.control.markAsDirty();
        input.modelValue = value ?? (input.maxSelections > 1 ? [] : '');

        // Don't emit empty values for multiselect fields to prevent API validation errors
        if (!finalValue && input.maxSelections > 1) {
            return;
        }

        this.emitParameterValue(input.name, finalValue, true);
    }

    private emitParameterValue(name: string, value: string, _isBlur = false): void {
        this.parameterValueChange.emit({ name, value });
        if (name === 'comparison' && this.cachedInstance) {
            this.applyRightHandSideCount(this.cachedInstance, value);
        }
    }

    private createInputConfig(
        instance: ServerSideFieldModel,
        type: string,
        param: ParameterModel,
        paramDef: ParameterDefinition
    ): InputConfig | null {
        if (!instance?.type?.i18nKey) {
            this.logger.warn(
                'DotServersideConditionComponent',
                'createInputConfig - no instance',
                type
            );
            return null;
        }

        const i18nBaseKey = paramDef.i18nBaseKey || instance.type.i18nKey;

        // Pre-load i18n resources
        this.i18nService.get(i18nBaseKey).subscribe({
            next: () => {
                // Pre-loading i18n resources
            }
        });

        let input: InputConfig | null = null;
        if (type === 'text' || type === 'number') {
            input = this.createTextInput(instance, param, paramDef, i18nBaseKey);
            this.logger.info(
                'DotServersideConditionComponent',
                'createInputConfig',
                type,
                paramDef
            );
        } else if (type === 'datetime') {
            input = this.createDateTimeInput(instance, param, paramDef, i18nBaseKey);
        } else if (type === 'restDropdown') {
            input = this.createRestDropdownInput(instance, param, paramDef, i18nBaseKey);
        } else if (type === 'dropdown') {
            input = this.createDropdownInput(instance, param, paramDef, i18nBaseKey);
        } else {
            this.logger.warn('DotServersideConditionComponent', 'Unknown input type:', type);
            return null;
        }

        if (input) {
            input.type = type;
        }
        return input;
    }

    private getDefaultDate(): Date {
        const date = new Date();
        date.setHours(0, 0, 0, 0);
        date.setMonth(date.getMonth() + 1);
        date.setDate(1);
        return date;
    }

    private convertToISOFormat(value: Date): string {
        const offset = new Date().getTimezoneOffset() * 60000;
        return new Date(value.getTime() - offset).toISOString().slice(0, -5);
    }

    private createTextInput(
        instance: ServerSideFieldModel,
        param: ParameterModel,
        paramDef: ParameterDefinition,
        i18nBaseKey: string
    ): InputConfig {
        const resourceKey = `${i18nBaseKey}.inputs.${paramDef.key}`;
        const placeholderKey = `${resourceKey}.placeholder`;
        const control = ServerSideFieldModel.createNgControl(instance, param.key);

        return {
            control,
            name: param.key,
            placeholder: this.i18nService.get(placeholderKey, paramDef.key),
            required: paramDef.inputType.dataType?.['minLength'] > 0,
            argIndex: null // Initialize to allow visibility control based on comparison
        };
    }

    private createDateTimeInput(
        instance: ServerSideFieldModel,
        param: ParameterModel,
        paramDef: ParameterDefinition,
        _i18nBaseKey: string
    ): InputConfig {
        const stringValue = instance.getParameterValue(param.key) || '';
        return {
            control: ServerSideFieldModel.createNgControl(instance, param.key),
            name: param.key,
            required: paramDef.inputType.dataType?.['minLength'] > 0,
            value: stringValue,
            visible: true,
            dateValue: this.parseStringToDate(stringValue),
            argIndex: null // Initialize to allow visibility control based on comparison
        };
    }

    private parseStringToDate(value: string): Date {
        if (!value || isEmpty(value)) {
            return this.getDefaultDate();
        }
        try {
            const date = new Date(value);
            if (isNaN(date.getTime())) {
                return this.getDefaultDate();
            }
            return date;
        } catch {
            return this.getDefaultDate();
        }
    }

    private createRestDropdownInput(
        instance: ServerSideFieldModel,
        param: ParameterModel,
        paramDef: ParameterDefinition,
        i18nBaseKey: string
    ): InputConfig {
        const inputType = paramDef.inputType as RestDropdownInputModel;
        const resourceKey = `${i18nBaseKey}.inputs.${paramDef.key}`;
        const placeholderKey = `${resourceKey}.placeholder`;

        let currentValue = instance.getParameterValue(param.key);
        if (
            currentValue &&
            (currentValue.indexOf('"') !== -1 || currentValue.indexOf("'") !== -1)
        ) {
            currentValue = currentValue.replace(/["']/g, '');
            instance.setParameter(param.key, currentValue);
        }

        const control = ServerSideFieldModel.createNgControl(instance, param.key);

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
            control,
            maxSelections: inputType.maxSelections,
            minSelections: inputType.minSelections,
            name: param.key,
            options$,
            placeholder: this.i18nService.get(placeholderKey, paramDef.key),
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
        if (Verify.isArray(res)) {
            return (res as Record<string, unknown>[]).map((item) =>
                this.jsonEntryToOption(item, null, optionValueField, optionLabelField)
            );
        }

        return Object.keys(res).map((key) =>
            this.jsonEntryToOption(
                res[key] as Record<string, unknown>,
                key,
                optionValueField,
                optionLabelField
            )
        );
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

    private createDropdownInput(
        instance: ServerSideFieldModel,
        param: ParameterModel,
        paramDef: ParameterDefinition,
        i18nBaseKey: string
    ): InputConfig {
        const inputType = paramDef.inputType as DropdownInputModel;
        const options = inputType.options;
        let resourceKey = `${i18nBaseKey}.inputs.${paramDef.key}`;
        const placeholderKey = `${resourceKey}.placeholder`;

        if (param.key === 'comparison') {
            resourceKey = 'api.sites.ruleengine.rules.inputs.comparison';
        } else {
            resourceKey = `${resourceKey}.options`;
        }

        const currentValue = instance.getParameterValue(param.key);
        let needsCustomAttribute = currentValue !== null;

        const opts: Array<{
            icon?: string;
            label: Observable<string> | string;
            rightHandArgCount?: number;
            value: string;
        }> = [];

        Object.keys(options).forEach((key: string) => {
            const option = options[key] as DropdownOption;
            if (needsCustomAttribute && key === currentValue) {
                needsCustomAttribute = false;
            }

            let labelKey = `${resourceKey}.${option.i18nKey}`;
            // hack for country
            if (param.key === 'country') {
                labelKey = `${i18nBaseKey}.${option.i18nKey}.name`;
            }

            opts.push({
                icon: option.icon,
                label: this.i18nService.get(labelKey, option.i18nKey),
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
            mergeMap((item) => {
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
            control: ServerSideFieldModel.createNgControl(instance, param.key),
            maxSelections: inputType.maxSelections,
            minSelections: inputType.minSelections,
            name: param.key,
            options$,
            placeholder: this.i18nService.get(placeholderKey, paramDef.key),
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

    private applyRightHandSideCount(
        instance: ServerSideFieldModel,
        selectedComparison: string
    ): void {
        if (!selectedComparison) {
            return;
        }
        const comparisonDef = instance.getParameterDef('comparison');
        if (!comparisonDef) {
            return;
        }
        const comparisonType = comparisonDef.inputType as DropdownInputModel;
        const selectedComparisonDef = comparisonType.options?.[selectedComparison];
        this.rightHandArgCount =
            DotServersideConditionComponent.getRightHandArgCount(selectedComparisonDef);
    }
}
