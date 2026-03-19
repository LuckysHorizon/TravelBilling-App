import React, { useState } from 'react';
import { Card, Select, DatePicker, Button, Table } from 'antd';
import { Download, Filter } from 'lucide-react';
import { 
  BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, Cell,
  PieChart, Pie, Legend
} from 'recharts';
import { useDashboardStats } from '../api/queries';
import dayjs from 'dayjs';

const { RangePicker } = DatePicker;
const { Option } = Select;

const Reports = () => {
  const { data: stats, isLoading } = useDashboardStats();
  const [dateRange, setDateRange] = useState<any>([dayjs().startOf('year'), dayjs()]);

  // Dummy chart data for illustration
  const revenueData = [
    { name: 'Sep', revenue: 450000 },
    { name: 'Oct', revenue: 520000 },
    { name: 'Nov', revenue: 480000 },
    { name: 'Dec', revenue: 610000 },
    { name: 'Jan', revenue: 590000 },
    { name: 'Feb', revenue: stats?.currentMonthRevenue || 650000, isCurrent: true },
  ];

  const clientSpendData = [
    { name: 'TechCorp India', value: 1200000 },
    { name: 'Global Logistics', value: 850000 },
    { name: 'Apex Solutions', value: 450000 },
    { name: 'Zenith Retail', value: 300000 },
    { name: 'Others', value: 500000 },
  ];

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

  const tableData = [
    { key: 1, name: 'TechCorp India', tickets: 145, spend: 1200000, outstanding: 45000 },
    { key: 2, name: 'Global Logistics', tickets: 87, spend: 850000, outstanding: 120000 },
    { key: 3, name: 'Apex Solutions', tickets: 42, spend: 450000, outstanding: 0 },
    { key: 4, name: 'Zenith Retail', tickets: 30, spend: 300000, outstanding: 15000 },
  ];

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
        <Card title={<span className="font-serif">Revenue Over Time</span>} className="shadow-sm">
          <div className="h-[300px] w-full mt-4">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={revenueData}>
                <XAxis dataKey="name" axisLine={false} tickLine={false} dy={10} fontSize={12} fill="#6b7280" />
                <YAxis hide />
                <Tooltip 
                  formatter={(value: any) => [formatCurrency(value as number), 'Revenue']}
                  contentStyle={{ borderRadius: '8px', border: 'none', boxShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.1)' }}
                />
                <Bar dataKey="revenue" radius={[6, 6, 6, 6]} barSize={40}>
                  {revenueData.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={entry.isCurrent ? '#c4a77d' : '#1a1a1a'} />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </div>
        </Card>

        {/* Top Clients Chart */}
        <Card title={<span className="font-serif">Top Clients by Billing</span>} className="shadow-sm">
          <div className="h-[300px] w-full mt-4 flex justify-center items-center">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie
                  data={clientSpendData}
                  cx="50%"
                  cy="50%"
                  innerRadius={80}
                  outerRadius={110}
                  paddingAngle={5}
                  dataKey="value"
                  stroke="none"
                >
                  {clientSpendData.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                  ))}
                </Pie>
                <Tooltip formatter={(value: any) => formatCurrency(value as number)} />
                <Legend verticalAlign="bottom" height={36} iconType="circle" />
              </PieChart>
            </ResponsiveContainer>
          </div>
        </Card>
      </div>

      {/* Client Breakdown Table */}
      <Card title={<span className="font-serif">Client Breakdown</span>} className="shadow-sm">
        <div className="mb-4 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Filter size={16} className="text-gray-400" />
            <Select defaultValue="all" className="w-48" bordered={false}>
              <Option value="all">All Clients</Option>
              <Option value="outstanding">With Outstanding</Option>
            </Select>
          </div>
        </div>
        <Table 
          columns={columns} 
          dataSource={tableData} 
          pagination={false} 
          size="middle" 
        />
      </Card>
    </div>
  );
};

export default Reports;
