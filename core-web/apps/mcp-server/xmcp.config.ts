import { BannerPlugin } from 'rspack';
import { type XmcpConfig } from 'xmcp';

/**
 * xmcp 1.x aliases are either a map or a list of `{ name, alias }` pairs.
 * Replacing the map drops the aliases the compiler adds for `zod`, `node:`
 * builtins and its `.xmcp` runtime shims, and the bundle then fails to resolve
 * them. Append ours on top of whichever shape arrived.
 *
 * This file is compiled by xmcp in a sandbox that can import nothing but
 * `rspack`, so the paths below are plain strings.
 */
function withAliases(
    existing: unknown,
    extra: Record<string, string>
): Record<string, unknown> | Array<Record<string, unknown>> {
    if (Array.isArray(existing)) {
        return [...existing, ...Object.entries(extra).map(([name, alias]) => ({ name, alias }))];
    }

    const record =
        existing && typeof existing === 'object'
            ? { ...(existing as Record<string, unknown>) }
            : {};

    return { ...record, ...extra };
}

// `xmcp build` runs with this app as the working directory (see project.json).
const WORKSPACE_ROOT = process.cwd() + '/../..';
const AI = WORKSPACE_ROOT + '/libs/sdk/ai/src';
// Zod is hoisted to the workspace root. xmcp only aliases it when
// `<cwd>/node_modules/zod` exists, which pnpm does not create for this app,
// so the bundle and the runtime would otherwise each get their own copy.
const ZOD = WORKSPACE_ROOT + '/node_modules/zod';

const config: XmcpConfig = {
    stdio: true,
    paths: {
        tools: './src/tools',
        prompts: false,
        // Resource modules stay off. Skill files are resources the skills
        // extension registers on its own, which is what declares `resources`.
        resources: false,
        skills: './src/skills'
    },
    bundler: (rspackConfig) => {
        // The published entry stays `dist/apps/mcp-server/stdio.js`. xmcp 1.x
        // still names the stdio entry `stdio` and emits `[name].js`, with the
        // shebang added by its BannerPlugin, so only the directory moves.
        if (rspackConfig.output) {
            rspackConfig.output.path = WORKSPACE_ROOT + '/dist/apps/mcp-server';
        }
        rspackConfig.resolve = rspackConfig.resolve ?? {};
        // xmcp 1.x runs dotenv on startup and logs "injected env … from .env" to
        // stderr even when the file is absent. On a stdio server that line is
        // protocol noise: the previous runtime was silent, and the smoke test
        // requires stderr to stay empty. Quiet is the default; DOTENV_CONFIG_QUIET
        // still wins when a caller sets it. The shebang has to stay the first
        // line, so it and the assignment share one banner. A second BannerPlugin
        // is prepended and pushes the shebang off line 1, which rspack then
        // refuses to parse.
        rspackConfig.plugins = (rspackConfig.plugins ?? []).filter(
            (plugin) => plugin?.constructor?.name !== 'BannerPlugin'
        );
        rspackConfig.plugins.push(
            new BannerPlugin({
                banner: '#!/usr/bin/env node\nif(process.env.DOTENV_CONFIG_QUIET==null)process.env.DOTENV_CONFIG_QUIET="true";',
                raw: true,
                include: /^stdio\.js$/
            })
        );
        rspackConfig.resolve.alias = withAliases(rspackConfig.resolve.alias, {
            zod: ZOD,
            'zod/v3': ZOD + '/v3',
            'zod/v4-mini': ZOD + '/v4-mini',
            // The front door lives at the /runtime subpath; @dotcms/ai is a pure namespace.
            // xmcp reads paths only from this app's tsconfig, which extends the base
            // config and does not repeat them, so the aliases have to be set here.
            '@dotcms/ai/runtime': AI + '/runtime.ts',
            '@dotcms/ai/sandbox': AI + '/sandbox/index.ts',
            '@dotcms/ai/adapter': AI + '/adapter/index.ts',
            '@dotcms/ai/spec': AI + '/spec/index.ts',
            '@dotcms/ai/tools': AI + '/tools/index.ts'
        });
        return rspackConfig;
    }
};

export default config;
