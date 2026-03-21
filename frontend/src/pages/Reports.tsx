import React, { useState } from 'react';
import { Card, Select, DatePicker, Button, Table } from 'antd';
import { Download, Filter } from 'lucide-react';
import { 
  BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, Cell,
  PieChart, Pie, Legend
} from 'recharts';
import { useQuery } from '@tanstack/react-query';
import api from '../api/axiosInstance';
import dayjs from 'dayjs';

const { RangePicker } = DatePicker;
const { Option } = Select;

const Reports = () => {
  const [dateRange, setDateRange] = useState<any>([dayjs().startOf('year'), dayjs()]);

  const { data: revenueTrend, isLoading: revLoading } = useQuery({
    queryKey: ['reports', 'revenue-trend'],
    queryFn: async () => {
      const { data } = await api.get('/reports/revenue-trend');
      return data;
    },
  });

  const { data: clientBreakdown, isLoading: clientLoading } = useQuery({
    queryKey: ['reports', 'client-breakdown'],
    queryFn: async () => {
      const { data } = await api.get('/reports/client-breakdown');
      return data;
    },
  });

  const COLORS = ['#1a1a1a', '#4a4a4a', '#c4a77d', '#e5e7eb', '#9ca3af'];

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(amount || 0);
  };

  const columns = [
    { title: 'Company Name', dataIndex: 'name', key: 'name', className: 'font-medium' },
    { title: 'Total Tickets', dataIndex: 'tickets', key: 'tickets' },
    { title: 'Total Spend', dataIndex: 'spend', key: 'spend', render: (val: number) => formatCurrency(val) },
    { title: 'Outstanding', dataIndex: 'outstanding', key: 'outstanding', render: (val: number) => <span className="text-red-600">{formatCurrency(val)}</span> },
  ];

  // Prepare pie chart data from clientBreakdown
  const pieData = (clientBreakdown || []).map((item: any) => ({
    name: item.name,
    value: Number(item.spend) || 0,
  }));

  return (
    <div className="space-y-6">
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
        <div>
          <h1 className="text-3xl font-serif text-brand-dark mb-1">Reports & Analytics</h1>
          <p className="text-gray-500">Analyze your billing and agency performance.</p>
        </div>
        <div className="flex gap-3 items-center">
          <RangePicker value={dateRange} onChange={setDateRange} className="rounded-lg" />
          <Button icon={<Download size={16} />} className="rounded-lg">Export CSV</Button>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Revenue Chart */}
        <Card title={<span className="font-serif">Revenue Over Time</span>} className="shadow-sm" loading={revLoading}>
          <div className="h-[300px] w-full mt-4">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={revenueTrend || []}>
                <XAxis dataKey="name" axisLine={false} tickLine={false} dy={10} fontSize={12} fill="#6b7280" />
                <YAxis hide />
                <Tooltip 
                  formatter={(value: any) => [formatCurrency(value as number), 'Revenue']}
                  contentStyle={{ borderRadius: '8px', border: 'none', boxShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.1)' }}
                />
                <Bar dataKey="revenue" radius={[6, 6, 6, 6]} barSize={40}>
                  {(revenueTrend || []).map((entry: any, index: number) => (
                    <Cell key={`cell-${index}`} fill={entry.isCurrent ? '#c4a77d' : '#1a1a1a'} />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </div>
        </Card>

        {/* Top Clients Chart */}
        <Card title={<span className="font-serif">Top Clients by Billing</span>} className="shadow-sm" loading={clientLoading}>
          <div className="h-[300px] w-full mt-4 flex justify-center items-center">
            {pieData.length > 0 ? (
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie
                    data={pieData}
                    cx="50%"
                    cy="50%"
                    innerRadius={80}
                    outerRadius={110}
                    paddingAngle={5}
                    dataKey="value"
                    stroke="none"
                  >
                    {pieData.map((_: any, index: number) => (
                      <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                    ))}
                  </Pie>
                  <Tooltip formatter={(value: any) => formatCurrency(value as number)} />
                  <Legend verticalAlign="bottom" height={36} iconType="circle" />
                </PieChart>
              </ResponsiveContainer>
            ) : (
              <p className="text-gray-400">No client data yet</p>
            )}
          </div>
        </Card>
      </div>

      {/* Client Breakdown Table */}
      <Card title={<span className="font-serif">Client Breakdown</span>} className="shadow-sm">
        <Table 
          columns={columns} 
          dataSource={clientBreakdown || []} 
          pagination={false} 
          size="middle" 
          loading={clientLoading}
          rowKey="key"
        />
      </Card>
    </div>
  );
};

export default Reports;
