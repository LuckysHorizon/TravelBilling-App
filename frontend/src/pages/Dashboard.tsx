import { Card, Skeleton, Alert, Table, Tag, Empty } from 'antd';
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
import { getStatusTag, formatCurrency } from '../lib/statusUtils';

interface MetricCardProps {
  title: string;
  value: string;
  loading: boolean;
  icon: any;
  colorClass: string;
  subtitle?: string;
  delay?: string;
}

const MetricCard = ({ title, value, loading, icon: Icon, colorClass, subtitle, delay = '0ms' }: MetricCardProps) => (
  <Card className="h-full animate-slide-up hover:-translate-y-1 transition-transform duration-300 shadow-sm hover:shadow-md" style={{ animationDelay: delay, animationFillMode: 'both' }}>
    <Skeleton loading={loading} active paragraph={{ rows: 1 }} title={false}>
      <div className="flex justify-between items-start">
        <div>
          <p className="text-gray-500 text-sm font-medium mb-1">{title}</p>
          <h3 className="text-3xl font-serif font-bold text-brand-dark tabular-nums">{value}</h3>
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
      render: (status: string) => {
        const tag = getStatusTag(status);
        return <Tag className={tag.className}>{tag.label}</Tag>;
      },
    },
  ];

  if (statsError) {
    return <Alert type="error" message="Failed to load dashboard data" showIcon className="m-6" />;
  }

  return (
    <div className="space-y-6">
      
      {/* Page Header */}
      <div className="animate-fade-in">
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
                className="text-brand-dark font-semibold hover:underline flex items-center gap-1"
              >
                Review now &rarr;
              </button>
            </div>
          }
          type="warning"
          showIcon
          icon={<AlertCircle className="mt-1 animate-pulse-subtle text-brand-gold" />}
          className="bg-brand-paper border-brand-gold/30 rounded-xl animate-slide-up shadow-sm"
          style={{ animationDelay: '100ms', animationFillMode: 'both' }}
        />
      )}

      {/* KPI Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <MetricCard
          title="Revenue (This Month)"
          value={formatCurrency(stats?.currentMonthRevenue)}
          icon={IndianRupee}
          loading={statsLoading}
          colorClass="bg-brand-paper text-brand-dark"
          subtitle="Total approved & billed"
          delay="100ms"
        />
        <MetricCard
          title="Tickets Processed"
          value={stats?.currentMonthTickets?.toLocaleString() || '0'}
          icon={TicketIcon}
          loading={statsLoading}
          colorClass="bg-brand-paper text-brand-dark"
          subtitle="This month"
          delay="200ms"
        />
        <MetricCard
          title="Outstanding Balance"
          value={formatCurrency(stats?.outstandingBalance)}
          icon={FileText}
          loading={statsLoading}
          colorClass="bg-brand-paper text-brand-dark"
          subtitle="Sent but unpaid invoices"
          delay="300ms"
        />
        <MetricCard
          title="Pending Invoices"
          value={stats?.pendingInvoicesCount?.toLocaleString() || '0'}
          icon={AlertCircle}
          loading={statsLoading}
          colorClass="bg-brand-paper text-brand-gold"
          subtitle="Draft or unsent"
          delay="400ms"
        />
      </div>

      {/* Main Content Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        
        {/* Revenue Chart */}
        <div className="lg:col-span-2 animate-slide-up" style={{ animationDelay: '500ms', animationFillMode: 'both' }}>
          <Card className="h-full relative overflow-hidden" title={<span className="font-serif">Revenue Trend (6 Months)</span>}>
            {/* Subtle gradient background for chart */}
            <div className="absolute inset-0 bg-gradient-to-b from-transparent to-brand-paper/50 pointer-events-none" />
            
            <div className="h-[300px] w-full relative z-10">
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
                      cursor={{ fill: '#f9fafb' }}
                      formatter={(value: any) => [
                        <span className="font-semibold text-brand-dark">{formatCurrency(value as number)}</span>, 
                        <span className="text-gray-500">Revenue</span>
                      ]}
                      contentStyle={{ 
                        borderRadius: '12px', 
                        border: '1px solid #e5e7eb', 
                        boxShadow: '0 10px 15px -3px rgba(0, 0, 0, 0.05)',
                        padding: '12px 16px',
                        backgroundColor: 'rgba(255, 255, 255, 0.95)',
                        backdropFilter: 'blur(4px)'
                      }}
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
        <div className="lg:col-span-1 animate-slide-up" style={{ animationDelay: '600ms', animationFillMode: 'both' }}>
          <Card 
            className="h-full" 
            title={<span className="font-serif">Recent Tickets</span>}
            extra={<a onClick={() => navigate('/tickets')} className="text-brand-dark font-medium hover:text-brand-gold transition-colors text-sm">View All</a>}
          >
            <Table
              dataSource={ticketsData?.content}
              columns={columns}
              rowKey="id"
              pagination={false}
              loading={ticketsLoading}
              size="small"
              className="mt-2"
              locale={{
                emptyText: <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="No recent tickets" />
              }}
            />
          </Card>
        </div>

      </div>
    </div>
  );
};

export default Dashboard;
