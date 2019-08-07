import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { DotEditPageStateControllerComponent } from './dot-edit-page-state-controller.component';

describe('DotEditPageStateControllerComponent', () => {
  let component: DotEditPageStateControllerComponent;
  let fixture: ComponentFixture<DotEditPageStateControllerComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ DotEditPageStateControllerComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(DotEditPageStateControllerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
