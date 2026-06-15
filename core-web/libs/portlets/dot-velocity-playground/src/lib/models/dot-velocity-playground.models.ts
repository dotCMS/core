export interface DotVelocityPlaygroundForm {
    velocity: string;
}

export type DotVelocityResponseContentType = 'json' | 'xml' | 'plaintext';

export interface DotVelocityPlaygroundResponse {
    body: string;
    contentType: DotVelocityResponseContentType;
    elapsedMs: number;
}
