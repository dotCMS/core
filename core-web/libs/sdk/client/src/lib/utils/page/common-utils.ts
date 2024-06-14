interface PageRequestParamsProps {
    path: string;
    params: Record<string, string>;
}

interface PageRequestParams {
    path: string;
    mode: string;
    language_id: string;
    variantName: string;
    'com.dotmarketing.persona.id': string;
}

export const getPageRequestParams = ({
    path = '',
    params = {}
}: PageRequestParamsProps): PageRequestParams => {
    const personaId = params?.['com.dotmarketing.persona.id'] || '';
    const { language_id = '', mode = '', variantName = '' } = params;

    return {
        path,
        mode,
        language_id,
        variantName,
        'com.dotmarketing.persona.id': personaId
    };
};
