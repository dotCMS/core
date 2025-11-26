import "./globals.css";

export const metadata = {
    title: "DotCMS",
    description: "Enterprise Headless CMS Platform",
};

export default function RootLayout({ children }) {
    return (
        <html lang="en">
            <body>
                <main>{children}</main>
            </body>
        </html>
    );
}
