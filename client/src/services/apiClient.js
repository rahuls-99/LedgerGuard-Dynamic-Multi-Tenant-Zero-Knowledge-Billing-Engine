
import axios from 'axios';

const apiClient = axios.create({
    baseURL: 'http://100.107.61.66:8080/api',
    headers: {
        'Content-Type': 'application/json'
    },
    timeout: 5000,
});

// Automatically attach JWT token to every request
apiClient.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem('token');

        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }

        return config;
    },
    (error) => Promise.reject(error)
);

// Automatic redirect on 401 Unauthorized
apiClient.interceptors.response.use(
    (response) => response,
    (error) => {
        if (error.response?.status === 401) {
            localStorage.removeItem('token');
            localStorage.removeItem('user');
            window.location.href = '/login';
        }

        return Promise.reject(error);
    }
);

// Login API
export const authenticateUser = async (orgId, email, password) => {
    try {
        const response = await apiClient.post('/real-auth/login', {
            orgId,
            email,
            password
        });

        const { token, user } = response.data;

        localStorage.setItem('token', token);
        localStorage.setItem('user', JSON.stringify(user));

        return user;

    } catch (error) {
        const errorMsg =
            error.response?.data?.message ||
            'Network connectivity error';

        throw new Error(errorMsg);
    }
};

export default apiClient;
