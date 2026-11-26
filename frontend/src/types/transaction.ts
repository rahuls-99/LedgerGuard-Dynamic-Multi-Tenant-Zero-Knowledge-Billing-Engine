
export interface Transaction {
    transactionId: string;
    type: string;
    amount: string;
    currency: string;
    status: string;
    createdAt: string;
    failureReason: string | null;
}
