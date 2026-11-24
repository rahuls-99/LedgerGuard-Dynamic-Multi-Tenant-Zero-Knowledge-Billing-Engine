
# LedgerGuard UI Components Documentation

This document describes the reusable React components available in the LedgerGuard frontend.

## 1. Card
**File:** `Card.tsx`

Purpose: Displays content inside a reusable card layout.

Main props:
- `title`: Card heading.
- `children`: Content displayed inside the card.
- `className`: Optional custom CSS class.

## 2. EmptyState
**File:** `EmptyState.tsx`

Purpose: Displays a message when there is no data to show.

Main props:
- `title`: Empty-state heading.
- `description`: Explanation message.
- `variant`: Visual style.
- `onAction`: Optional action callback.

## 3. ErrorState
**File:** `ErrorState.tsx`

Purpose: Displays an error message and optional retry action.

Main props:
- `title`: Error heading.
- `message`: Error description.
- `variant`: Error display style.
- `onRetry`: Optional retry callback.

## 4. ErrorBoundaryFallback
**File:** `ErrorBoundaryFallback.tsx`

Purpose: Displays a fallback UI when a React component encounters an error.

## 5. InlineError and FieldError
**File:** `InlineError.tsx`

Purpose:
- `InlineError`: Displays general error messages.
- `FieldError`: Displays validation errors near form fields.

Main props:
- `message`: Error message.
- `className`: Optional custom class for InlineError.
- `id`: Optional element ID for FieldError.

## 6. Modal
**File:** `Modal.tsx`

Purpose: Displays content in a dialog overlay.

Main props:
- `isOpen`: Controls modal visibility.
- `title`: Modal heading.
- `onClose`: Callback to close the modal.
- `children`: Modal content.

Accessibility:
- Supports Escape key to close.
- Supports keyboard Tab navigation.
- Restores focus after closing.

## 7. Pagination
**File:** `Pagination.tsx`

Purpose: Provides navigation between pages of data.

Main props:
- `currentPage`: Current page.
- `totalPages`: Total number of pages.
- Additional props are defined in `PaginationProps`.

## 8. Skeleton
**File:** `Skeleton.tsx`

Purpose: Displays a loading placeholder.

Main props:
- `variant`: line, circle, or rect.
- `width`: Placeholder width.
- `height`: Placeholder height.
- `className`: Optional CSS class.

## 9. StatCard
**File:** `StatCard.tsx`

Purpose: Displays a dashboard statistic.

Main props are defined in `StatCardProps`.

## 10. StatCardGrid
**File:** `StatCardGrid.tsx`

Purpose: Arranges statistic cards in a responsive grid.

## 11. StatCardSkeleton
**File:** `StatCardSkeleton.tsx`

Purpose: Displays a loading placeholder for statistic cards.

## 12. StatusBadge
**File:** `StatusBadge.tsx`

Purpose: Displays the status of a billing operation.

Supported statuses:
- SUCCESS
- FAILED
- PENDING
- ROLLED_BACK

## 13. Table
**File:** `Table.tsx`

Purpose: Displays structured tabular data.

Main types:
- `TableProps`
- `TableColumn`

## 14. TableSkeleton
**File:** `TableSkeleton.tsx`

Purpose: Displays a loading placeholder for tables.

## 15. Toast
**File:** `Toast.tsx`

Purpose: Displays temporary feedback messages.

Supported types:
- success
- error
- warning
- info

## Component Exports

Reusable components and their public types are exported through:

`src/components/index.ts`

This allows components to be imported from a single entry point.

## Styling

Component styles are maintained in separate CSS files. Shared design tokens are defined in `src/index.css`.

## Responsive Design

Components are designed to support desktop, tablet, and mobile layouts.
## 16. StatusChip

**File:** `StatusBadge.tsx`

Purpose: Displays transaction status with an icon, label, color, and optional tooltip.

Props:
- `status: string`
- `size?: 'sm' | 'md'` (default: md)
- `showIcon?: boolean` (default: true)
- `showTooltip?: boolean` (default: true)

Supported statuses:
- SUCCESS
- FAILED
- PENDING
- ROLLED_BACK

Unknown statuses use a neutral style.

## 17. StatusFilterChip

**File:** `StatusFilterChip.tsx`

Purpose: Provides an interactive transaction status filter.

Props:
- `status: string`
- `selected: boolean`
- `onToggle: (status: string) => void`
- `count?: number`

Supports checkbox accessibility and keyboard interaction.

## 18. TransactionRow

**File:** `TransactionRow.tsx`

Purpose: Displays an individual transaction in table or card layout.

Props:
- `transaction: Transaction`
- `expanded: boolean`
- `onToggle: () => void`
- `variant: 'table' | 'card'`

Features:
- Short transaction ID with copy functionality.
- Transaction type, amount, status, and date.
- Expandable transaction details.
- Full transaction ID and UTC/local time.
- Failure reason for FAILED and ROLLED_BACK transactions.
- Memoized using React.memo.

## 19. TransactionList

**File:** `TransactionList.tsx`

Purpose: Displays a responsive list of transactions.

Props:
- `transactions: Transaction[]`
- `loading?: boolean` (default: false)
- `error?: string | null` (default: null)
- `traceId?: string`
- `onRetry?: () => void`

Features:
- Responsive desktop table and mobile card layouts.
- Loading skeleton.
- Empty state.
- Error state with optional retry.
- Expandable transaction details.

## Transaction Data Type

**File:** `src/types/transaction.ts`

The `Transaction` interface contains:
- `transactionId: string`
- `type: string`
- `amount: string`
- `currency: string`
- `status: string`
- `createdAt: string`
- `failureReason: string | null`