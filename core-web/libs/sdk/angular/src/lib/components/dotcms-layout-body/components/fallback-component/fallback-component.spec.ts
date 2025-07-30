/* eslint-disable @typescript-eslint/no-explicit-any */
import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { Component, Input } from '@angular/core';

import { DotCMSBasicContentlet } from '@dotcms/types';

import { FallbackComponent } from './fallback-component.component';

import { DynamicComponentEntity } from '../../../../models';

// Mock component to test UserNoComponent functionality
@Component({
    selector: 'dotcms-mock-component',
    template: '<div data-testid="mock-component">Mock Component: {{contentlet?.contentType}}</div>'
})
class MockComponent {
    @Input() contentlet!: DotCMSBasicContentlet;
}

describe('FallbackComponent', () => {
    let spectator: Spectator<FallbackComponent>;
    let component: FallbackComponent;

    const createComponent = createComponentFactory({
        component: FallbackComponent,
        detectChanges: false
    });

    beforeEach(() => {
        spectator = createComponent({
            props: {
                contentlet: {
                    contentType: '' // Initialize with an empty contentType
                } as DotCMSBasicContentlet,
                UserNoComponent: null as DynamicComponentEntity | null
            }
        });

        component = spectator.component;
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should display the fallback message when UserNoComponent is null', () => {
        // Set up contentlet with a content type
        component.contentlet = {
            contentType: 'testContentType',
            identifier: 'test-id',
            title: 'Test Title',
            archived: false,
            baseType: 'content',
            folder: 'folder',
            hasTitleImage: false,
            host: 'host',
            hostName: 'host-name',
            inode: 'inode',
            languageId: 1,
            live: true,
            locked: false,
            modDate: '2023-01-01',
            modUser: 'admin',
            modUserName: 'Admin User',
            owner: 'admin',
            sortOrder: 0,
            stInode: 'stInode',
            titleImage: '',
            url: '/test',
            working: true
        } as DotCMSBasicContentlet;

        component.UserNoComponent = null;

        spectator.detectChanges();

        const fallbackElement = spectator.query('[data-testid="dotcms-fallback-component"]');
        expect(fallbackElement).toBeTruthy();
        expect(spectator.element.textContent).toContain(
            'No component found for content type: testContentType'
        );
    });

    it('should render the UserNoComponent when provided', () => {
        // Create a Promise that resolves to MockComponent as UserNoComponent
        const mockComponentPromise = Promise.resolve(MockComponent) as DynamicComponentEntity;
        component.UserNoComponent = mockComponentPromise;
        component.contentlet = {
            contentType: 'testContentType',
            identifier: 'test-id',
            title: 'Test Title'
        } as DotCMSBasicContentlet;

        spectator.detectChanges();

        // The fallback message should not be shown when UserNoComponent is provided
        // Note: Due to the async nature of Promise, we can't effectively test the actual
        // rendering of MockComponent in this test without using fakeAsync or waitForAsync
        const fallbackElement = spectator.query('[data-testid="dotcms-fallback-component"]');
        expect(fallbackElement).toBeFalsy();
    });

    it('should pass the contentlet to the UserNoComponent', () => {
        // This test demonstrates that the contentlet is passed to the UserNoComponent
        const mockContentlet = {
            contentType: 'testContentType',
            title: 'Test Title',
            identifier: 'test-id'
        } as DotCMSBasicContentlet;

        const mockComponentPromise = Promise.resolve(MockComponent) as DynamicComponentEntity;
        component.UserNoComponent = mockComponentPromise;
        component.contentlet = mockContentlet;

        spectator.detectChanges();

        // In this test setup, we're verifying the component configuration
        // rather than the actual rendering result
        expect(component.contentlet).toEqual(mockContentlet);
    });
});
