/** @type {import('tailwindcss').Config} */
module.exports = {
    content: [
        "./src/components/**/*.{js,ts,jsx,tsx,mdx}",
        "./src/app/**/*.{js,ts,jsx,tsx,mdx}",
    ],
    plugins: [],
    safelist: [
        'container'
    ],
    theme: {
        container: {
            center: true,
            padding: '2rem',
        },
    },
};
