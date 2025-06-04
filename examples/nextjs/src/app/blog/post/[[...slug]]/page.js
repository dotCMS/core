import NotFound from "@/app/not-found";
import { DetailPage } from "@/views/DetailPage";
import { getDotCMSPage } from "@/utils/getDotCMSPage";

export async function generateMetadata(props) {
    const searchParams = await props.searchParams;
    const params = await props.params;
    try {
        const path = params.slug[0];
        const { pageAsset } = await getDotCMSPage(
            `/blog/post/${path}`,
            searchParams,
        );
        const urlContentMap = pageAsset?.urlContentMap;
        const title = urlContentMap?.title || "Page not found";
        return {
            title: `${title} - Blog`,
        };
    } catch (e) {
        return {
            title: "not found",
        };
    }
}

export default async function Home(props) {
    const params = await props.params;
    const searchParams = await props.searchParams;
    const path = params.slug[0];
    const pageContent = await getDotCMSPage(`/blog/post/${path}`, searchParams);

    if (!pageContent) {
        return <NotFound />;
    }

    return <DetailPage pageContent={pageContent} />;
}
