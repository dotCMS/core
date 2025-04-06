import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule, NgForm } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { MultiSelectModule } from 'primeng/multiselect';

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
        MultiSelectModule,
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

  it('should initialize content types in ngOnInit', () => {
    expect(component.contentTypes.length).toBe(5);
    expect(component.contentTypes[0].name).toBe('Content');
    expect(component.contentTypes[1].name).toBe('Pages');
    expect(component.contentTypes[2].name).toBe('Language Variables');
    expect(component.contentTypes[3].name).toBe('Widgets');
    expect(component.contentTypes[4].name).toBe('Files');
  });

  it('should have a form with search field, multiselect and submit button', () => {
    const form = fixture.debugElement.query(By.css('form'));
    const input = fixture.debugElement.query(By.css('input[name="searchQuery"]'));
    const multiselect = fixture.debugElement.query(By.css('p-multiSelect'));
    const button = fixture.debugElement.query(By.css('p-button[type="submit"]'));
    
    expect(form).toBeTruthy();
    expect(input).toBeTruthy();
    expect(multiselect).toBeTruthy();
    expect(button).toBeTruthy();
  });

  it('should emit search event with form value and selected types on submit', () => {
    const searchData = { searchQuery: 'test search' };
    const selectedTypes = [
      { name: 'Content', value: 1 },
      { name: 'Pages', value: 2 }
    ];
    component.selectedTypes = selectedTypes;
    
    const emitSpy = spyOn(component.search, 'emit');
    
    // Create a form object to pass to onSubmit
    const form = {
      value: searchData,
      valid: true
    } as NgForm;
    
    component.onSubmit(form);
    
    expect(emitSpy).toHaveBeenCalledWith({
      ...searchData,
      selectedTypes
    });
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
