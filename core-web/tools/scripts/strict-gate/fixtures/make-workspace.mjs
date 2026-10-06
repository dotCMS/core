/**
 * Miniature Nx-shaped workspaces for the mapping and configuration-selection tests.
 *
 * Reproduces the two structural facts the real workspace has and that the harness must cope with:
 * a base config that turns strict OFF and is inherited by everyone, and path aliases that point at
 * SOURCES rather than build output — which is what drags a dependency's files into your program.
 */
import fs from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';

/**
 * @typedef {Object} FixtureProject
 * @property {string} name
 * @property {string} root                       Workspace-relative, e.g. `libs/thing`.
 * @property {Record<string,string>} files       Workspace-relative path → contents.
 * @property {'lib'|'app'|'references'} [shape]  Which tsconfig layout to emit. Default `lib`.
 * @property {string[]} [dependsOn]              Project names this one imports by alias.
 * @property {Record<string,unknown>} [angularCompilerOptions]
 */

/**
 * @param {{ projects: FixtureProject[], strict?: boolean }} spec
 */
export async function makeWorkspace({ projects, strict = false }) {
    const dir = await fs.mkdtemp(path.join(os.tmpdir(), 'strict-gate-ws-'));

    const write = async (relative, contents) => {
        const target = path.join(dir, relative);
        await fs.mkdir(path.dirname(target), { recursive: true });
        await fs.writeFile(
            target,
            typeof contents === 'string' ? contents : JSON.stringify(contents, null, 4),
            'utf8'
        );
    };

    // Aliases point at sources, exactly as tsconfig.base.json does in the real workspace.
    const paths = {};
    for (const project of projects) {
        paths[`@fixture/${project.name}`] = [`./${project.root}/src/index.ts`];
    }

    await write('tsconfig.base.json', {
        compilerOptions: {
            target: 'es2022',
            module: 'esnext',
            moduleResolution: 'bundler',
            lib: ['es2022', 'dom'],
            skipLibCheck: true,
            strict,
            baseUrl: '.',
            paths
        }
    });

    await write('nx.json', {
        namedInputs: {
            default: ['{projectRoot}/**/*', 'sharedGlobals'],
            sharedGlobals: ['{workspaceRoot}/tsconfig.base.json', '{workspaceRoot}/nx.json']
        }
    });

    for (const project of projects) {
        const { name, root, files, shape = 'lib', angularCompilerOptions } = project;
        await write(path.join(root, 'project.json'), { name, root, targets: {} });

        const depth = root.split('/').length;
        const toBase = `${'../'.repeat(depth)}tsconfig.base.json`;

        if (shape === 'references') {
            // The real portlets do this: a root config that owns no files and only points at others.
            // It must resolve to zero files and exclude itself from selection with no special-casing.
            await write(path.join(root, 'tsconfig.json'), {
                extends: toBase,
                files: [],
                include: [],
                references: [{ path: './tsconfig.lib.json' }, { path: './tsconfig.spec.json' }],
                ...(angularCompilerOptions ? { angularCompilerOptions } : {})
            });
            await write(path.join(root, 'tsconfig.lib.json'), {
                extends: './tsconfig.json',
                compilerOptions: { outDir: '../../dist' },
                include: ['src/**/*.ts'],
                exclude: ['**/*.spec.ts']
            });
            await write(path.join(root, 'tsconfig.spec.json'), {
                extends: './tsconfig.json',
                include: ['src/**/*.spec.ts']
            });
        } else {
            const mainName = shape === 'app' ? 'tsconfig.app.json' : 'tsconfig.lib.json';
            await write(path.join(root, 'tsconfig.json'), {
                extends: toBase,
                files: [],
                include: [],
                references: [{ path: `./${mainName}` }, { path: './tsconfig.spec.json' }],
                ...(angularCompilerOptions ? { angularCompilerOptions } : {})
            });
            await write(path.join(root, mainName), {
                extends: './tsconfig.json',
                include: ['src/**/*.ts'],
                exclude: ['**/*.spec.ts']
            });
            await write(path.join(root, 'tsconfig.spec.json'), {
                extends: './tsconfig.json',
                include: ['src/**/*.spec.ts']
            });
        }

        for (const [relative, contents] of Object.entries(files)) {
            await write(path.join(root, relative), contents);
        }
    }

    return {
        dir,
        projects: projects.map(({ name, root }) => ({ name, root })),
        cleanup: () => fs.rm(dir, { recursive: true, force: true })
    };
}
