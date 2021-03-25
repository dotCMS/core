import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DotGraphqlComponent } from './dot-graphql.component';
import * as graphql from 'graphql-playground-html';
import { By } from '@angular/platform-browser';

describe('DotGraphqlComponent', () => {
    let component: DotGraphqlComponent;
    let fixture: ComponentFixture<DotGraphqlComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [DotGraphqlComponent]
        }).compileComponents();
    });

    beforeEach(() => {
        fixture = TestBed.createComponent(DotGraphqlComponent);
        component = fixture.componentInstance;
        spyOn(graphql, 'renderPlaygroundPage').and.returnValue('markup');
        fixture.detectChanges();
    });

    it('should create', () => {
        const iframe = fixture.debugElement.query(By.css('[data-testId="iframe"]'));
        expect(graphql.renderPlaygroundPage).toHaveBeenCalledWith({ endpoint: '/api/v1/graphql' });
        expect(iframe.nativeElement.contentDocument.body.innerText).toEqual('markup');
    });
});
