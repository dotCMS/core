import { TestBed, async, ComponentFixture } from '@angular/core/testing';
import { FieldValidationMessageComponent } from './field-validation-message';
import { By } from '@angular/platform-browser';
import { DebugElement } from '@angular/core';

describe('FieldValidationComponent', () => {
  let de: DebugElement;
  let el: HTMLElement;
  let fixture: ComponentFixture<FieldValidationMessageComponent>;
  let component: FieldValidationMessageComponent;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [
        FieldValidationMessageComponent,
      ]
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(FieldValidationMessageComponent);
    component = fixture.debugElement.componentInstance;
  });

  it('should hide the message by default', () => {
    let fakeForm: any = {};
    let fakeInput: any = {};
    component.form = fakeForm;
    component.field = fakeInput;
    fixture.detectChanges();
    de = fixture.debugElement.query(By.css('div'));
    expect(de).toBeNull();
  });

  it('should hide the message when field it\'s valid', () => {
    let fakeForm: any = {};
    let fakeInput: any = {};
    fakeInput.valid = true;
    component.form = fakeForm;
    component.field = fakeInput;
    fixture.detectChanges();
    de = fixture.debugElement.query(By.css('div'));
    expect(de).toBeNull();
  });

  it('should show the message when field it\'s touched and invalid', () => {
    let fakeForm: any = {};
    let fakeInput: any = {};
    fakeInput.touched = true;
    fakeInput.valid = false;
    component.form = fakeForm;
    component.field = fakeInput;
    component.message = 'Error message';
    fixture.detectChanges();
    de = fixture.debugElement.query(By.css('div'));
    el = de.nativeElement;
    expect(el).toBeDefined();
    expect(el.textContent).toBe('Error message');
  });

  it('should show the message when field it\'s required and form submitted', () => {
    let fakeForm: any = {};
    let fakeInput: any = {};
    fakeInput.pristine = true;
    fakeForm.submitted = true;
    component.form = fakeForm;
    component.field = fakeInput;
    component.message = 'Error message';
    fixture.detectChanges();
    de = fixture.debugElement.query(By.css('div'));
    el = de.nativeElement;
    expect(el).toBeDefined();
    expect(el.textContent).toBe('Error message');
  });

});
