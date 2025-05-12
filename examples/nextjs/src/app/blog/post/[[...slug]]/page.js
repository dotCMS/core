import { DetailPage } from "@/pages/DetailPage";
import { getPage } from "@/utils/getPage";

export async function generateMetadata({ params, searchParams }) {
    const path = params.slug?.join("/");
    const { pageAsset } = await getPage(`/blog/post/${path}`, searchParams);
    const urlContentMap = pageAsset?.urlContentMap;
    const title = urlContentMap?.title || "Page not found";
    return {
        title: `${title} - Blog`
    };
}

export default async function Home({ searchParams, params }) {
    const path = params?.slug?.join("/");
    const pageContent = await getPage(`/blog/post/${path}`, searchParams);
    return <DetailPage pageContent={pageContent} />;
}
