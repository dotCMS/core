
import {interval as observableInterval} from 'rxjs';

import {takeWhile} from 'rxjs/operators';
import { ElementRef, Component, Directive, EventEmitter, Optional } from '@angular/core';
import {
  Host,
  AfterViewInit,
  OnDestroy,
  Output,
  Input,
  ChangeDetectionStrategy
} from '@angular/core';
import { ControlValueAccessor, NgControl } from '@angular/forms';
import { BehaviorSubject, Observable } from 'rxjs';
import * as _ from 'lodash';
import { LoggerService } from 'dotcms-js/dotcms-js';

/**
 * Angular wrapper around OLD Semantic UI Dropdown Module.
 *
 */
@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'cw-input-dropdown',
  template: `
    <p-dropdown *ngIf="maxSelections <= 1" 
                [options]="options | async" 
                ng-valid 
                appendTo="body" 
                [style]="{'width':'100%'}" 
                class="ui fluid ng-valid"
                [placeholder]="placeholder"
                [(ngModel)]="modelValue"
                (onChange)="onChangeSemantic($event.value)"
                [required]="minSelections > 0"
                [editable]="allowAdditions">
    </p-dropdown>
    <dot-autocomplete-tags  [inputId]="name" *ngIf="maxSelections > 1" 
                [value]="modelValue" 
                [options]="options | async" 
                [placeholder]="placeholder" 
                (onChange)="fireMultiSelectChanges($event)"> 
    </dot-autocomplete-tags>`
})
export class Dropdown implements ControlValueAccessor {
  @Input() name: string;
  @Input() placeholder: string;
  @Input() allowAdditions: boolean;
  @Input() minSelections: number;
  @Input() maxSelections: number;

  @Output() onDropDownChange: EventEmitter<any> = new EventEmitter();
  @Output() touch: EventEmitter<any> = new EventEmitter();
  @Output() enter: EventEmitter<boolean> = new EventEmitter(false);

  modelValue: string;

  private _optionsAry: InputOption[] = [];
  private _options: BehaviorSubject<InputOption[]>;
  private elementRef: ElementRef;
  private _$dropdown: any;

  onChange: Function = () => {};
  onTouched: Function = () => {};

  constructor(
    elementRef: ElementRef,
    @Optional() control: NgControl,
    private loggerService: LoggerService
  ) {
    if (control && !control.valueAccessor) {
      control.valueAccessor = this;
    }
    this.placeholder = '';
    this.allowAdditions = false;
    this.minSelections = 0;
    this.maxSelections = 1;
    this._options = new BehaviorSubject(this._optionsAry);
    this.elementRef = elementRef;
  }

  @Input()
  set value(value: string) {
    this.modelValue = value;
  }

  get options() {
    return this._options;
  }

  focus(): void {
    try {
      this._$dropdown.children('input.search')[0].focus();
    } catch (e) {
      this.loggerService.info('Dropdown', 'could not focus search element');
    }
  }

  writeValue(value: any): void {
    this.modelValue = _.isEmpty(value) ? '' : value;
    this.applyValue(this.modelValue);
  }

  registerOnChange(fn): void {
    this.onChange = fn;
  }

  registerOnTouched(fn): void {
    this.onTouched = fn;
  }

  fireChange($event): void {
    if (this.onDropDownChange) {
      this.onDropDownChange.emit($event);
      this.onChange($event);
    }
  }

  fireMultiSelectChanges(event: any): void {
    this.onChangeSemantic(event);
    this.fireTouch(event);
  }

  completeMethod($event): void {
    console.log('completeMethod');
  }

  fireTouch($event): void {
    this.touch.emit($event);
    this.onTouched();
  }

  hasOption(option: InputOption): boolean {
    const x = this._optionsAry.filter(opt => {
      return option.value === opt.value;
    });
    return x.length !== 0;
  }

  addOption(option: InputOption): void {
    this._optionsAry = this._optionsAry.concat(option);
    this._options.next(this._optionsAry);
    if (option.value === this.modelValue) {
      this.refreshDisplayText(option.label);
    }
  }

  updateOption(option: InputOption): void {
    this._optionsAry = this._optionsAry.filter(opt => {
      return opt.value !== option.value;
    });
    this.addOption(option);
  }

  refreshDisplayText(label: string): void {
    if (this._$dropdown) {
      this._$dropdown.dropdown('set text', label);
    }
  }

  /**
   * Is called after a dropdown value changes. Receives the name and value of selection and the active menu element
   * @param value
   */
  onChangeSemantic(value): void {
    this.modelValue = value;
    this.fireChange(value);
  }

  private applyValue(value): void {
    let count = 0;

    observableInterval(10).pipe(
      takeWhile(() => {
        count++;
        if (count > 100) {
          throw new Error('Dropdown element not found.');
        }
        return this._$dropdown == null;
      }))
      .subscribe(
        () => {
          // still null!
        },
        e => {
          this.loggerService.info('Dropdown', 'Error', e);
        },
        () => {
          this.loggerService.info('Dropdown', 'onComplete');
          this._$dropdown.dropdown('set selected', value);
        }
      );
  }
}

@Directive({
  selector: 'cw-input-option'
})
export class InputOption {
  @Input() value: string;
  @Input() label: string;
  @Input() icon: string;
  private _dropdown: Dropdown;
  private _isRegistered: boolean;

  constructor(@Host() dropdown: Dropdown) {
    this._dropdown = dropdown;
  }

  ngOnChanges(change): void {
    if (!this._isRegistered) {
      if (this._dropdown.hasOption(this)) {
        this._dropdown.updateOption(this);
      } else {
        this._dropdown.addOption(this);
      }
      this._isRegistered = true;
    } else {
      if (!this.label) {
        this.label = this.value;
      }
      this._dropdown.updateOption(this);
    }
  }
}
