import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { Card, Tabs, Descriptions, Tag, Button, Statistic, Row, Col, Table } from 'antd';
import { Building2, Plus, Mail, MapPin, Receipt, Ticket, Activity } from 'lucide-react';
import api from '../api/axiosInstance';
import { useTickets } from '../api/queries';

const CompanyDetail = () => {
  const { id } = useParams();
  const [activeTab, setActiveTab] = useState('overview');

  const { data: company, isLoading } = useQuery({
    queryKey: ['company', id],
    queryFn: async () => {
      const { data } = await api.get(`/companies/${id}`);
      return data;
    },
    enabled: !!id,
  });

  const { data: ticketsData, isLoading: ticketsLoading } = useTickets(0, 10); // Mocking fetch by companyId

  if (isLoading) return <Card loading={true} />;
  if (!company) return <div>Company not found</div>;

  const OverviewTab = () => (
    <div className="space-y-6">
      <Row gutter={[16, 16]}>
        <Col span={8}>
          <Card bordered={false} className="bg-brand-paper">
            <Statistic title="Total Billed (YTD)" value={company.stats?.totalBilledYtd || 0} precision={2} prefix="₹" />
          </Card>
        </Col>
        <Col span={8}>
          <Card bordered={false} className="bg-brand-paper">
            <Statistic title="Tickets This Month" value={company.stats?.ticketsThisMonth || 0} />
          </Card>
        </Col>
        <Col span={8}>
          <Card bordered={false} className="bg-brand-paper">
            <Statistic title="Outstanding Balance" value={company.stats?.outstandingBalance || 0} precision={2} prefix="₹" valueStyle={{ color: '#cf1322' }} />
          </Card>
        </Col>
      </Row>

      <Card title="Company Information" className="shadow-none border border-gray-100">
        <Descriptions column={{ xxl: 3, xl: 3, lg: 2, md: 1, sm: 1, xs: 1 }} bordered>
          <Descriptions.Item label="Company Name">{company.name}</Descriptions.Item>
          <Descriptions.Item label="GST Number"><span className="font-mono">{company.gstNumber}</span></Descriptions.Item>
          <Descriptions.Item label="Status">
            {company.active ? <Tag color="success">Active</Tag> : <Tag color="error">Inactive</Tag>}
          </Descriptions.Item>
          <Descriptions.Item label="Contact Name">{company.contactName || '—'}</Descriptions.Item>
          <Descriptions.Item label="Billing Email">{company.billingEmail || '—'}</Descriptions.Item>
          <Descriptions.Item label="Phone">{company.phone || '—'}</Descriptions.Item>
          <Descriptions.Item label="Address" span={3}>
            {company.address}, {company.city}, {company.state} {company.pinCode}
          </Descriptions.Item>
          <Descriptions.Item label="Billing Cycle">{company.billingCycle.replace('_', ' ')}</Descriptions.Item>
          <Descriptions.Item label="Service Charge %">{company.serviceChargePct}%</Descriptions.Item>
        </Descriptions>
      </Card>
    </div>
  );

  const TicketsTab = () => {
    const columns = [
      { title: 'Date', dataIndex: 'travelDate', key: 'travelDate' },
      { title: 'PNR', dataIndex: 'pnrNumber', key: 'pnr', render: (t: string) => <span className="font-mono">{t}</span> },
      { title: 'Status', dataIndex: 'status', key: 'status', render: (s: string) => <Tag>{s}</Tag> },
      { title: 'Amount', dataIndex: 'totalAmount', key: 'amt', render: (a: number) => `₹${a?.toFixed(2) || '0.00'}` }
    ];

    return (
      <Card className="shadow-none border border-gray-100">
        <Table dataSource={ticketsData?.content} columns={columns} rowKey="id" pagination={false} loading={ticketsLoading} />
      </Card>
    );
  };

  const tabs = [
    { key: 'overview', label: <span className="flex items-center gap-2"><Building2 size={16} /> Overview</span>, children: <OverviewTab /> },
    { key: 'tickets', label: <span className="flex items-center gap-2"><Ticket size={16} /> Tickets</span>, children: <TicketsTab /> },
    { key: 'invoices', label: <span className="flex items-center gap-2"><Receipt size={16} /> Invoices</span>, children: <div className="p-8 text-center text-gray-500">Invoice list coming soon</div> },
    { key: 'settings', label: <span className="flex items-center gap-2"><Activity size={16} /> Audit Log</span>, children: <div className="p-8 text-center text-gray-500">Audit logs coming soon</div> },
  ];

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div className="flex items-center gap-4">
           <div className="w-16 h-16 bg-brand-dark rounded-xl flex items-center justify-center text-white text-2xl font-serif">
             {company.name.charAt(0)}
           </div>
           <div>
             <h1 className="text-3xl font-serif text-brand-dark mb-1">{company.name}</h1>
             <p className="text-gray-500 flex items-center gap-4">
               <span className="flex items-center gap-1"><MapPin size={14}/> {company.city || 'No city'}</span>
               <span className="flex items-center gap-1"><Mail size={14}/> {company.billingEmail || 'No email'}</span>
             </p>
           </div>
        </div>
        <div className="flex gap-3">
          <Button>Edit Company</Button>
          <Button type="primary" className="bg-brand-dark">Generate Invoice</Button>
        </div>
      </div>

      <Tabs activeKey={activeTab} onChange={setActiveTab} items={tabs} className="bg-white p-2 rounded-xl" />
    </div>
  );
};

export default CompanyDetail;
