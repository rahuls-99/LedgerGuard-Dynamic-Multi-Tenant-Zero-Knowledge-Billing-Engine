# LedgerGuard REST API Contract

Base URL: `http://localhost:8080`

## Required Headers

* `Authorization: Bearer <jwt_token>` (obtained from `POST /api/real-auth/login`)
* `X-Tenant-ID: <tenant_id>` (e.g., `alpha`, `beta`)
* `Idempotency-Key: <unique_key>` (required on `POST /api/v1/ledger/post`)

---

## 1. Authentication

### `POST /api/real-auth/login`

**Request**
``json
{
  "orgId": "orgA",
  "email": "admin@orga.com",
  "password": "123456"
}
``

**Response (`200 OK`)**
``json
{
  "success": true,
  "token": "<signed_jwt>",
  "user": {
    "userId": "u101",
    "orgId": "orgA",
    "role": "admin"
  }
}
``

---

## 2. Create Wallet

### `POST /api/v1/wallets`

**Request**
``json
{
  "walletId": "w-live-9187",
  "ownerId": "cust-101",
  "currency": "INR",
  "initialBalance": 1000.00
}
``

**Response (`201 Created`)**
``json
{
  "walletId": "w-live-9187",
  "ownerId": "cust-101",
  "currency": "INR",
  "balance": 1000,
  "system": false,
  "updatedAt": "2026-11-20T13:33:38.723457500Z"
}
``

---

## 3. Post Ledger Transaction (Idempotent)

### `POST /api/v1/ledger/post`

**Headers:** `Authorization`, `X-Tenant-ID`, `Idempotency-Key`

**Request** (`type`: `CHARGE` | `TOPUP` | `REFUND`)
``json
{
  "type": "CHARGE",
  "walletId": "w-live-9187",
  "amount": 250.00,
  "currency": "INR"
}
``

**Response - First Execution (`200 OK`)**
``json
{
  "transactionId": "TXN-5b7c2f56ffd5",
  "status": "SUCCESS",
  "failureCode": null,
  "balanceAfter": 750.00,
  "idempotentReplay": false
}
``

**Response - Duplicate Key with Same Payload (`200 OK`, Header `X-Idempotent-Replay: true`)**
``json
{
  "transactionId": "TXN-5b7c2f56ffd5",
  "status": "SUCCESS",
  "failureCode": null,
  "balanceAfter": 750.00,
  "idempotentReplay": true
}
``

**Response - Duplicate Key with Different Payload (`422 Unprocessable Entity`)**
``json
{
  "error": "IDEMPOTENCY_PAYLOAD_MISMATCH",
  "message": "Idempotency-Key already used with a different request payload"
}
``

---

## 4. Wallet Statement & Ledger Entries

### `GET /api/v1/wallets/{walletId}`

**Response (`200 OK`)**
``json
{
  "wallet": {
    "walletId": "w-live-9187",
    "ownerId": "cust-101",
    "currency": "INR",
    "balance": 750.00,
    "system": false,
    "updatedAt": "2026-11-20T13:33:39.016Z"
  },
  "entries": [
    {
      "entryId": "le_decea2e2c6594dbebdedad8642f9d516",
      "transactionId": "TXN-5b7c2f56ffd5",
      "direction": "DEBIT",
      "amount": 250.00,
      "balanceAfter": 750.00,
      "createdAt": "2026-11-20T13:33:39.034Z"
    }
  ]
}
``

**Response - Wallet Not Found / Different Tenant (`404 Not Found`)**
``json
{
  "error": "WALLET_NOT_FOUND",
  "message": "Wallet not found: w-live-9187"
}
``
