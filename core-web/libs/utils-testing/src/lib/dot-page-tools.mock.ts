import { DotPageTools } from '@dotcms/dotcms-models';

export const mockPageTools: DotPageTools = {
    pageTools: [
        {
            icon: 'assets/seo/wave.png',
            title: 'Wave',
            description:
                'The WAVE® evaluation suite helps educate authors on how to make their web content more accessible to individuals with disabilities. WAVE can identify many accessibility and Web Content Accessibility Guideline (WCAG) errors, but also facilitates human evaluation of web content.',
            tags: ['Accessibility', 'WCAG'],
            runnableLink:
                'https://wave.webaim.org/report#/http://localhost/blogTest?host_id=123?language_id=1'
        },
        {
            icon: 'assets/seo/mozilla.png',
            title: 'Mozilla Observatory',
            description:
                'The Mozilla Observatory has helped hundreds of thousands of websites by teaching developers, system administrators, and security professionals how to configure their sites safely and securely. ',
            tags: ['Security', 'Best Practices'],
            runnableLink: 'https://developer.mozilla.org/en-US/observatory/analyze?host=localhost'
        },
        {
            icon: 'assets/seo/security-headers.png',
            title: 'Security Headers',
            description:
                'This tool is designed to help you better deploy and understand modern security features that are available for your website. It will provide a simple to understand grading system for how well your site follows best practices, as well as suggestions for how to make improvement.',
            tags: ['Securty', 'Best Practices'],
            runnableLink:
                'https://securityheaders.com/?q=http://localhost/blogTest&host_id=123&language_id=1&followRedirects=on'
        }
    ]
};
