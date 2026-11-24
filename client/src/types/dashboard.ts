export interface DashboardStats {
    totalUsers: number;
    totalTransactions: number;
    totalWalletBalance: string;
    currency: string;
    lastTransactionAt: string | null;
}