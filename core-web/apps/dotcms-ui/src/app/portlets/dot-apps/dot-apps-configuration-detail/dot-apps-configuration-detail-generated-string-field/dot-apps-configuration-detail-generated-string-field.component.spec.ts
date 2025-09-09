/* eslint-disable @typescript-eslint/no-explicit-any */

import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator,
    SpyObject
} from '@ngneat/spectator/jest';
import { of, Subject } from 'rxjs';

import { HttpClient, provideHttpClient } from '@angular/common/http';
import { NO_ERRORS_SCHEMA } from '@angular/core';

import { ConfirmationService } from 'primeng/api';

import { DotMessageService } from '@dotcms/data-access';

import { DotAppsConfigurationDetailGeneratedStringFieldComponent } from './dot-apps-configuration-detail-generated-string-field.component';

describe('DotAppsConfigurationDetailGeneratedStringFieldComponent', () => {
    let spectator: Spectator<DotAppsConfigurationDetailGeneratedStringFieldComponent>;
    let confirmationService: ConfirmationService;
    let httpClient: SpyObject<HttpClient>;

    const mockField = {
        name: 'testField',
        label: 'Test Field',
        hint: 'This is a test hint',
        required: true,
        type: 'generated-string',
        buttonLabel: 'Generate',
        buttonEndpoint: '/api/generate-string'
    };

    const createComponent = createComponentFactory({
        component: DotAppsConfigurationDetailGeneratedStringFieldComponent,
        providers: [provideHttpClient(), mockProvider(DotMessageService), ConfirmationService],
        schemas: [NO_ERRORS_SCHEMA],
        detectChanges: false
    });

    beforeEach(() => {
        spectator = createComponent();
        confirmationService = spectator.inject(ConfirmationService);
        httpClient = spectator.inject(HttpClient, true);
        spectator.setInput('field', mockField);
    });

    it('should create the component', () => {
        expect(spectator.component).toBeTruthy();
    });

    it('should render the input field', () => {
        spectator.detectChanges();
        const input = spectator.query(byTestId('generated-string-input'));
        expect(input).toBeTruthy();
    });

    it('should render the generate button', () => {
        spectator.detectChanges();
        const button = spectator.query(byTestId('generate-button'));
        expect(button).toBeTruthy();
    });

    it('should call HTTP service when button is clicked with empty input', () => {
        // Mock HTTP response
        jest.spyOn(httpClient, 'get').mockReturnValue(of('generated-string-value'));

        spectator.detectChanges();

        // Ensure input is empty
        spectator.component.$value.set('');
        spectator.detectChanges();

        const button = spectator.query(byTestId('generate-button'));
        spectator.click(button);

        expect(httpClient.get).toHaveBeenCalledWith(mockField.buttonEndpoint, {
            responseType: 'text'
        });
    });

    describe('Confirmation Dialog Tests', () => {
        it('should generate new string when user confirms (YES)', async () => {
            // Arrange
            const mockGeneratedValue = 'new-generated-value';
            jest.spyOn(httpClient, 'get').mockReturnValue(of(mockGeneratedValue));

            spectator.detectChanges();
            spectator.component.$value.set('existing-value');

            const button = spectator.query(byTestId('generate-button'));

            // Act - Click the generate button to show dialog
            spectator.click(button);
            spectator.detectChanges();

            // Wait for the dialog to render and find the Yes button
            await spectator.fixture.whenStable();
            spectator.detectChanges();

            // Look for the actual Yes button in the confirmation popup
            const yesButton = spectator.query('.p-confirm-popup-accept');

            expect(yesButton).toBeTruthy();

            // Real click on the Yes button
            spectator.click(yesButton);
            spectator.detectChanges();

            // Assert
            expect(httpClient.get).toHaveBeenCalledWith(mockField.buttonEndpoint, {
                responseType: 'text'
            });
            expect(spectator.component.$value()).toBe(mockGeneratedValue);
        });

        it('should NOT generate new string when user cancels (NO)', async () => {
            // Arrange
            const originalValue = 'existing-value';
            spectator.detectChanges();
            spectator.component.$value.set(originalValue);

            // Create spy for httpClient.get to verify it's not called
            const httpGetSpy = jest.spyOn(httpClient, 'get');

            const button = spectator.query(byTestId('generate-button'));

            // Act - Click the generate button to show dialog
            spectator.click(button);
            spectator.detectChanges();

            // Wait for the dialog to render and find the No button
            await spectator.fixture.whenStable();
            spectator.detectChanges();

            // Look for the actual No button in the confirmation popup
            const noButton = spectator.query('.p-confirm-popup-reject');

            expect(noButton).toBeTruthy();

            // Real click on the No button
            spectator.click(noButton);
            spectator.detectChanges();

            // Assert
            expect(httpGetSpy).not.toHaveBeenCalled();
            expect(spectator.component.$value()).toBe(originalValue); // Value unchanged
        });

        it('should bypass confirmation and generate directly when input is empty', async () => {
            // Arrange
            const mockGeneratedValue = 'generated-value';
            jest.spyOn(httpClient, 'get').mockReturnValue(of(mockGeneratedValue));

            spectator.detectChanges();
            spectator.component.$value.set(''); // Empty input

            const button = spectator.query(byTestId('generate-button'));

            // Act
            spectator.click(button);
            spectator.detectChanges();

            // Wait for any potential async behavior
            await spectator.fixture.whenStable();
            spectator.detectChanges();

            // Assert - No confirmation popup should appear
            const yesButton = spectator.query('.p-confirm-popup-accept');
            const noButton = spectator.query('.p-confirm-popup-reject');

            expect(yesButton).toBeFalsy(); // No Yes button should exist
            expect(noButton).toBeFalsy(); // No No button should exist

            // Should generate directly without confirmation
            expect(httpClient.get).toHaveBeenCalledWith(mockField.buttonEndpoint, {
                responseType: 'text'
            });
            expect(spectator.component.$value()).toBe(mockGeneratedValue);
        });

        it('should handle confirmation accept scenario', () => {
            // Arrange
            const mockGeneratedValue = 'accepted-generated-value';
            jest.spyOn(httpClient, 'get').mockReturnValue(of(mockGeneratedValue));

            const originalValue = 'original-value';
            spectator.detectChanges();
            spectator.component.$value.set(originalValue);

            let capturedConfig: any;
            jest.spyOn(confirmationService, 'confirm').mockImplementation((config) => {
                capturedConfig = config;

                return confirmationService;
            });

            const button = spectator.query(byTestId('generate-button'));

            // Act
            spectator.click(button);

            // Simulate user clicking "Accept"
            if (capturedConfig && capturedConfig.accept) {
                capturedConfig.accept();
            }

            // Assert
            expect(confirmationService.confirm).toHaveBeenCalled();
            expect(httpClient.get).toHaveBeenCalled();
            expect(spectator.component.$value()).toBe(mockGeneratedValue);
        });

        it('should handle confirmation reject scenario', () => {
            // Arrange
            const originalValue = 'original-value';
            spectator.detectChanges();
            spectator.component.$value.set(originalValue);

            // Create spy for httpClient.get to verify it's not called
            const httpGetSpy = jest.spyOn(httpClient, 'get');

            let capturedConfig: any;
            jest.spyOn(confirmationService, 'confirm').mockImplementation((config) => {
                capturedConfig = config;

                return confirmationService;
            });

            const button = spectator.query(byTestId('generate-button'));

            // Act
            spectator.click(button);

            // Simulate user clicking "Reject"
            if (capturedConfig && capturedConfig.reject) {
                capturedConfig.reject();
            }

            // Assert
            expect(confirmationService.confirm).toHaveBeenCalled();
            expect(httpGetSpy).not.toHaveBeenCalled();
            expect(spectator.component.$value()).toBe(originalValue);
        });
    });

    describe('Loading State Tests', () => {
        it('should show loading state during string generation', () => {
            // Arrange
            const responseSubject = new Subject<string>();
            jest.spyOn(httpClient, 'get').mockReturnValue(responseSubject.asObservable());

            spectator.detectChanges();
            spectator.component.$value.set(''); // Empty input to bypass confirmation

            const button = spectator.query(byTestId('generate-button'));

            // Act
            spectator.click(button);
            spectator.detectChanges();

            // Assert - Loading state should be active
            expect(spectator.component.$isLoading()).toBe(true);
            expect(button).toBeDisabled();

            // Complete the observable
            responseSubject.next('generated-value');
            responseSubject.complete();
        });

        it('should reset loading state after successful generation', async () => {
            // Arrange
            const responseSubject = new Subject<string>();
            const mockGeneratedValue = 'generated-value';
            jest.spyOn(httpClient, 'get').mockReturnValue(responseSubject.asObservable());

            spectator.detectChanges();
            spectator.component.$value.set(''); // Empty input to bypass confirmation

            const button = spectator.query(byTestId('generate-button'));

            // Act
            spectator.click(button);
            spectator.detectChanges();

            // Initial loading state
            expect(spectator.component.$isLoading()).toBe(true);

            // Complete the observable
            responseSubject.next(mockGeneratedValue);
            responseSubject.complete();

            // Wait for observable to complete
            await spectator.fixture.whenStable();
            spectator.detectChanges();

            // Assert - Loading state should be reset
            expect(spectator.component.$isLoading()).toBe(false);
            expect(spectator.component.$value()).toBe(mockGeneratedValue);
        });

        it('should disable button when loading', () => {
            // Arrange
            spectator.detectChanges();
            spectator.component.$isLoading.set(true);
            spectator.detectChanges();

            const button = spectator.query(byTestId('generate-button'));

            // Assert
            expect(button).toBeDisabled();
        });

        it('should disable button when component is disabled', () => {
            // Arrange
            spectator.detectChanges();
            spectator.component.$isDisabled.set(true);
            spectator.detectChanges();

            const button = spectator.query(byTestId('generate-button'));

            // Assert
            expect(button).toBeDisabled();
        });

        it('should disable button when both loading and disabled', () => {
            // Arrange
            spectator.detectChanges();
            spectator.component.$isLoading.set(true);
            spectator.component.$isDisabled.set(true);
            spectator.detectChanges();

            const button = spectator.query(byTestId('generate-button'));

            // Assert
            expect(button).toBeDisabled();
        });

        it('should reset loading state after HTTP error', async () => {
            // Arrange
            const responseSubject = new Subject<string>();
            const mockError = new Error('HTTP Error');
            jest.spyOn(httpClient, 'get').mockReturnValue(responseSubject.asObservable());
            jest.spyOn(console, 'error'); // Spy on console.error to avoid console output

            spectator.detectChanges();
            spectator.component.$value.set(''); // Empty input to bypass confirmation

            const button = spectator.query(byTestId('generate-button'));

            // Act
            spectator.click(button);
            spectator.detectChanges();

            // Initial loading state
            expect(spectator.component.$isLoading()).toBe(true);

            // Emit error
            responseSubject.error(mockError);

            // Wait for observable to complete (with error)
            await spectator.fixture.whenStable();
            spectator.detectChanges();

            // Assert - Loading state should be reset after error
            expect(spectator.component.$isLoading()).toBe(false);
            expect(console.error).toHaveBeenCalledWith('Error generating string:', mockError);
        });
    });
});
