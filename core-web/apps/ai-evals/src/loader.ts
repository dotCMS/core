import { parse } from 'yaml';

import fs from 'node:fs';
import path from 'node:path';

import type { TaskDefinition } from './types';

export function loadTask(filePath: string): TaskDefinition {
    const resolved = path.resolve(filePath);
    const content = fs.readFileSync(resolved, 'utf-8');
    const raw = parse(content) as TaskDefinition;

    if (!raw.id) throw new Error(`Task file ${filePath} is missing required field: id`);
    if (!raw.prompt) throw new Error(`Task file ${filePath} is missing required field: prompt`);
    if (!raw.runs || raw.runs < 1) throw new Error(`Task file ${filePath}: runs must be >= 1`);
    if (!Array.isArray(raw.assertions))
        throw new Error(`Task file ${filePath}: assertions must be an array`);

    return raw;
}

export function loadTasksFromDir(dir: string): TaskDefinition[] {
    const resolved = path.resolve(dir);
    const files = fs.readdirSync(resolved).filter((f) => f.endsWith('.yaml') || f.endsWith('.yml'));
    return files.map((f) => loadTask(path.join(resolved, f)));
}
