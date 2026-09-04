export interface SkillsResult {
    ok: boolean;
    /** The exact command to re-run, printed when installation fails (FR-026). */
    command: string;
    reason?: string;
}

export async function installSkills(_args: {
    agentIds: string[];
    global: boolean;
}): Promise<SkillsResult> {
    throw new Error('not implemented');
}
