
import React, { useMemo, useState } from 'react';

import {
    StatusBadge,
    StatusChip,
    type BadgeStatus,
} from './StatusBadge';

import { StatusFilterChip } from './StatusFilterChip';
import { TransactionList } from './TransactionList';
import type { Transaction } from '../types/transaction';

import { Skeleton } from './Skeleton';
import { StatCardSkeleton } from './StatCardSkeleton';
import { Table, type TableColumn } from './Table';
import { Pagination } from './Pagination';

import { EmptyState } from './EmptyState';
import { ErrorState } from './ErrorState';
import { InlineError, FieldError } from './InlineError';
import { ErrorBoundaryFallback } from './ErrorBoundaryFallback';
import { Modal } from './Modal';
import { Toast } from './Toast';
import { Card } from './Card';
import { useToast } from './Toast/useToast';
import TransactionResultCard from './TransactionResultCard/TransactionResultCard';

import StatCardGrid from './StatCardGrid';
import type { DashboardStats } from '../types/dashboard';

interface BillingEntry {
    id: string;
    tenant: string;
    amount: string;
    status: BadgeStatus;
    createdAt: string;
}

interface ComponentsDemoPageProps {
    onLogout: () => void;
}

const SAMPLE_ENTRIES: BillingEntry[] = Array.from(
    { length: 57 },
    (_, index) => {
        const statuses: BadgeStatus[] = [
            'SUCCESS',
            'PENDING',
            'FAILED',
            'ROLLED_BACK',
        ];

        const tenants = [
            'Acme Cloud',
            'Northstar Labs',
            'Vertex Systems',
            'Helios Pay',
            'Kinetix AI',
            'Stratos Data',
            'Orbit Commerce',
        ];

        return {
            id: `INV-${1001 + index}`,
            tenant: tenants[index % tenants.length],
            amount: `$${(500 + index * 125).toLocaleString('en-US')}.00`,
            status: statuses[index % statuses.length],
            createdAt: `2026-11-${String((index % 21) + 1).padStart(2, '0')}`,
        };
    }
);

// DAY 8: SAMPLE TRANSACTIONS
const SAMPLE_TRANSACTIONS: Transaction[] = [
    {
        transactionId: 'TXN-2026-10001-ABCDEF',
        type: 'PAYMENT',
        amount: '2500.00',
        currency: 'INR',
        status: 'SUCCESS',
        createdAt: '2026-11-22T10:30:00Z',
        failureReason: null,
    },
    {
        transactionId: 'TXN-2026-10002-GHIJKL',
        type: 'REFUND',
        amount: '0.00',
        currency: 'INR',
        status: 'PENDING',
        createdAt: '2026-11-22T11:30:00Z',
        failureReason: null,
    },
    {
        transactionId: 'TXN-2026-10003-MNOPQR',
        type: 'PAYMENT',
        amount: '1500.00',
        currency: 'INR',
        status: 'FAILED',
        createdAt: '2026-11-22T12:30:00Z',
        failureReason:
            'Payment failed because the bank authorization was declined. This is a deliberately long message to test how the transaction details panel handles long failure reasons on mobile screens.',
    },
    {
        transactionId: 'TXN-2026-10004-STUVWX',
        type: 'REFUND',
        amount: '999999999999999999.99',
        currency: 'INR',
        status: 'ROLLED_BACK',
        createdAt: '2026-11-22T13:30:00Z',
        failureReason: null,
    },
    {
        transactionId: 'TXN-2026-10005-UNKNOWN',
        type: 'PAYMENT',
        amount: '500.00',
        currency: 'INR',
        status: 'PROCESSING',
        createdAt: '2026-11-22T14:30:00Z',
        failureReason: null,
    },
];

export const ComponentsDemoPage: React.FC<ComponentsDemoPageProps> = ({
    onLogout,
}) => {
    const toast = useToast();
    const [page, setPage] = useState(0);
    const [size, setSize] = useState(10);
    const [isTableLoading, setIsTableLoading] = useState(false);
    const [showEmpty, setShowEmpty] = useState(false);

    const [isModalOpen, setIsModalOpen] = useState(false);
    const [showToast, setShowToast] = useState(false);
    const [showFallback, setShowFallback] = useState(false);
    const [showInlineError, setShowInlineError] = useState(true);

    const [selectedStatuses, setSelectedStatuses] = useState<string[]>([
        'SUCCESS',
    ]);

    const [stats, setStats] = useState<DashboardStats | null>({
        totalUsers: 2,
        totalTransactions: 5,
        totalWalletBalance: '15250.00',
        currency: 'INR',
        lastTransactionAt: '2026-11-22T10:30:00Z',
    });

    const [isStatsLoading, setIsStatsLoading] = useState(false);
    const [statsError, setStatsError] = useState<string | null>(null);
    const [showStatsEmpty, setShowStatsEmpty] = useState(false);

    const activeRows = useMemo<BillingEntry[]>(() => {
        if (showEmpty) return [];

        const start = page * size;

        return SAMPLE_ENTRIES.slice(start, start + size);
    }, [page, size, showEmpty]);

    const totalPages = showEmpty
        ? 1
        : Math.ceil(SAMPLE_ENTRIES.length / size);

    const columns: TableColumn<BillingEntry>[] = [
        {
            key: 'id',
            header: 'Invoice ID',
            render: (row) => <code>{row.id}</code>,
        },
        {
            key: 'tenant',
            header: 'Tenant',
            render: (row) => row.tenant,
        },
        {
            key: 'amount',
            header: 'Amount',
            align: 'right',
            render: (row) => row.amount,
        },
        {
            key: 'status',
            header: 'Status',
            render: (row) => <StatusBadge status={row.status} />,
        },
        {
            key: 'createdAt',
            header: 'Date',
            render: (row) => row.createdAt,
        },
    ];

    const handleToggleEmpty = () => {
        setShowEmpty((prev) => {
            setPage(0);
            return !prev;
        });
    };

    const handleStatusToggle = (status: string) => {
        setSelectedStatuses((previous) =>
            previous.includes(status)
                ? previous.filter((item) => item !== status)
                : [...previous, status]
        );
    };

    const handleStatsLoading = () => {
        setStatsError(null);
        setShowStatsEmpty(false);
        setIsStatsLoading(true);

        window.setTimeout(() => {
            setIsStatsLoading(false);
        }, 1500);
    };

    const handleStatsError = () => {
        setIsStatsLoading(false);
        setShowStatsEmpty(false);
        setStatsError('Unable to connect to the dashboard statistics service.');
    };

    const handleStatsEmpty = () => {
        setIsStatsLoading(false);
        setStatsError(null);
        setShowStatsEmpty(true);
    };

    const handleStatsData = () => {
        setIsStatsLoading(false);
        setStatsError(null);
        setShowStatsEmpty(false);

        setStats({
            totalUsers: 2,
            totalTransactions: 5,
            totalWalletBalance: '15250.00',
            currency: 'INR',
            lastTransactionAt: '2026-11-22T10:30:00Z',
        });
    };

    const handleStatsRetry = () => {
        setStatsError(null);
        setIsStatsLoading(true);

        window.setTimeout(() => {
            setIsStatsLoading(false);
            setStatsError(null);
            setShowStatsEmpty(false);
        }, 1000);
    };

    if (showFallback) {
        return (
            <div style={{ padding: '24px' }}>
                <ErrorBoundaryFallback
                    message="This is a demonstration of the error boundary fallback."
                    onRetry={() => setShowFallback(false)}
                />
            </div>
        );
    }

    return (
        <section
            style={{
                width: '100%',
                maxWidth: '100%',
                padding: '24px',
                display: 'flex',
                flexDirection: 'column',
                gap: '28px',
                textAlign: 'left',
                boxSizing: 'border-box',
            }}
        >
            {/* PAGE HEADER */}
            <div
                style={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                    flexWrap: 'wrap',
                    gap: '12px',
                }}
            >
                <div>
                    <h2>LedgerGuard UI Components Demo</h2>
                    <p>
                        Day 3, Day 4, Day 5 and Day 8 frontend components demonstration.
                    </p>
                </div>

                <button type="button" onClick={onLogout}>
                    Logout
                </button>
            </div>

            {/* DAY 5: DASHBOARD STATISTICS */}
            <Card title="Dashboard Statistics - Day 5">
                <div
                    style={{
                        display: 'flex',
                        flexWrap: 'wrap',
                        gap: '10px',
                        marginBottom: '18px',
                    }}
                >
                    <button type="button" onClick={handleStatsData}>
                        Show Data
                    </button>

                    <button type="button" onClick={handleStatsLoading}>
                        Simulate Loading
                    </button>

                    <button type="button" onClick={handleStatsError}>
                        Simulate Error
                    </button>

                    <button type="button" onClick={handleStatsEmpty}>
                        Simulate Empty Data
                    </button>
                </div>

                <StatCardGrid
                    stats={showStatsEmpty ? null : stats}
                    loading={isStatsLoading}
                    error={statsError}
                    onRetry={handleStatsRetry}
                    traceId={statsError ? 'LG-STATS-1001' : undefined}
                />

                {showStatsEmpty && (
                    <p style={{ marginTop: '12px' }}>
                        No dashboard activity yet. All statistics are currently zero.
                    </p>
                )}
            </Card>

            {/* STATUS BADGES */}
            <Card title="Status Badges">
                <div style={{ display: 'flex', flexWrap: 'wrap', gap: '10px' }}>
                    <StatusBadge status="SUCCESS" />
                    <StatusBadge status="FAILED" />
                    <StatusBadge status="PENDING" />
                    <StatusBadge status="ROLLED_BACK" />
                </div>
            </Card>

            {/* DAY 8: STATUS CHIPS */}
            <Card title="Day 8: Transaction Status Chips">
                <div
                    style={{
                        display: 'flex',
                        flexWrap: 'wrap',
                        gap: '12px',
                        alignItems: 'center',
                    }}
                >
                    <StatusChip status="SUCCESS" size="sm" />
                    <StatusChip status="FAILED" size="md" />
                    <StatusChip status="PENDING" showIcon />
                    <StatusChip status="ROLLED_BACK" />
                    <StatusChip status="PROCESSING" />
                </div>
            </Card>

            {/* DAY 8: STATUS FILTER CHIPS */}
            <Card title="Day 8: Status Filter Chips">
                <p>Select or deselect transaction statuses:</p>

                <div
                    style={{
                        display: 'flex',
                        flexWrap: 'wrap',
                        gap: '10px',
                    }}
                >
                    <StatusFilterChip
                        status="SUCCESS"
                        selected={selectedStatuses.includes('SUCCESS')}
                        onToggle={handleStatusToggle}
                        count={1}
                    />

                    <StatusFilterChip
                        status="FAILED"
                        selected={selectedStatuses.includes('FAILED')}
                        onToggle={handleStatusToggle}
                        count={1}
                    />

                    <StatusFilterChip
                        status="PENDING"
                        selected={selectedStatuses.includes('PENDING')}
                        onToggle={handleStatusToggle}
                        count={1}
                    />

                    <StatusFilterChip
                        status="ROLLED_BACK"
                        selected={selectedStatuses.includes('ROLLED_BACK')}
                        onToggle={handleStatusToggle}
                        count={1}
                    />
                </div>

                <p style={{ marginTop: '12px' }}>
                    Selected statuses: {selectedStatuses.length > 0
                        ? selectedStatuses.join(', ')
                        : 'None'}
                </p>
            </Card>

            {/* DAY 8: TRANSACTION LIST */}
            <Card title="Day 8: Transaction History">
                <p>
                    Select a transaction row to expand its details.
                    Use the Copy button to copy its full transaction ID.
                </p>

                <TransactionList
                    transactions={SAMPLE_TRANSACTIONS}
                />
            </Card>

            {/* SKELETON COMPONENTS */}
            <Card title="Skeleton & Stat Card Skeleton">
                <div
                    style={{
                        display: 'grid',
                        gridTemplateColumns: 'repeat(auto-fit, minmax(min(100%, 220px), 1fr))',
                        gap: '14px',
                    }}
                >
                    <StatCardSkeleton showIcon />
                    <StatCardSkeleton showIcon={false} />

                    <div
                        style={{
                            padding: '16px',
                            borderRadius: '10px',
                            border: '1px solid var(--border)',
                            background: 'var(--bg)',
                            display: 'flex',
                            flexDirection: 'column',
                            gap: '10px',
                            minWidth: 0,
                        }}
                    >
                        <div
                            style={{
                                display: 'flex',
                                alignItems: 'center',
                                gap: '10px',
                            }}
                        >
                            <Skeleton variant="circle" width={36} height={36} />
                            <Skeleton variant="line" width="60%" height={16} />
                        </div>

                        <Skeleton variant="rect" height={64} />
                    </div>
                </div>
            </Card>

            {/* BILLING TABLE */}
            <Card title={`Billing Table (0-Based Page: ${page})`}>
                <div
                    style={{
                        display: 'flex',
                        flexWrap: 'wrap',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        gap: '10px',
                        marginBottom: '16px',
                    }}
                >
                    <button
                        type="button"
                        onClick={() => setIsTableLoading((prev) => !prev)}
                    >
                        {isTableLoading ? 'Stop Loading' : 'Simulate Loading'}
                    </button>

                    <button type="button" onClick={handleToggleEmpty}>
                        {showEmpty ? 'Restore Sample Data' : 'Simulate Empty'}
                    </button>
                </div>

                <Table<BillingEntry>
                    columns={columns}
                    data={activeRows}
                    rowKey={(row) => row.id}
                    isLoading={isTableLoading}
                    skeletonRows={4}
                    emptyMessage="No billing records available for this view."
                />

                {!showEmpty && (
                    <Pagination
                        page={page}
                        totalPages={totalPages}
                        totalElements={SAMPLE_ENTRIES.length}
                        size={size}
                        onPageChange={(nextPage) => setPage(nextPage)}
                        onSizeChange={(nextSize) => {
                            setSize(nextSize);
                            setPage(0);
                        }}
                        disabled={isTableLoading}
                    />
                )}
            </Card>

            {/* EMPTY STATES */}
            <Card title="Empty State Components">
                <div style={{ display: 'grid', gap: '16px' }}>
                    <EmptyState
                        variant="no-data"
                        icon="📭"
                        title="No Billing Data"
                        description="There are no billing records available yet."
                    />

                    <EmptyState
                        variant="no-results"
                        title="No Results Found"
                        description="Try changing your search or clearing the filters."
                        actionLabel="Clear Filters"
                        onAction={() => setShowToast(true)}
                    />

                    <EmptyState
                        variant="first-time"
                        title="Welcome to LedgerGuard"
                        description="Get started by creating your first billing record."
                        actionLabel="Get Started"
                        onAction={() => setIsModalOpen(true)}
                    />
                </div>
            </Card>

            {/* ERROR STATES */}
            <Card title="Error State Components">
                <div style={{ display: 'grid', gap: '16px' }}>
                    <ErrorState
                        variant="network"
                        title="Network Error"
                        message="Unable to connect. Please check your internet connection."
                        onRetry={() => setShowToast(true)}
                    />

                    <ErrorState
                        variant="server"
                        title="Server Error"
                        message="Something went wrong on the server."
                        traceId="LG-SERVER-1001"
                        onRetry={() => setShowToast(true)}
                    />

                    <ErrorState
                        variant="forbidden"
                        title="Access Forbidden"
                        message="You do not have permission to access this resource."
                    />

                    <ErrorState
                        variant="generic"
                        title="Something Went Wrong"
                        message="An unexpected error occurred."
                        onRetry={() => setShowToast(true)}
                    />
                </div>
            </Card>

            {/* INLINE AND FIELD ERRORS */}
            <Card title="Inline Error & Field Error">
                <button
                    type="button"
                    onClick={() => setShowInlineError((prev) => !prev)}
                    style={{ marginBottom: '12px' }}
                >
                    {showInlineError ? 'Hide Errors' : 'Show Errors'}
                </button>

                {showInlineError && (
                    <>
                        <InlineError message="Unable to load billing information." />

                        <div style={{ marginTop: '16px' }}>
                            <label htmlFor="demo-email">Email Address</label>

                            <input
                                id="demo-email"
                                type="email"
                                placeholder="Enter your email"
                                aria-invalid="true"
                                aria-describedby="demo-field-error"
                            />

                            <FieldError
                                id="demo-field-error"
                                message="Please enter a valid email address."
                            />
                        </div>
                    </>
                )}
            </Card>

            {/* ERROR BOUNDARY FALLBACK */}
            <Card title="Error Boundary Fallback">
                <p>
                    Preview the fallback UI that can be used when a component
                    crashes.
                </p>

                <button
                    type="button"
                    onClick={() => setShowFallback(true)}
                >
                    Show Error Boundary Fallback
                </button>
            </Card>

            {/* MODAL */}
            <Card title="Modal Component">
                <p>Click below to preview the responsive modal.</p>

                <button
                    type="button"
                    onClick={() => setIsModalOpen(true)}
                >
                    Open Modal
                </button>
            </Card>

            <Modal
                isOpen={isModalOpen}
                title="LedgerGuard Modal Demo"
                onClose={() => setIsModalOpen(false)}
            >
                <p>This is a reusable modal component.</p>

                <p>
                    It supports mobile width and scrollable content.
                </p>

                <button
                    type="button"
                    onClick={() => setIsModalOpen(false)}
                >
                    Close Modal
                </button>
            </Modal>

            {/* TOAST */}
            <Card title="Toast Notifications">
                <p>Click the button to preview a toast notification.</p>

                <button
                    type="button"
                    onClick={() => setShowToast(true)}
                >
                    Show Toast
                </button>
            </Card>

            {showToast && (
                <Toast
                    type="success"
                    message="LedgerGuard action completed successfully!"
                    onClose={() => setShowToast(false)}
                />
            )}
        </section>
    );
};

export default ComponentsDemoPage;
