import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotTemplatePropsComponent } from './dot-template-props.component';

xdescribe('DotTemplatePropsComponent', () => {
    let component: DotTemplatePropsComponent;
    let fixture: ComponentFixture<DotTemplatePropsComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [DotTemplatePropsComponent]
        }).compileComponents();
    });

    beforeEach(() => {
        fixture = TestBed.createComponent(DotTemplatePropsComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
