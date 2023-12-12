function ActionButton({ message, children }) {
    return (
        <button
            style={{
                border: 0,
                backgroundColor: 'lightgray',
                color: 'black',
                border: 'solid 1px',
                padding: '0.25rem 0.5rem',
                marginBottom: '0.5rem'
            }}
            onClick={() => {
                window.parent.postMessage(message, '*');
            }}>
            {children || message.action}
        </button>
    );
}

export default ActionButton;
