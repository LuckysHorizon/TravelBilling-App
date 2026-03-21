import { Card, Skeleton, Alert, Table, Tag } from 'antd';
import { useDashboardStats, useRecentTickets } from '../api/queries';
import { 
  IndianRupee, 
  Ticket as TicketIcon, 
  FileText, 
  AlertCircle 
} from 'lucide-react';
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, Cell } from 'recharts';
import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import api from '../api/axiosInstance';

const MetricCard = ({ title, value, loading, icon: Icon, colorClass, subtitle }: any) => (
  <Card className="h-full">
    <Skeleton loading={loading} active paragraph={{ rows: 1 }} title={false}>
      <div className="flex justify-between items-start">
        <div>
          <p className="text-gray-500 text-sm font-medium mb-1">{title}</p>
          <h3 className="text-3xl font-serif font-bold text-brand-dark">{value}</h3>
          {subtitle && <p className="text-xs text-gray-400 mt-2">{subtitle}</p>}
        </div>
        <div className={`p-3 rounded-xl ${colorClass}`}>
          <Icon size={24} />
        </div>
      </div>
    </Skeleton>
  </Card>
);

const Dashboard = () => {
  const { data: stats, isLoading: statsLoading, error: statsError } = useDashboardStats();
  const { data: ticketsData, isLoading: ticketsLoading } = useRecentTickets(5);
  const navigate = useNavigate();

  // Fetch real revenue trend from API
  const { data: chartData, isLoading: chartLoading } = useQuery({
    queryKey: ['reports', 'revenue-trend'],
    queryFn: async () => {
      const { data } = await api.get('/reports/revenue-trend');
      return data;
    },
  });

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(amount || 0);
  };

  const getStatusTag = (status: string) => {
    switch(status) {
      case 'APPROVED': return <Tag color="blue">Approved</Tag>;
      case 'PENDING_REVIEW': return <Tag color="warning">Pending Review</Tag>;
      case 'BILLED': return <Tag color="purple">Billed</Tag>;
      case 'PAID': return <Tag color="success">Paid</Tag>;
      default: return <Tag color="default">{status}</Tag>;
    }
  };

  const columns = [
    {
      title: 'PNR',
      dataIndex: 'pnrNumber',
      key: 'pnr',
      render: (text: string) => <a className="font-mono text-brand-accent">{text}</a>,
    },
    {
      title: 'Company',
      dataIndex: 'companyName',
      key: 'company',
      render: (text: string) => <span className="font-medium">{text}</span>,
    },
    {
      title: 'Travel Date',
      dataIndex: 'travelDate',
      key: 'date',
    },
    {
      title: 'Amount',
      dataIndex: 'totalAmount',
      key: 'amount',
      render: (amount: number) => formatCurrency(amount),
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => getStatusTag(status),
    },
  ];

  if (statsError) {
    return <Alert type="error" message="Failed to load dashboard data" showIcon className="m-6" />;
  }

  return (
    <div className="space-y-6">
      
      {/* Page Header */}
      <div>
        <h1 className="text-3xl font-serif text-brand-dark mb-1">Overview</h1>
        <p className="text-gray-500">Welcome back. Here's what's happening with your agency today.</p>
      </div>

      {/* Pending Action Banner */}
      {!statsLoading && stats?.pendingTicketsCount > 0 && (
        <Alert
          message={
            <div className="flex items-center justify-between w-full">
              <span>You have <strong className="text-brand-dark">{stats?.pendingTicketsCount} tickets</strong> waiting for manual review.</span>
              <button 
                onClick={() => navigate('/tickets')}
                className="text-brand-accent font-semibold hover:underline"
              >
                Review now &rarr;
              </button>
            </div>
          }
          type="warning"
          showIcon
          icon={<AlertCircle className="mt-1" />}
          className="bg-amber-50 border-amber-200 text-amber-900 rounded-xl"
        />
      )}

      {/* KPI Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <MetricCard
          title="Revenue (This Month)"
          value={formatCurrency(stats?.currentMonthRevenue)}
          icon={IndianRupee}
          loading={statsLoading}
          colorClass="bg-green-100 text-green-600"
          subtitle="Total approved & billed"
        />
        <MetricCard
          title="Tickets Processed"
          value={stats?.currentMonthTickets?.toLocaleString() || '0'}
          icon={TicketIcon}
          loading={statsLoading}
          colorClass="bg-blue-100 text-blue-600"
          subtitle="This month"
        />
        <MetricCard
          title="Outstanding Balance"
          value={formatCurrency(stats?.outstandingBalance)}
          icon={FileText}
          loading={statsLoading}
          colorClass="bg-amber-100 text-amber-600"
          subtitle="Sent but unpaid invoices"
        />
        <MetricCard
          title="Pending Invoices"
          value={stats?.pendingInvoicesCount?.toLocaleString() || '0'}
          icon={AlertCircle}
          loading={statsLoading}
          colorClass="bg-purple-100 text-purple-600"
          subtitle="Draft or unsent"
        />
      </div>

      {/* Main Content Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        
        {/* Revenue Chart */}
        <div className="lg:col-span-2">
          <Card className="h-full" title={<span className="font-serif">Revenue Trend (6 Months)</span>}>
            <div className="h-[300px] w-full">
              {statsLoading || chartLoading ? (
                <Skeleton active className="h-full w-full" />
              ) : (
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={chartData || []} margin={{ top: 10, right: 10, left: 10, bottom: 20 }}>
                    <XAxis 
                      dataKey="name" 
                      axisLine={false} 
                      tickLine={false} 
                      tick={{ fill: '#6b7280', fontSize: 12 }} 
                      dy={10}
                    />
                    <YAxis 
                      hide 
                    />
                    <Tooltip 
                      cursor={{ fill: '#f3f4f6' }}
                      formatter={(value: any) => [formatCurrency(value as number), 'Revenue']}
                      contentStyle={{ borderRadius: '8px', border: 'none', boxShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.1)' }}
                    />
                    <Bar dataKey="revenue" radius={[6, 6, 6, 6]} barSize={40}>
                      {(chartData || []).map((entry: any, index: number) => (
                        <Cell key={`cell-${index}`} fill={entry.isCurrent ? '#c4a77d' : '#1a1a1a'} />
                      ))}
                    </Bar>
                  </BarChart>
                </ResponsiveContainer>
              )}
            </div>
          </Card>
        </div>

        {/* Recent Tickets Table */}
        <div className="lg:col-span-1">
          <Card 
            className="h-full" 
            title={<span className="font-serif">Recent Tickets</span>}
            extra={<a onClick={() => navigate('/tickets')} className="text-brand-accent text-sm">View All</a>}
          >
            <Table
              dataSource={ticketsData?.content}
              columns={columns}
              rowKey="id"
              pagination={false}
              loading={ticketsLoading}
              size="small"
              className="mt-2"
            />
          </Card>
        </div>

      </div>
    </div>
  );
};

export default Dashboard;
