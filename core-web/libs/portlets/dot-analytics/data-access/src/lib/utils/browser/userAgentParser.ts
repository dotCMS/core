export interface ParsedUserAgent {
    browser: {
        name: string;
        version: string;
    };
    device: {
        type: 'mobile' | 'tablet' | 'desktop';
        vendor?: string;
        model?: string;
    };
    os: {
        name: string;
        version?: string;
    };
    // Helpers
    isMobile: boolean;
    isTablet: boolean;
    isDesktop: boolean;
}

class UserAgentParser {
    private userAgent: string;

    constructor(userAgent: string) {
        this.userAgent = userAgent.toLowerCase();
    }

    public parse(): ParsedUserAgent {
        const browser = this.parseBrowser();
        const device = this.parseDevice();
        const os = this.parseOS();

        return {
            browser,
            device,
            os,
            isMobile: device.type === 'mobile',
            isTablet: device.type === 'tablet',
            isDesktop: device.type === 'desktop'
        };
    }

    private parseBrowser() {
        const ua = this.userAgent;

        // Chrome (debe ir antes que Safari)
        if (ua.includes('chrome') && !ua.includes('edg')) {
            const version = this.extractVersion(ua, /chrome\/([0-9.]+)/);

            return { name: 'Chrome', version };
        }

        // Edge (nuevo basado en Chromium)
        if (ua.includes('edg/') || ua.includes('edge/')) {
            const version = this.extractVersion(ua, /edg?\/([0-9.]+)/);

            return { name: 'Edge', version };
        }

        // Firefox
        if (ua.includes('firefox')) {
            const version = this.extractVersion(ua, /firefox\/([0-9.]+)/);

            return { name: 'Firefox', version };
        }

        // Safari (debe ir después de Chrome)
        if (ua.includes('safari') && !ua.includes('chrome')) {
            const version = this.extractVersion(ua, /version\/([0-9.]+)/);

            return { name: 'Safari', version };
        }

        // Opera
        if (ua.includes('opera') || ua.includes('opr/')) {
            const version = this.extractVersion(ua, /(opera|opr)\/([0-9.]+)/);

            return { name: 'Opera', version };
        }

        // Internet Explorer
        if (ua.includes('msie') || ua.includes('trident')) {
            const version = this.extractVersion(ua, /(msie|rv:)([0-9.]+)/);

            return { name: 'Internet Explorer', version };
        }

        return { name: 'Unknown', version: 'Unknown' };
    }

    private parseDevice(): {
        type: 'mobile' | 'tablet' | 'desktop';
        vendor?: string;
        model?: string;
    } {
        const ua = this.userAgent;

        // Mobile patterns
        const mobilePatterns = [
            /android.*mobile/,
            /iphone/,
            /ipod/,
            /windows phone/,
            /blackberry/,
            /bb10/,
            /mobile/,
            /phone/
        ];

        // Tablet patterns
        const tabletPatterns = [
            /ipad/,
            /android(?!.*mobile)/,
            /tablet/,
            /kindle/,
            /silk/,
            /playbook/
        ];

        let vendor: string | undefined;
        let model: string | undefined;

        // Detect vendor and model
        if (ua.includes('iphone') || ua.includes('ipad') || ua.includes('ipod')) {
            vendor = 'Apple';
            if (ua.includes('iphone')) model = 'iPhone';
            else if (ua.includes('ipad')) model = 'iPad';
            else if (ua.includes('ipod')) model = 'iPod';
        } else if (ua.includes('android')) {
            vendor = 'Android';
            // Try to extract specific Android device
            const samsungMatch = ua.match(/samsung|sm-|galaxy/);
            const huaweiMatch = ua.match(/huawei|honor/);
            const xiaomiMatch = ua.match(/xiaomi|mi |redmi/);

            if (samsungMatch) vendor = 'Samsung';
            else if (huaweiMatch) vendor = 'Huawei';
            else if (xiaomiMatch) vendor = 'Xiaomi';
        }

        // Check device type
        if (tabletPatterns.some((pattern) => pattern.test(ua))) {
            return { type: 'tablet', vendor, model };
        }

        if (mobilePatterns.some((pattern) => pattern.test(ua))) {
            return { type: 'mobile', vendor, model };
        }

        return { type: 'desktop', vendor, model };
    }

    private parseOS() {
        const ua = this.userAgent;

        // iOS
        if (ua.includes('iphone') || ua.includes('ipad') || ua.includes('ipod')) {
            const version = this.extractVersion(ua, /os ([0-9_]+)/);

            return {
                name: 'iOS',
                version: version.replace(/_/g, '.')
            };
        }

        // Android
        if (ua.includes('android')) {
            const version = this.extractVersion(ua, /android ([0-9.]+)/);

            return { name: 'Android', version };
        }

        // Windows
        if (ua.includes('windows')) {
            let version = 'Unknown';
            if (ua.includes('windows nt 10.0')) version = '10';
            else if (ua.includes('windows nt 6.3')) version = '8.1';
            else if (ua.includes('windows nt 6.2')) version = '8';
            else if (ua.includes('windows nt 6.1')) version = '7';
            else if (ua.includes('windows nt 6.0')) version = 'Vista';
            else if (ua.includes('windows nt 5.1')) version = 'XP';

            return { name: 'Windows', version };
        }

        // macOS
        if (ua.includes('mac os x') || ua.includes('macos')) {
            const version = this.extractVersion(ua, /mac os x ([0-9_]+)/);

            return {
                name: 'macOS',
                version: version.replace(/_/g, '.')
            };
        }

        // Linux
        if (ua.includes('linux')) {
            return { name: 'Linux', version: undefined };
        }

        // Chrome OS
        if (ua.includes('cros')) {
            return { name: 'Chrome OS', version: undefined };
        }

        return { name: 'Unknown', version: undefined };
    }

    private extractVersion(ua: string, regex: RegExp): string {
        const match = ua.match(regex);

        return match ? match[match.length - 1] : 'Unknown';
    }
}

// Export main function
export function parseUserAgent(userAgent?: string): ParsedUserAgent {
    // Use navigator.userAgent only when no arguments are provided
    // If undefined is explicitly passed, treat it as empty string for testing
    const ua =
        arguments.length === 0 && typeof navigator !== 'undefined'
            ? navigator?.userAgent || ''
            : userAgent || '';
    const parser = new UserAgentParser(ua);

    return parser.parse();
}

// Export convenient helpers
export function isMobile(userAgent?: string): boolean {
    return parseUserAgent(userAgent).isMobile;
}

export function isTablet(userAgent?: string): boolean {
    return parseUserAgent(userAgent).isTablet;
}

export function isDesktop(userAgent?: string): boolean {
    return parseUserAgent(userAgent).isDesktop;
}

export function getBrowserName(userAgent?: string): string {
    return parseUserAgent(userAgent).browser.name;
}

export function getDeviceType(userAgent?: string): 'mobile' | 'tablet' | 'desktop' {
    return parseUserAgent(userAgent).device.type;
}
