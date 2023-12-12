import Column from './column';

function Row({ row }) {
    return (
        <div className="grid grid-cols-12 gap-4">
            {row.columns.map((column, index) => (
                <Column key={index} column={column} />
            ))}
        </div>
    );
}

export default Row;
