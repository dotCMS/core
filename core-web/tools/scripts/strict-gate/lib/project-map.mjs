/**
 * Maps changed files to the project that OWNS them.
 *
 * Deliberately not `nx affected`: that returns projects which DEPEND on what changed, and
 * tsconfig.base.json / nx.json are declared under nx.json's sharedGlobals — so touching either
 * makes all 56 projects affected. Ownership is a property of where a file lives (FR-010).
 */
import fs from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import { run } from './exec.mjs';
import { resolveBin } from './resolve-tools.mjs';

const owns = (root, filePath) => filePath === root || filePath.startsWith(`${root}/`);

/**
 * @param {{ projects: {name:string,root:string}[], files: object[] }} input
 * @returns {{ targets: {project:string,root:string,files:string[]}[], unmapped: {path:string,reason:string}[] }}
 */
export function mapFilesToProjects({ projects, files }) {
    // Longest root first so a nested project wins over its parent.
    const ordered = [...projects].sort((a, b) => b.root.length - a.root.length);
    const byProject = new Map();
    const unmapped = [];

    for (const file of files) {
        const owner = ordered.find((p) => owns(p.root, file.path));
        if (!owner) {
            unmapped.push({
                path: file.path,
                reason: 'no project root is a path prefix of this file'
            });
            continue;
        }
        if (!byProject.has(owner.name)) {
            byProject.set(owner.name, { project: owner.name, root: owner.root, files: [] });
        }
        byProject.get(owner.name).files.push(file.path);
    }

    return { targets: [...byProject.values()], unmapped };
}

/**
 * Reads project roots from the Nx graph — roots only, never the dependency edges.
 * @returns {Promise<{name:string,root:string}[]>} roots relative to `repoDir`.
 */
export async function readProjects({ workspaceDir, repoDir }) {
    const out = path.join(await fs.mkdtemp(path.join(os.tmpdir(), 'strict-gate-graph-')), 'graph.json');
    await run('node', [resolveBin('nx'), 'graph', '--file', out], { cwd: workspaceDir });

    const graph = JSON.parse(await fs.readFile(out, 'utf8'));
    const nodes = graph.graph?.nodes ?? graph.nodes ?? {};
    const prefix = path.relative(repoDir, workspaceDir);

    const projects = Object.entries(nodes)
        .map(([name, node]) => ({ name, root: node?.data?.root }))
        .filter((p) => typeof p.root === 'string' && p.root.length > 0)
        .map((p) => ({ name: p.name, root: prefix ? path.join(prefix, p.root) : p.root }));

    await fs.rm(path.dirname(out), { recursive: true, force: true });
    return projects;
}
