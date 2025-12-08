export class FaildToCreateFrontendProjectError extends Error {
    constructor(framework: string) {
        super(
            `Failed to create frontend project for framework: ${framework}. Please check if you have git installed and an active internet connection.`
        );
    }
}

export class DockerCliNotInstalledError extends Error {
    constructor() {
        super(`Please check if docker cli installed in you system!`);
    }
}

export class FailedToDownloadDockerComposeError extends Error {
    constructor() {
        super(`Failed to download the docker compose file`);
    }
}

export class FailedToGetDotcmsTokenError extends Error {
    constructor() {
        super(`Failed to get Dotcms API token. Please make sure if dotcms container is running.`);
    }
}

export class FailedToSetUpUVEConfig extends Error {
    constructor() {
        super(`Failed to set up UVE configuration in Dotcms.`);
    }
}

export class FailedToGetDemoSiteIdentifierError extends Error {
    constructor() {
        super(`Failed to get demo site identifier from Dotcms.`);
    }
}
