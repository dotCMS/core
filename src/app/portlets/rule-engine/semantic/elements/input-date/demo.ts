import {bootstrap} from '@angular/bootstrap';
import {Attribute, Component, View} from '@angular/core';
import {CORE_DIRECTIVES, FORM_DIRECTIVES} from '@angular/common';
import {InputDate} from './input-date';

export class InputDateModel {
  name: string;
  placeholder: string;
  value: string;
  disabled: boolean;
  icon: string;
  type: string;

  constructor(name: string = null,
              placeholder = '',
              type = 'date',
              value: string = null,
              disabled: boolean = null,
              icon = '') {

    this.name = !!name ? name : 'field-' + new Date().getTime() + Math.floor(Math.random() * 1000);
    this.placeholder = placeholder;
    this.type = type;
    this.value = value;
    this.disabled = disabled;
    this.icon = icon || '';
    if (this.icon.indexOf(' ') === -1 && this.icon.length > 0){
      this.icon = (this.icon + ' icon').trim();
    }
  }

  validateDate(date: string): void {
    let date_regex = /^(?:(?:31(\/|-|\.)(?:0?[13578]|1[02]))\1|(?:(?:29|30)(\/|-|\.)(?:0?[1,3-9]|1[0-2])\2))(?:(?:1[6-9]|[2-9]\d)?\d{2})$|^(?:29(\/|-|\.)0?2\3(?:(?:(?:1[6-9]|[2-9]\d)?(?:0[48]|[2468][048]|[13579][26])|(?:(?:16|[2468][048]|[3579][26])00))))$|^(?:0?[1-9]|1\d|2[0-8])(\/|-|\.)(?:(?:0?[1-9])|(?:1[0-2]))\4(?:(?:1[6-9]|[2-9]\d)?\d{2})$/;
    if (!date_regex.test(date)) {
      throw new Error('Insert a valid date dd/mm/yyyy,dd-mm-yyyy or dd.mm.yyyy');
    }
  }

  validateTime(time: string): void {
    let time_regex = /^(10|11|12|[1-9]):[0-5][0-9]$/;
    if (!time_regex.test(time)) {
      throw new Error('Insert a valid time HH:MM');
    }
  }

  validateDateTime(dateTime: string): void {
    // TODO: better match this regex for MM/DD/YYYY HH:MM
    let date_time_regex = /^(((\d\d)(([02468][048])|([13579][26]))-02-29)|(((\d\d)(\d\d)))-((((0\d)|(1[0-2]))-((0\d)|(1\d)|(2[0-8])))|((((0[13578])|(1[02]))-31)|(((0[1,3-9])|(1[0-2]))-(29|30)))))\s(([01]\d|2[0-3]):([0-5]\d):([0-5]\d))$/;
    if (!date_time_regex.test(dateTime)) {
      throw new Error('Insert a valid date time');
    }
  }

  validate(value: string): void {
    console.log(this.type);
    if (this.type === 'date') {
      this.validateDate(value);
    } else if (this.type === 'time') {
      this.validateTime(value);
    } else if (this.type === 'datetime-local') {
      this.validateDateTime(value);
    }
  };
}

@Component({
  selector: 'demo',
  template: `<div class="ui three column grid">
  <div class="column">
    <h4 class="ui top attached inverted header">Default</h4>
    <div class="ui attached segment">
      <cw-input-date></cw-input-date>
    </div>
  </div>
  <div class="column">
    <h4 class="ui top attached inverted header">datetime-local</h4>
    <div class="ui attached segment">
      <cw-input-date [model]="demoValue"></cw-input-date>
    </div>
  </div>
  <div class="column">
    <h4 class="ui top attached inverted header">Disabled</h4>
    <div class="ui attached segment">
      <cw-input-date [model]="demoDisabled"></cw-input-date>
    </div>
  </div>
  <div class="column">
    <h4 class="ui top attached inverted header">Required and bigger than 5</h4>
    <div class="ui attached segment">
      <cw-input-date [model]="demoError" (change)="customChange($event)"></cw-input-date>
    </div>
  </div>
  <div class="column">
    <h4 class="ui top attached inverted header">Icon</h4>
    <div class="ui attached segment">
      <cw-input-date [model]="demoIcon"></cw-input-date>
    </div>
  </div>
</div>
`
})
class App {
  demoValue: InputDateModel;
  demoDisabled: InputDateModel;
  demoError: InputDateModel;
  demoIcon: InputDateModel;

  constructor(@Attribute('id') id: string) {
    this.initDemoValue();
    this.initDemoDisabled();
    this.initDemoError();
    this.initDemoIcon();
  }

  initDemoValue(): void {
    let model = new InputDateModel();
    model.name = 'field-' + new Date().getTime() + Math.floor(Math.random() * 1000);
    model.value = 'Costa Rica';
    model.type = 'datetime-local';

    this.demoValue = model;
  }

  initDemoDisabled(): void {
    let model = new InputDateModel();
    model.name = 'field-' + new Date().getTime() + Math.floor(Math.random() * 1000);
    model.disabled = true;
    model.placeholder = 'Disabled';

    this.demoDisabled = model;
  }

  initDemoError(): void {
    let model = new InputDateModel();
    model.type = 'time';
    model.name = 'field-' + new Date().getTime() + Math.floor(Math.random() * 1000);
    model.value = 'Required Field';
    model.validate = (newValue: string) => {
      let biggerThanFive = /^([5-9]|0[5-9]|1[0-9]|2[0-3]):[0-5][0-9]$/;
      if (!newValue) {
        throw new Error('Required Field');
      } else if (!biggerThanFive.test(newValue)) {
        throw new Error('Time should be bigger than 5AM');
      }
    };

    this.demoError = model;
  }

  initDemoIcon(): void {
    let model = new InputDateModel();
    model.name = 'field-' + new Date().getTime() + Math.floor(Math.random() * 1000);
    model.icon = 'icon circular calendar link';
    model.placeholder = 'Icon';

    this.demoIcon = model;
  }

  customChange(event): void {
    console.log('Value of field: ' + event.target.value);
  }

}

export function main(): any {
  let app = bootstrap(App);
  app.then((appRef) => {
    console.log('Bootstrapped App: ', appRef);
  }).catch((e) => {
    console.log('Error bootstrapping app: ', e);
    throw e;
  });
  return app;
}
