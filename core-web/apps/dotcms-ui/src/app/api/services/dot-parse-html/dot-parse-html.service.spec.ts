import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotParseHtmlService } from './dot-parse-html.service';

@Component({
    selector: 'dot-test-host-component',
    template: ``,
    standalone: false
})
class TestHostComponent {}

describe('DotParseHtmlService', () => {
    let dotParseHtmlService: DotParseHtmlService;
    let fixture: ComponentFixture<TestHostComponent>;
    let target;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [],
            declarations: [TestHostComponent],
            providers: [DotParseHtmlService]
        }).compileComponents();

        target = document.createElement('div');

        fixture = TestBed.createComponent(TestHostComponent);
        dotParseHtmlService = fixture.debugElement.injector.get(DotParseHtmlService);
    });

    it('should render html and script', () => {
        const code = `<button>Test</button><script>console.log('test')</script>`;
        dotParseHtmlService.parse(code, target, false);

        const button = target.querySelectorAll('button');
        const script = target.querySelectorAll('script');

        expect(button.length).toBe(1);
        expect(script.length).toBe(1);
        expect(script[0].getAttribute('type')).toBe('text/javascript');
        expect(script[0].innerHTML).toBe(`console.log('test')`);
    });

    it('should clear content and render html', () => {
        const code = '<input/><span>Name</span>';
        dotParseHtmlService.parse(code, target, true);
        expect(target.childNodes.length).toEqual(2);
    });

    it('should append content ', () => {
        const code = '<span>last Content</span>';
        dotParseHtmlService.parse(code, target, false);
        expect(target.childNodes.length).toEqual(1);
    });
});
