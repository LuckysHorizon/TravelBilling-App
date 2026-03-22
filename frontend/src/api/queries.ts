import { useQuery } from '@tanstack/react-query';
import api from './axiosInstance';

// Fetch Dashboard Stats
export const useDashboardStats = () => {
  return useQuery({
    queryKey: ['dashboardStats'],
    queryFn: async () => {
      const { data } = await api.get('/reports/dashboard-stats');
      return data;
    },
    refetchInterval: 300000, // Refresh every 5 minutes automatically
  });
};

// Fetch Recent Tickets (Reusable for dashboard tables)
export const useRecentTickets = (size = 5) => {
  return useQuery({
    queryKey: ['tickets', 'recent', size],
    queryFn: async () => {
      const { data } = await api.get(`/tickets?page=0&size=${size}&sort=createdAt,desc`);
      return data;
    },
  });
};

// Fetch Companies
export const useCompanies = (page = 0, size = 10) => {
  return useQuery({
    queryKey: ['companies', page, size],
    queryFn: async () => {
      const { data } = await api.get(`/companies?page=${page}&size=${size}`);
      return data;
    },
  });
};

// Fetch Tickets
export const useTickets = (page = 0, size = 10, status?: string, search?: string) => {
  return useQuery({
    queryKey: ['tickets', page, size, status, search],
    queryFn: async () => {
      const params = new URLSearchParams();
      params.set('page', String(page));
      params.set('size', String(size));
      params.set('sort', 'createdAt,desc');
      if (search) params.set('search', search);

      const url = status
        ? `/tickets/status/${status}?${params.toString()}`
        : `/tickets?${params.toString()}`;
      const { data } = await api.get(url);
      return data;
    },
  });
};
