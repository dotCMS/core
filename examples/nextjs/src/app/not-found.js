import { ErrorLayout } from "@/components/ErrorLayout";

export default function NotFound() {
    return (
        <ErrorLayout
            status={404}
            heading="Something's missing."
            body="Sorry, we can't find that page. You'll find lots to explore on the home page."
        />
    );
}
