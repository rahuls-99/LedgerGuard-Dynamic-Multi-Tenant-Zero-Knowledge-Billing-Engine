
import api from './axiosConfig';

// Login API
export const loginUser = async (orgId, email, password) => {
    const res = await api.post('/real-auth/login', {
        orgId,
        email,
        password
    });

    if (res.data.success) {
        localStorage.setItem('token', res.data.token);
        localStorage.setItem('user', JSON.stringify(res.data.user));
    }

    return res.data;
};

// Get logged-in user profile
export const fetchUserProfile = async () => {
    const res = await api.get('/real-auth/me');
    return res.data;
};
