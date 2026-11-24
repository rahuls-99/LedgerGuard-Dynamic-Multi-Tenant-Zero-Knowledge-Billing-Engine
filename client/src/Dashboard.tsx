
import React from 'react';
import { Table } from './components/Table';

interface DashboardUser {
    name?: string;
    orgName?: string;
    orgId?: string;
}

interface DashboardProps {
    user: DashboardUser | null;
    onLogout: () => void;
}

export const Dashboard: React.FC<DashboardProps> = ({ user, onLogout }) => {
    return (
        <div
            style={{
                display: 'flex',
                minHeight: '100vh',
                fontFamily: 'sans-serif',
                backgroundColor: '#f4f6f8',
            }}
        >
            {/* Left Sidebar */}
            <aside
                style={{
                    width: '240px',
                    backgroundColor: '#1e293b',
                    color: '#fff',
                    padding: '20px',
                    display: 'flex',
                    flexDirection: 'column',
                    justifyContent: 'space-between',
                }}
            >
                <div>
                    <h2
                        style={{
                            fontSize: '20px',
                            marginBottom: '30px',
                            color: '#38bdf8',
                        }}
                    >
                        LedgerGuard
                    </h2>

                    <nav
                        style={{
                            display: 'flex',
                            flexDirection: 'column',
                            gap: '12px',
                        }}
                    >
                        <div
                            style={{
                                padding: '10px',
                                backgroundColor: '#334155',
                                borderRadius: '6px',
                                fontWeight: 'bold',
                            }}
                        >
                            Overview & Ledger
                        </div>

                        <div
                            style={{
                                padding: '10px',
                                color: '#cbd5e1',
                                cursor: 'pointer',
                            }}
                        >
                            Analytics
                        </div>

                        <div
                            style={{
                                padding: '10px',
                                color: '#cbd5e1',
                                cursor: 'pointer',
                            }}
                        >
                            API Security
                        </div>
                    </nav>
                </div>

                <button
                    type="button"
                    onClick={onLogout}
                    style={{
                        padding: '10px',
                        backgroundColor: '#ef4444',
                        color: '#fff',
                        border: 'none',
                        borderRadius: '6px',
                        cursor: 'pointer',
                    }}
                >
                    Logout
                </button>
            </aside>

            {/* Main Content Area */}
            <main style={{ flex: 1, padding: '24px' }}>
                {/* Top Header */}
                <header
                    style={{
                        display: 'flex',
                        justifyContent: 'space-between',
                        alignItems: 'center',
                        backgroundColor: '#fff',
                        padding: '16px 24px',
                        borderRadius: '8px',
                        marginBottom: '24px',
                        boxShadow: '0 1px 3px rgba(0,0,0,0.1)',
                    }}
                >
                    <h1 style={{ fontSize: '20px', margin: 0 }}>
                        Welcome, {user?.name || 'Admin'}
                    </h1>

                    <span
                        style={{
                            backgroundColor: '#e0f2fe',
                            color: '#0369a1',
                            padding: '6px 12px',
                            borderRadius: '20px',
                            fontSize: '14px',
                            fontWeight: 'bold',
                        }}
                    >
                        Tenant: {user?.orgName || 'Org A'} ({user?.orgId || 'orgA'})
                    </span>
                </header>

                {/* Summary Metric Cards */}
                <div
                    style={{
                        display: 'grid',
                        gridTemplateColumns: 'repeat(3, 1fr)',
                        gap: '20px',
                        marginBottom: '24px',
                    }}
                >
                    <div
                        style={{
                            backgroundColor: '#fff',
                            padding: '20px',
                            borderRadius: '8px',
                            boxShadow: '0 1px 3px rgba(0,0,0,0.1)',
                        }}
                    >
                        <p
                            style={{
                                color: '#64748b',
                                margin: '0 0 8px 0',
                                fontSize: '14px',
                            }}
                        >
                            Total Spend (This Month)
                        </p>
                        <h3 style={{ margin: 0, fontSize: '24px' }}>
                            $12,450.00
                        </h3>
                    </div>

                    <div
                        style={{
                            backgroundColor: '#fff',
                            padding: '20px',
                            borderRadius: '8px',
                            boxShadow: '0 1px 3px rgba(0,0,0,0.1)',
                        }}
                    >
                        <p
                            style={{
                                color: '#64748b',
                                margin: '0 0 8px 0',
                                fontSize: '14px',
                            }}
                        >
                            Committed Transactions
                        </p>
                        <h3
                            style={{
                                margin: 0,
                                fontSize: '24px',
                                color: '#16a34a',
                            }}
                        >
                            1,284
                        </h3>
                    </div>

                    <div
                        style={{
                            backgroundColor: '#fff',
                            padding: '20px',
                            borderRadius: '8px',
                            boxShadow: '0 1px 3px rgba(0,0,0,0.1)',
                        }}
                    >
                        <p
                            style={{
                                color: '#64748b',
                                margin: '0 0 8px 0',
                                fontSize: '14px',
                            }}
                        >
                            Failed / Rolled Back
                        </p>
                        <h3
                            style={{
                                margin: 0,
                                fontSize: '24px',
                                color: '#dc2626',
                            }}
                        >
                            0
                        </h3>
                    </div>
                </div>

                {/* Billing Ledger Transactions */}
                <div
                    style={{
                        backgroundColor: '#fff',
                        padding: '20px',
                        borderRadius: '8px',
                        boxShadow: '0 1px 3px rgba(0,0,0,0.1)',
                    }}
                >
                    <h3 style={{ marginTop: 0, marginBottom: '16px' }}>
                        Billing Ledger Transactions
                    </h3>

                    <Table
                        columns={[]}
                        data={[]}
                        rowKey={(row) => String(row)}
                    />
                </div>
            </main>
        </div>
    );
};
