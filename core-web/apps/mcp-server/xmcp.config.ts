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
            // Output directly to the Nx workspace dist folder
            rspackConfig.output.path = process.cwd() + '/../../dist/apps/mcp-server';
        }
        return rspackConfig;
    }
};

export default config;
