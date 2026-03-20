import '@testing-library/jest-dom';
import { render, screen } from '@testing-library/react';

import { BlockEditorMark } from '@dotcms/types';

import {
    Bold,
    Italic,
    Strike,
    Underline,
    Superscript,
    Subscript,
    Link,
    Heading,
    Paragraph,
    TextBlock
} from '../../components/DotCMSBlockEditorRenderer/components/blocks/Texts';

describe('Texts components', () => {
    describe('TextBlock', () => {
        it('should render plain text when no marks provided', () => {
            const { container } = render(
                <TextBlock text="Hello World" marks={[]} />
            );
            expect(container.textContent).toBe('Hello World');
        });

        it('should render bold text', () => {
            const { container } = render(
                <TextBlock text="Bold Text" marks={[{ type: 'bold', attrs: {} }]} />
            );
            expect(container.querySelector('strong')).toBeInTheDocument();
            expect(container.textContent).toBe('Bold Text');
        });

        it('should render italic text', () => {
            const { container } = render(
                <TextBlock text="Italic Text" marks={[{ type: 'italic', attrs: {} }]} />
            );
            expect(container.querySelector('em')).toBeInTheDocument();
        });

        it('should render strike-through text', () => {
            const { container } = render(
                <TextBlock text="Strike Text" marks={[{ type: 'strike', attrs: {} }]} />
            );
            expect(container.querySelector('s')).toBeInTheDocument();
        });

        it('should render underlined text', () => {
            const { container } = render(
                <TextBlock text="Underline Text" marks={[{ type: 'underline', attrs: {} }]} />
            );
            expect(container.querySelector('u')).toBeInTheDocument();
        });

        it('should render nested marks (bold inside link)', () => {
            const marks: BlockEditorMark[] = [
                { type: 'link', attrs: { href: 'https://example.com' } },
                { type: 'bold', attrs: {} }
            ];
            const { container } = render(<TextBlock text="Linked Bold" marks={marks} />);
            const link = container.querySelector('a');
            expect(link).toBeInTheDocument();
            expect(container.querySelector('strong')).toBeInTheDocument();
        });

        describe('class → className conversion', () => {
            it('should convert class attribute to className without mutating original attrs', () => {
                const originalAttrs = { class: 'my-class', href: 'https://example.com' };
                const mark: BlockEditorMark = { type: 'link', attrs: { ...originalAttrs } };

                render(<TextBlock text="Link" marks={[mark]} />);

                // Original mark.attrs should NOT be mutated
                expect(mark.attrs).toEqual(originalAttrs);
                expect(mark.attrs?.class).toBe('my-class');
                expect(mark.attrs?.className).toBeUndefined();
            });

            it('should pass className to the rendered component', () => {
                const mark: BlockEditorMark = {
                    type: 'link',
                    attrs: { class: 'custom-class', href: 'https://example.com' }
                };

                const { container } = render(<TextBlock text="Link" marks={[mark]} />);
                const link = container.querySelector('a');
                // The component should receive className (not class)
                expect(link).toBeInTheDocument();
            });

            it('should not add className when class is not present', () => {
                const mark: BlockEditorMark = {
                    type: 'link',
                    attrs: { href: 'https://example.com' }
                };
                const originalAttrs = { ...mark.attrs };

                render(<TextBlock text="Link" marks={[mark]} />);

                // Original attrs should not be mutated
                expect(mark.attrs).toEqual(originalAttrs);
            });

            it('should handle marks without attrs', () => {
                const mark: BlockEditorMark = { type: 'bold' };
                const { container } = render(<TextBlock text="Bold" marks={[mark]} />);
                expect(container.querySelector('strong')).toBeInTheDocument();
            });
        });

        it('should return undefined text when no text provided', () => {
            const { container } = render(
                <TextBlock marks={[{ type: 'unknown-type', attrs: {} }]} />
            );
            expect(container.textContent).toBe('');
        });
    });

    describe('Bold', () => {
        it('should render children in strong tag', () => {
            render(<Bold type="bold" attrs={{}}>Bold text</Bold>);
            expect(screen.getByText('Bold text').tagName).toBe('STRONG');
        });
    });

    describe('Italic', () => {
        it('should render children in em tag', () => {
            render(<Italic type="italic" attrs={{}}>Italic text</Italic>);
            expect(screen.getByText('Italic text').tagName).toBe('EM');
        });
    });

    describe('Strike', () => {
        it('should render children in s tag', () => {
            render(<Strike type="strike" attrs={{}}>Strike text</Strike>);
            expect(screen.getByText('Strike text').tagName).toBe('S');
        });
    });

    describe('Underline', () => {
        it('should render children in u tag', () => {
            render(<Underline type="underline" attrs={{}}>Underline text</Underline>);
            expect(screen.getByText('Underline text').tagName).toBe('U');
        });
    });

    describe('Superscript', () => {
        it('should render children in sup tag', () => {
            render(<Superscript type="superscript" attrs={{}}>Sup text</Superscript>);
            expect(screen.getByText('Sup text').tagName).toBe('SUP');
        });
    });

    describe('Subscript', () => {
        it('should render children in sub tag', () => {
            render(<Subscript type="subscript" attrs={{}}>Sub text</Subscript>);
            expect(screen.getByText('Sub text').tagName).toBe('SUB');
        });
    });

    describe('Link', () => {
        it('should render children in a tag with attrs', () => {
            render(
                <Link type="link" attrs={{ href: 'https://example.com', target: '_blank' }}>
                    Click me
                </Link>
            );
            const link = screen.getByText('Click me');
            expect(link.tagName).toBe('A');
            expect(link).toHaveAttribute('href', 'https://example.com');
            expect(link).toHaveAttribute('target', '_blank');
        });
    });

    describe('Heading', () => {
        it('should render h1 by default', () => {
            const { container } = render(
                <Heading node={{ type: 'heading', attrs: { level: 1 } }}>Heading 1</Heading>
            );
            expect(container.querySelector('h1')).toBeInTheDocument();
        });

        it('should render correct heading level', () => {
            const { container } = render(
                <Heading node={{ type: 'heading', attrs: { level: 3 } }}>Heading 3</Heading>
            );
            expect(container.querySelector('h3')).toBeInTheDocument();
        });
    });

    describe('Paragraph', () => {
        it('should render children in p tag', () => {
            render(<Paragraph node={{ type: 'paragraph', attrs: {} }}>Paragraph text</Paragraph>);
            expect(screen.getByText('Paragraph text').tagName).toBe('P');
        });
    });
});
