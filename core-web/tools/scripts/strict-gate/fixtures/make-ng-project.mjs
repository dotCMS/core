/**
 * An Angular fixture project with template strictness switched OFF.
 *
 * This mirrors the state of the four real applications, which carry
 * `TODO(#35930): re-enable strictTemplates once Angular 22 template errors are fixed per app`.
 * The template arm's entire premise is that the harness can force strictness on a project shaped
 * exactly like this without touching a single file it owns.
 *
 * Two components on purpose:
 *  - separate template  → the diagnostic's originating file is the .html
 *  - inline template    → the diagnostic's originating file is the .ts
 * The filter has to attribute both correctly or template findings land on the wrong file.
 */
import fs from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';

/**
 * @param {{ strictTemplates?: boolean, withViolations?: boolean }} [options]
 */
export async function makeNgProject({ strictTemplates = false, withViolations = true } = {}) {
    const dir = await fs.mkdtemp(path.join(os.tmpdir(), 'strict-gate-ng-'));
    const root = 'libs/fixture-ng';

    const write = async (relative, contents) => {
        const target = path.join(dir, relative);
        await fs.mkdir(path.dirname(target), { recursive: true });
        await fs.writeFile(
            target,
            typeof contents === 'string' ? contents : JSON.stringify(contents, null, 4),
            'utf8'
        );
    };

    await write('tsconfig.base.json', {
        compilerOptions: {
            target: 'es2022',
            module: 'esnext',
            moduleResolution: 'bundler',
            lib: ['es2022', 'dom'],
            skipLibCheck: true,
            experimentalDecorators: true,
            strict: false,
            baseUrl: '.'
        }
    });

    await write('nx.json', { namedInputs: { sharedGlobals: ['{workspaceRoot}/nx.json'] } });
    await write(path.join(root, 'project.json'), { name: 'fixture-ng', root, targets: {} });

    await write(path.join(root, 'tsconfig.json'), {
        extends: '../../tsconfig.base.json',
        files: [],
        include: [],
        references: [{ path: './tsconfig.lib.json' }],
        angularCompilerOptions: {
            // Deliberately off — the harness must override this without editing the file.
            strictTemplates,
            strictInjectionParameters: false
        }
    });
    await write(path.join(root, 'tsconfig.lib.json'), {
        extends: './tsconfig.json',
        include: ['src/**/*.ts']
    });

    // A number bound to a string input: passes with strictTemplates off, fails with it on.
    const badBinding = withViolations ? '[label]="count"' : '[label]="title"';

    await write(
        path.join(root, 'src/child.component.ts'),
        `import { Component, Input } from '@angular/core';

@Component({
    selector: 'fx-child',
    standalone: true,
    template: '<span>{{ label }}</span>'
})
export class ChildComponent {
    @Input() label!: string;
}
`
    );

    await write(
        path.join(root, 'src/separate.component.ts'),
        `import { Component } from '@angular/core';
import { ChildComponent } from './child.component';

@Component({
    selector: 'fx-separate',
    standalone: true,
    imports: [ChildComponent],
    templateUrl: './separate.component.html'
})
export class SeparateComponent {
    title = 'hello';
    count = 42;
}
`
    );
    await write(path.join(root, 'src/separate.component.html'), `<fx-child ${badBinding}></fx-child>\n`);

    await write(
        path.join(root, 'src/inline.component.ts'),
        `import { Component } from '@angular/core';
import { ChildComponent } from './child.component';

@Component({
    selector: 'fx-inline',
    standalone: true,
    imports: [ChildComponent],
    template: '<fx-child ${badBinding}></fx-child>'
})
export class InlineComponent {
    title = 'hello';
    count = 7;
}
`
    );

    return {
        dir,
        root,
        separateTemplate: path.join(root, 'src/separate.component.html'),
        inlineComponent: path.join(root, 'src/inline.component.ts'),
        cleanup: () => fs.rm(dir, { recursive: true, force: true })
    };
}
