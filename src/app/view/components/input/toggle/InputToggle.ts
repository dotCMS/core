import {Component, EventEmitter, Input, Output} from '@angular/core';
import {LoggerService} from '../../../../api/services/logger.service';

@Component({
  selector: 'cw-toggle-input',
  template: `<style>
  .ui.toggle.checkbox label {
    float: left
  }

  .on-label, .off-label {
    position: absolute;
    top: 0;
    padding-top: .3em;
    font-weight: 900;
    font-size: 75%;
    z-index: 2;
  }

  .on-label {
    left: .75em;
    color: white;
  }

  .off-label {
    right: .75em;
    color:#555;
  }

  .off .on-label, .on .off-label {
    display: none;
  }

</style>
  <span class="ui toggle fitted checkbox" [class.on]="value === true" [class.off]="value === false">
    <input type="checkbox" [value]="value" [checked]="value" (change)="updateValue($event)" [disabled]="disabled">
    <label></label>
    <span class="on-label">{{onText}}</span>
    <span class="off-label">{{offText}}</span>
  </span>
  `
})

export class InputToggle {
  @Input() value: boolean = false;
  @Input() disabled: boolean = false;
  @Input() onText: string = 'On';
  @Input() offText: string = 'Off';

  @Output() change: EventEmitter<boolean> = new EventEmitter();

  constructor(private loggerService: LoggerService) {}

  ngOnChanges(change): void {
    if (change.value) {
      this.value = change.value.currentValue === true;
    }
  }

  updateValue($event): void {
    $event.stopPropagation(); // grrr.
    let value = $event.target.checked;
    this.loggerService.debug('InputToggle', 'updateValue', 'input value changed: [from / to]', this.value, value);
    this.value = value;
    this.change.emit(value);
  }
}