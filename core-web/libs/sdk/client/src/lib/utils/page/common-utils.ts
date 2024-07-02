interface PageRequestParamsProps {
    path: string;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    params: { [key: string]: any } | undefined; // QueryParams are typed as `any` in frameworks
}

export const getPageRequestParams = ({ path = '', params = {} }: PageRequestParamsProps) => {
    const dotMarketingPersonaId = params?.['com.dotmarketing.persona.id'] || '';
    const { language_id = 1, mode = '', variantName = '', personaId } = params;

    return {
        path,
        mode,
        language_id,
        variantName,
        personaId: personaId || dotMarketingPersonaId
    };
};
