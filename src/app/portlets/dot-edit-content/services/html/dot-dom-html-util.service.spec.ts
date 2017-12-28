import { DotDOMHtmlUtilService } from './dot-dom-html-util.service';
import { async } from '@angular/core/testing';
import { TestBed } from '@angular/core/testing';
import { window } from 'rxjs/operator/window';

describe('DotDOMHtmlUtilService', () => {
    let dotDOMHtmlUtilService: DotDOMHtmlUtilService;

    beforeEach(
        async(() => {
            TestBed.configureTestingModule({
                providers: [DotDOMHtmlUtilService],
                imports: []
            });

            dotDOMHtmlUtilService = TestBed.get(DotDOMHtmlUtilService);
        })
    );

    it('should create a link element', () => {
        const href = 'https://testing/test.css';

        const cssElementCreated = dotDOMHtmlUtilService.createLinkElement(href);

        expect(cssElementCreated.rel).toEqual('stylesheet');
        expect(cssElementCreated.type ).toEqual('text/css');
        expect(cssElementCreated.media).toEqual('all');
        expect(cssElementCreated.href).toEqual(href);
    });

    it('should create an style element', () => {
        const styleElementCreated = dotDOMHtmlUtilService.createStyleElement('h1 {color: red}');
        expect(styleElementCreated.innerHTML).toEqual('h1 {color: red}');
    });

    it('should create a external script', () => {
        const src = 'https://testing/test.js';
        const onloadCallbackFunc = () => {};

        const scriptElementCreated = dotDOMHtmlUtilService.creatExternalScriptElement(src, onloadCallbackFunc);

        expect(scriptElementCreated.src).toEqual(src);
        expect(scriptElementCreated.onload).toEqual(onloadCallbackFunc);
    });

    it('should create a inline script', () => {
        const text = 'var a = 2;';

        const scriptElementCreated = dotDOMHtmlUtilService.createInlineScriptElement(text);

        expect(scriptElementCreated.text).toEqual(text);
    });
});
