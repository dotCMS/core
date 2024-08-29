interface PageRequestParamsProps {
    path: string;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    params: { [key: string]: any } | undefined; // QueryParams are typed as `any` in frameworks
}

export const getPageRequestParams = ({ path = '', params = {} }: PageRequestParamsProps) => {
    const finalParams: Record<string, unknown> = {};
    const dotMarketingPersonaId = params['com.dotmarketing.persona.id'] || '';

    if (params['mode']) {
        finalParams['mode'] = params['mode'];
    }

    if (params['language_id']) {
        finalParams['language_id'] = params['language_id'];
    }

    if (params['variantName']) {
        finalParams['variantName'] = params['variantName'];
    }

    if (params['personaId'] || dotMarketingPersonaId) {
        finalParams['personaId'] = params['personaId'] || dotMarketingPersonaId;
    }

    return {
        path,
        ...finalParams
    };
};
