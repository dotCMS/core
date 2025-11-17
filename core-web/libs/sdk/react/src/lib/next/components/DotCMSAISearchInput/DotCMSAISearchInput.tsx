import { useRef, useState } from 'react';

import { useDotCMSAISearchContext } from '../../contexts/DotCMSAISearchContext';
import { DotCMSAISearchInputProps } from '../../shared/types';

/**
 * AI Search Input component with default UI or render prop pattern.
 *
 * @example Default usage with built-in input
 * ```tsx
 * <DotCMSAISearchInput
 *   placeholder="Search with AI..."
 *   onSearch={(prompt) => console.log('Searching:', prompt)}
 * />
 * ```
 *
 * @example Custom implementation with render props
 * ```tsx
 * <DotCMSAISearchInput>
 *   {({ search, reset }) => {
 *     const [value, setValue] = useState('');
 *     return (
 *       <div>
 *         <input
 *           value={value}
 *           onChange={(e) => setValue(e.target.value)}
 *           placeholder="Custom search..."
 *         />
 *         <button onClick={() => search(value)}>Search</button>
 *         <button onClick={() => { reset(); setValue(''); }}>Clear</button>
 *       </div>
 *     );
 *   }}
 * </DotCMSAISearchInput>
 * ```
 *
 * @category Components
 */
export function DotCMSAISearchInput({
    children,
    onSearch,
    onReset,
    placeholder = 'Search...',
    className = ''
}: DotCMSAISearchInputProps) {
    const { search, reset } = useDotCMSAISearchContext();
    const inputRef = useRef<HTMLInputElement>(null);
    const [hasValue, setHasValue] = useState(false);

    // If render prop is provided, use it
    if (children) {
        return <>{children({ search, reset })}</>;
    }

    // Default implementation
    const handleSearch = () => {
        const prompt = inputRef.current?.value?.trim();
        if (prompt) {
            search(prompt);
            onSearch?.(prompt);
        }
    };

    const handleReset = () => {
        if (inputRef.current) {
            inputRef.current.value = '';
            setHasValue(false);
        }
        reset();
        onReset?.();
    };

    const handleInputChange = () => {
        setHasValue(!!inputRef.current?.value);
    };

    const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
        if (e.key === 'Enter') {
            e.preventDefault();
            handleSearch();
        }
    };

    return (
        <div className={className} style={{ display: 'flex', gap: '12px', alignItems: 'center' }}>
            <div style={{ position: 'relative', flex: 1 }}>
                <input
                    ref={inputRef}
                    type="text"
                    placeholder={placeholder}
                    onKeyDown={handleKeyDown}
                    onChange={handleInputChange}
                    style={{
                        width: '100%',
                        padding: '16px 48px 16px 20px',
                        fontSize: '16px',
                        border: '2px solid #ddd',
                        borderRadius: '12px',
                        outline: 'none',
                        transition: 'border-color 0.2s'
                    }}
                    onFocus={(e) => (e.target.style.borderColor = '#999')}
                    onBlur={(e) => (e.target.style.borderColor = '#ddd')}
                />
                {hasValue && (
                    <button
                        onClick={handleReset}
                        style={{
                            position: 'absolute',
                            right: '16px',
                            top: '50%',
                            transform: 'translateY(-50%)',
                            background: 'none',
                            border: 'none',
                            cursor: 'pointer',
                            fontSize: '20px',
                            color: '#666',
                            padding: '4px',
                            lineHeight: 1
                        }}
                        aria-label="Clear search">
                        ✕
                    </button>
                )}
            </div>
            <button
                onClick={handleSearch}
                style={{
                    padding: '16px 32px',
                    fontSize: '16px',
                    fontWeight: 500,
                    color: 'white',
                    background: '#9333ea',
                    border: 'none',
                    borderRadius: '12px',
                    cursor: 'pointer',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '8px',
                    transition: 'background-color 0.2s',
                    whiteSpace: 'nowrap'
                }}
                onMouseEnter={(e) => (e.currentTarget.style.background = '#7e22ce')}
                onMouseLeave={(e) => (e.currentTarget.style.background = '#9333ea')}>
                <span style={{ fontSize: '18px' }} role="img" aria-label="search icon">
                    🔍
                </span>
                Search
            </button>
        </div>
    );
}
