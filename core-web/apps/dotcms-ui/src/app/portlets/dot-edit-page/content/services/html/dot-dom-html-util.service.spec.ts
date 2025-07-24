import { TestBed, waitForAsync } from '@angular/core/testing';

import { DotDOMHtmlUtilService } from './dot-dom-html-util.service';

describe('DotDOMHtmlUtilService', () => {
    let service: DotDOMHtmlUtilService;

    beforeEach(waitForAsync(() => {
        TestBed.configureTestingModule({
            providers: [DotDOMHtmlUtilService]
        });

        service = TestBed.inject(DotDOMHtmlUtilService);
    }));

    it('should create a link element', () => {
        const href = 'https://testing/test.css';

        const cssElementCreated = service.createLinkElement(href);

        expect(cssElementCreated.rel).toEqual('stylesheet');
        expect(cssElementCreated.type).toEqual('text/css');
        expect(cssElementCreated.media).toEqual('all');
        expect(cssElementCreated.href).toEqual(href);
    });

    it('should create an style element', () => {
        const styleElementCreated = service.createStyleElement('h1 {color: red}');
        expect(styleElementCreated.innerHTML).toEqual('h1 {color: red}');
    });

    it('should create a external script', () => {
        const src = 'https://testing/test.js';
        const onloadCallbackFunc = () => {
            //
        };

        const scriptElementCreated = service.creatExternalScriptElement(src, onloadCallbackFunc);

        expect(scriptElementCreated.src).toEqual(src);
        expect(scriptElementCreated.onload).toEqual(onloadCallbackFunc);
    });

    it('should create a inline script', () => {
        const text = 'var a = 2;';

        const scriptElementCreated = service.createInlineScriptElement(text);

        expect(scriptElementCreated.text).toEqual(text);
    });

    it('should get a button html code', () => {
        const label = 'ButtonLabel';
        const className = 'ButtonClass';
        const dataset = {
            a: 'a value',
            b: 'b value'
        };
        const buttonHTML = service.getButtomHTML(label, className, dataset);

        const divElement = document.createElement('div');
        divElement.innerHTML = buttonHTML;
        const button = divElement.querySelector('button');

        expect(button.getAttribute('class')).toEqual('ButtonClass', 'button class is wrong');
        expect(button.getAttribute('type')).toEqual('button', 'button type is wrong');
        expect(button.getAttribute('role')).toEqual('button', 'button role is wrong');
        expect(button.getAttribute('aria-label')).toEqual(
            'ButtonLabel',
            'button aria-label is wrong'
        );
        expect(button.dataset['a']).toEqual('a value', 'button datset[a] is wrong');
        expect(button.dataset['b']).toEqual('b value', 'button datset[a] is wrong');

        expect(button.disabled).toBeFalsy();
    });
});
