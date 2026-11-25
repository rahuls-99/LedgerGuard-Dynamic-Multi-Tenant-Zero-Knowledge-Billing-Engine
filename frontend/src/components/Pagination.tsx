
import React from 'react';
import './Pagination.css';

export interface PaginationProps {
    page: number;
    totalPages: number;
    totalElements: number;
    size: number;
    onPageChange: (nextPage: number) => void;
    onSizeChange: (nextSize: number) => void;
    disabled?: boolean;
}

export const Pagination: React.FC<PaginationProps> = ({
    page,
    totalPages,
    totalElements,
    size,
    onPageChange,
    onSizeChange,
    disabled = false,
}) => {
    if (totalPages <= 0) {
        return null;
    }

    const currentPage = Math.max(0, Math.min(page, totalPages - 1));

    const isFirstPage = currentPage === 0;
    const isLastPage = currentPage === totalPages - 1;

    const startItem =
        totalElements > 0 ? currentPage * size + 1 : 0;

    const endItem =
        Math.min((currentPage + 1) * size, totalElements);

    // Generate page numbers and ellipsis
    const getPageNumbers = (): (number | string)[] => {
        if (totalPages <= 7) {
            return Array.from(
                { length: totalPages },
                (_, index) => index
            );
        }

        if (currentPage <= 3) {
            return [0, 1, 2, 3, 4, '...', totalPages - 1];
        }

        if (currentPage >= totalPages - 4) {
            return [
                0,
                '...',
                ...Array.from(
                    { length: 5 },
                    (_, index) => totalPages - 5 + index
                ),
            ];
        }

        return [
            0,
            '...',
            currentPage - 1,
            currentPage,
            currentPage + 1,
            '...',
            totalPages - 1,
        ];
    };

    const pageNumbers = getPageNumbers();

    return (
        <nav
            className="pagination"
            aria-label="Pagination navigation"
        >
            <div className="pagination__info">
                Showing <strong>{startItem}</strong>–
                <strong>{endItem}</strong> of{' '}
                <strong>{totalElements}</strong>
            </div>

            <div className="pagination__controls">

                <button
                    type="button"
                    className="pagination__btn"
                    disabled={disabled || isFirstPage}
                    onClick={() => onPageChange(currentPage - 1)}
                    aria-label="Previous page"
                >
                    Previous
                </button>

                {pageNumbers.map((pageNumber, index) => (
                    typeof pageNumber === 'number' ? (
                        <button
                            key={pageNumber}
                            type="button"
                            className={`pagination__btn ${currentPage === pageNumber
                                ? 'pagination__btn--active'
                                : ''
                                }`}
                            disabled={disabled}
                            onClick={() => onPageChange(pageNumber)}
                            aria-label={`Go to page ${pageNumber + 1}`}
                            aria-current={
                                currentPage === pageNumber
                                    ? 'page'
                                    : undefined
                            }
                        >
                            {pageNumber + 1}
                        </button>
                    ) : (
                        <span
                            key={`ellipsis-${index}`}
                            className="pagination__ellipsis"
                            aria-hidden="true"
                        >
                            …
                        </span>
                    )
                ))}

                <button
                    type="button"
                    className="pagination__btn"
                    disabled={disabled || isLastPage}
                    onClick={() => onPageChange(currentPage + 1)}
                    aria-label="Next page"
                >
                    Next
                </button>
            </div>

            <div className="pagination__size">
                <label htmlFor="page-size">
                    Rows per page:
                </label>

                <select
                    id="page-size"
                    value={size}
                    disabled={disabled}
                    onChange={(e) =>
                        onSizeChange(Number(e.target.value))
                    }
                >
                    <option value={10}>10</option>
                    <option value={20}>20</option>
                    <option value={50}>50</option>
                </select>
            </div>
        </nav>
    );
};
