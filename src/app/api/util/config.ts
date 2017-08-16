import { Injectable } from '@angular/core';
import { HttpRequestUtils } from './httpRequestUtils';

/**
 * Encapsulates generic configuration, such as the dev mode, etc.
 * @author jsanca
 */
@Injectable()
export class Config {
    private httpRequestUtils: HttpRequestUtils;

    constructor() {
        this.httpRequestUtils = new HttpRequestUtils();
    }
} // E:O:F:Config
