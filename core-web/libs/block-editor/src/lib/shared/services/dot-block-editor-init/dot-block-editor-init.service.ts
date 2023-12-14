import { inject, Injectable } from '@angular/core';

import { DotAiService } from '../dot-ai/dot-ai.service';

/**
 * Service for initializing the Dot Block Editor external configuration.
 */
@Injectable({
    providedIn: 'root'
})
export class DotBlockEditorInitService {
    private dotAiService: DotAiService = inject(DotAiService);

    private _isPluginInstalled = false;

    get isPluginInstalled(): boolean {
        return this._isPluginInstalled;
    }

    /**
     * Initializes the Block Editor external configurations.
     *
     * @return {Promise<any>} A Promise that resolves when the Block Editor is initialized.
     */
    initializeBlockEditor(): Promise<boolean[]> {
        return Promise.all([
            this.verifyAiDotCMSPlugin()
            // additional configs
        ]);
    }

    /**
     * Verifies the installation of the AiDotCMSPlugin.
     *
     * @returns {Promise<boolean>} A Promise that resolves to a boolean value indicating whether the plugin is installed or not.
     *
     * @throws {Error} If an error occurs while checking the plugin installation.
     */
    async verifyAiDotCMSPlugin(): Promise<boolean> {
        if (this._isPluginInstalled) {
            return Promise.resolve(this._isPluginInstalled);
        }

        this._isPluginInstalled = false;

        try {
            const response = await this.dotAiService.checkPluginInstallation().toPromise();
            this._isPluginInstalled = response.status === 200;
        } catch (error) {
            console.error('Error checking plugin installation:', error);
        }

        return this._isPluginInstalled;
    }
}
