
import React from 'react';
import { TableSkeleton } from './TableSkeleton';
import './table.css';

export interface TableColumn<T> {
    key: keyof T | string;
    header: string;
    render?: (row: T) => React.ReactNode;
    align?: 'left' | 'center' | 'right';
}

export interface TableProps<T> {
    columns: TableColumn<T>[];
    data: T[];
    rowKey: (row: T) => string | number;
    isLoading?: boolean;
    error?: string | null;
    skeletonRows?: number;
    emptyMessage?: string;
    onRowClick?: (row: T) => void;
}

export function Table<T>({
    columns,
    data,
    rowKey,
    isLoading = false,
    error = null,
    skeletonRows = 5,
    emptyMessage = 'No records found.',
    onRowClick,
}: TableProps<T>): React.ReactElement {

    return (
        <div className="table-wrapper">
            <table className="data-table">
                <thead>
                    <tr>
                        {columns.map((col) => (
                            <th
                                key={String(col.key)}
                                className="data-table__th"
                                scope="col"
                                style={{ textAlign: col.align ?? 'left' }}
                            >
                                {col.header}
                            </th>
                        ))}
                    </tr>
                </thead>

                {isLoading ? (
                    <TableSkeleton
                        rows={skeletonRows}
                        columns={columns.length}
                    />
                ) : error ? (
                    <tbody>
                        <tr>
                            <td
                                colSpan={columns.length}
                                className="data-table__empty"
                                role="alert"
                            >
                                {error}
                            </td>
                        </tr>
                    </tbody>
                ) : data.length === 0 ? (
                    <tbody>
                        <tr>
                            <td
                                colSpan={columns.length}
                                className="data-table__empty"
                            >
                                {emptyMessage}
                            </td>
                        </tr>
                    </tbody>
                ) : (
                    <tbody>
                        {data.map((row) => (
                            <tr
                                key={rowKey(row)}
                                className={`data-table__tr ${onRowClick
                                    ? 'data-table__tr--clickable'
                                    : ''
                                    }`}
                                onClick={
                                    onRowClick
                                        ? () => onRowClick(row)
                                        : undefined
                                }
                                tabIndex={onRowClick ? 0 : undefined}
                                onKeyDown={
                                    onRowClick
                                        ? (event) => {
                                            if (
                                                event.key === 'Enter' ||
                                                event.key === ' '
                                            ) {
                                                event.preventDefault();
                                                onRowClick(row);
                                            }
                                        }
                                        : undefined
                                }
                            >
                                {columns.map((col) => (
                                    <td
                                        key={String(col.key)}
                                        className="data-table__td"
                                        style={{
                                            textAlign: col.align ?? 'left',
                                        }}
                                    >
                                        {col.render
                                            ? col.render(row)
                                            : String(row[col.key as keyof T] ?? '')}
                                    </td>
                                ))}
                            </tr>
                        ))}
                    </tbody>
                )}
            </table>
        </div>
    );
}
