/* eslint-disable @typescript-eslint/no-explicit-any */
export const DotContent = (props: any) => {
    const { data, customRenderers } = props;
    const DefaultContent = () => <div>Unknown Content Type</div>;

    const Component = customRenderers?.[data.contentType] ?? DefaultContent;

    return <Component {...data} />;
};
