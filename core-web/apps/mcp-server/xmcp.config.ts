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
            // The front door lives at the /runtime subpath; @dotcms/ai is a pure namespace.
            '@dotcms/ai/runtime': process.cwd() + '/../../libs/sdk/ai/src/runtime.ts',
            '@dotcms/ai/sandbox': process.cwd() + '/../../libs/sdk/ai/src/sandbox/index.ts',
            '@dotcms/ai/adapter': process.cwd() + '/../../libs/sdk/ai/src/adapter/index.ts',
            '@dotcms/ai/spec': process.cwd() + '/../../libs/sdk/ai/src/spec/index.ts'
        };
        return rspackConfig;
    }
};

export default config;
