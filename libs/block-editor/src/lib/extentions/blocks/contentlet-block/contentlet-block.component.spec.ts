import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ContentletBlockComponent } from './contentlet-block.component';

describe('ContentletBlockComponent', () => {
  let component: ContentletBlockComponent;
  let fixture: ComponentFixture<ContentletBlockComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ ContentletBlockComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ContentletBlockComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
