import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule, NgForm } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';

import { SearchFormComponent } from './search-form.component';

describe('SearchFormComponent', () => {
  let component: SearchFormComponent;
  let fixture: ComponentFixture<SearchFormComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        SearchFormComponent,
        FormsModule,
        ButtonModule,
        InputTextModule,
        NoopAnimationsModule
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(SearchFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have a form with search field and submit button', () => {
    const form = fixture.debugElement.query(By.css('form'));
    const input = fixture.debugElement.query(By.css('input[name="searchQuery"]'));
    const button = fixture.debugElement.query(By.css('p-button[type="submit"]'));

    expect(form).toBeTruthy();
    expect(input).toBeTruthy();
    expect(button).toBeTruthy();
  });

  it('should emit search event with form value on submit', () => {
    const searchData = { searchQuery: 'test search' };
    const emitSpy = spyOn(component.search, 'emit');

    // Create a form object to pass to onSubmit
    const form = {
      value: searchData,
      valid: true
    } as NgForm;

    component.onSubmit(form);

    expect(emitSpy).toHaveBeenCalledWith(searchData);
  });

  it('should not emit search event if form is invalid', () => {
    const emitSpy = spyOn(component.search, 'emit');

    // Create an invalid form
    const form = {
      valid: false
    } as NgForm;

    component.onSubmit(form);

    expect(emitSpy).not.toHaveBeenCalled();
  });
});
