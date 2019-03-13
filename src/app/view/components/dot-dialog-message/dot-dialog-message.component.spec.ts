import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { DotDialogMessageComponent } from './dot-dialog-message.component';

describe('DotDialogMessageComponent', () => {
  let component: DotDialogMessageComponent;
  let fixture: ComponentFixture<DotDialogMessageComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ DotDialogMessageComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(DotDialogMessageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
