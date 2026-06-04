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
            '@dotcms/agentic-tools': process.cwd() + '/../../libs/agentic-tools/src/index.ts'
        };
        return rspackConfig;
    }
};

export default config;
