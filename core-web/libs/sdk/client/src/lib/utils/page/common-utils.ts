interface PageRequestParamsProps {
    path: string;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    params: { [key: string]: any } | undefined | URLSearchParams; // QueryParams are typed as `any` in frameworks
}

export const getPageRequestParams = ({
    path = '',
    params = {}
}: PageRequestParamsProps): {
    path: string;
    [key: string]: string | number;
} => {
    const copiedParams: PageRequestParamsProps['params'] =
        params instanceof URLSearchParams ? Object.fromEntries(params.entries()) : { ...params };

    const finalParams: Record<string, unknown> = {};
    const dotMarketingPersonaId = copiedParams['com.dotmarketing.persona.id'] || '';

    if (copiedParams['mode']) {
        finalParams['mode'] = copiedParams['mode'];
    }

    if (copiedParams['language_id']) {
        finalParams['language_id'] = copiedParams['language_id'];
    }

    if (copiedParams['variantName']) {
        finalParams['variantName'] = copiedParams['variantName'];
    }

    if (copiedParams['personaId'] || dotMarketingPersonaId) {
        finalParams['personaId'] = copiedParams['personaId'] || dotMarketingPersonaId;
    }

    return {
        path,
        ...finalParams
    };
};
