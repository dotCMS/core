import { type XmcpConfig } from 'xmcp';

const config: XmcpConfig = {
    stdio: true,
    paths: {
        tools: './src/tools',
        prompts: false,
        resources: false
    },
    bundler: (rspackConfig) => {
        if (rspackConfig.output) {
            rspackConfig.output.path = process.cwd() + '/../../dist/apps/mcp-server';
        }
        rspackConfig.resolve = rspackConfig.resolve || {};
        rspackConfig.resolve.alias = {
            // `$` forces an exact match so the bare entry never shadows the subpaths below.
            '@dotcms/ai$': process.cwd() + '/../../libs/sdk/ai/src/index.ts',
            '@dotcms/ai/sandbox': process.cwd() + '/../../libs/sdk/ai/src/sandbox/index.ts',
            '@dotcms/ai/adapter': process.cwd() + '/../../libs/sdk/ai/src/adapter/index.ts',
            '@dotcms/ai/spec': process.cwd() + '/../../libs/sdk/ai/src/spec/index.ts'
        };
        return rspackConfig;
    }
};

export default config;
