export interface LoginResponse {
    success: boolean;
    token?: string;
    user?: unknown;
    message?: string;
}

export function loginUser(
    orgId: string,
    email: string,
    password: string
): Promise<LoginResponse>;

export function fetchUserProfile(): Promise<unknown>;