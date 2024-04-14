import { TestBed } from '@angular/core/testing';

// import { ElementRef } from '@angular/core';
import { InlineEditService } from './inline-edit.service';

describe('InlineEditService', () => {
    let service: InlineEditService;

    beforeEach(() => {
        TestBed.configureTestingModule({});
        service = TestBed.inject(InlineEditService);
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });

    // it('should inject inline edit', () => {
    //     const iframe = {
    //         nativeElement: {
    //             contentDocument: document,
    //             contentWindow: window
    //         }
    //     } as ElementRef<HTMLIFrameElement>;

    //     service.injectInlineEdit(iframe);

    //     const script = document.querySelector('script[data-inline="true"]');
    //     const style = document.querySelector('style[data-inode][data-field-name][data-mode]');

    //     expect(script).toBeTruthy();
    //     expect(style).toBeTruthy();
    // });

    // it('should handle inline edit', () => {
    //     const event = new MouseEvent('click');
    //     const target = document.createElement('div');
    //     target.dataset.mode = 'minimal';
    //     event.target = target;

    //     service.handleInlineEdit(event);

    //     // Add your assertions here
    // });

    // it('should replace contentlet on copy', () => {
    //     const oldInode = 'old-inode';
    //     const newInode = 'new-inode';

    //     const contentlet = document.createElement('div');
    //     contentlet.dataset.dotInode = oldInode;

    //     const editorElement = document.createElement('div');
    //     editorElement.dataset.inode = oldInode;
    //     contentlet.appendChild(editorElement);

    //     document.body.appendChild(contentlet);

    //     service.replaceContentletONCopy({ oldInode, newInode });

    //     expect(contentlet.dataset.dotInode).toBe(newInode);
    //     expect(editorElement.dataset.inode).toBe(newInode);
    // });

    // it('should handle inline edit events', () => {
    //     const editor = {
    //         on: jest.fn()
    //     };

    //     service.handleInlineEditEvents(editor);

    //     // Add your assertions here
    // });

    // it('should check if editor element is in multiple pages', () => {
    //     const editorElement = document.createElement('div');
    //     const contentlet = document.createElement('div');
    //     contentlet.dataset.dotOnNumberOfPages = '2';
    //     contentlet.appendChild(editorElement);

    //     const result = service.isInMultiplePages(editorElement);

    //     expect(result).toBe(true);
    // });
});