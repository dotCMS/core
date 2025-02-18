import { ErrorMessage } from './components/ErrorMessage';

import { DotCMSPageContext, DotCMSPageRendererMode } from '../../contexts/DotCMSPageContext';
import { DotCMSContentlet, DotCMSPageAsset } from '../../types';
import { Row } from '../Row/Row';

interface DotCMSLayoutBodyProps {
    page: DotCMSPageAsset;
    components: Record<string, React.ComponentType<DotCMSContentlet>>;
    mode?: DotCMSPageRendererMode;
}

/**
 * DotCMSLayoutBody component renders the layout body for a DotCMS page.
 *
 * It utilizes the page asset's layout body to render rows using the Row component.
 * If the layout body does not exist, it renders an error message.
 * It also provides context (DotCMSPageContext) with the page asset, optional user components,
 * and the renderer mode to its children.
 *
 * @public
 * @component
 * @param {Object} props - Component properties.
 * @param {DotCMSPageAsset} props.page - The DotCMS page asset containing the layout information.
 * @param {Record<string, React.ComponentType<DotCMSContentlet>>} [props.components] - Optional mapping of custom components for content rendering.
 * @param {DotCMSPageRendererMode} [props.mode='production'] - The renderer mode; defaults to 'production'. Alternate modes might trigger different behaviors.
 *
 * @returns {JSX.Element} The rendered DotCMS page body or an error message if the layout body is missing.
 *
 * -------------------------------------------------------------------
 *
 * El componente DotCMSLayoutBody renderiza el cuerpo del layout para una página de DotCMS.
 *
 * Utiliza el "body" del layout del asset de la página para renderizar las filas mediante el componente Row.
 * Si el "body" del layout no está presente, renderiza un mensaje de error.
 * También provee un contexto (DotCMSPageContext) con el asset de la página, componentes de usuario opcionales,
 * y el modo del renderizado para sus componentes hijos.
 */
export const DotCMSLayoutBody = ({
    page,
    components = {},
    mode = 'production'
}: DotCMSLayoutBodyProps) => {
    const dotCMSPageBody = page?.layout?.body;

    if (!dotCMSPageBody) {
        return <ErrorMessage mode={mode} />;
    }

    const contextValue = {
        pageAsset: page,
        userComponents: components,
        mode
    };

    return (
        <DotCMSPageContext.Provider value={contextValue}>
            {dotCMSPageBody.rows.map((row, index) => (
                <Row key={index} row={row} />
            ))}
        </DotCMSPageContext.Provider>
    );
};
