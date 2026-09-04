export type ConnectResult =
    | { ok: true; toolCount: number }
    | { ok: false; cause: 'fetch-failed' | 'runtime-unsupported' | 'exited' | 'timeout'; detail: string };

/**
 * Launch the configured server and confirm it reports its tools (FR-024a).
 * The token reaches the child through its ENVIRONMENT, never argv (FR-022).
 */
export async function confirmConnection(_args: {
    url: string;
    token: string;
    timeoutMs?: number;
}): Promise<ConnectResult> {
    throw new Error('not implemented');
}
