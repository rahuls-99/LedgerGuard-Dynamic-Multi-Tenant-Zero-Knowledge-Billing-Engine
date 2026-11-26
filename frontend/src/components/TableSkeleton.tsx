import React from 'react';
import { Skeleton } from './Skeleton';

export interface TableSkeletonProps {
    rows?: number;
    columns?: number;
}

export const TableSkeleton: React.FC<TableSkeletonProps> = ({
    rows = 5,
    columns = 4,
}) => {
    const safeRows = Math.max(1, rows);
    const safeColumns = Math.max(1, columns);

    return (
        <tbody className="table-skeleton" aria-busy="true">
            {Array.from({ length: safeRows }).map((_, rowIndex) => (
                <tr key={`skeleton-row-${rowIndex}`} className="table-row">
                    {Array.from({ length: safeColumns }).map((__, colIndex) => (
                        <td
                            key={`skeleton-cell-${rowIndex}-${colIndex}`}
                            className="table-cell"
                        >
                            <Skeleton variant="line" height={16} width="85%" />
                        </td>
                    ))}
                </tr>
            ))}
        </tbody>
    );
};