import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Card, Tabs, Descriptions, Tag, Button, Statistic, Row, Col, Table, Modal, Form, Input, InputNumber, Select, message } from 'antd';
import { Building2, Plus, Mail, MapPin, Receipt, Ticket, Activity } from 'lucide-react';
import api from '../api/axiosInstance';

const CompanyDetail = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState('overview');
  const [editModalOpen, setEditModalOpen] = useState(false);
  const [editForm] = Form.useForm();
  const queryClient = useQueryClient();

  const { data: company, isLoading } = useQuery({
    queryKey: ['company', id],
    queryFn: async () => {
      const { data } = await api.get(`/companies/${id}`);
      return data;
    },
    enabled: !!id,
  });

  // Fetch tickets by company
  const { data: ticketsData, isLoading: ticketsLoading } = useQuery({
    queryKey: ['tickets', 'company', id],
    queryFn: async () => {
      const { data } = await api.get(`/tickets/company/${id}?page=0&size=20&sort=createdAt,desc`);
      return data;
    },
    enabled: !!id,
  });

  // Fetch invoices by company
  const { data: invoicesData, isLoading: invoicesLoading } = useQuery({
    queryKey: ['invoices', 'company', id],
    queryFn: async () => {
      const { data } = await api.get(`/invoices/company/${id}?page=0&size=20`);
      return data;
    },
    enabled: !!id,
  });

  const updateCompanyMutation = useMutation({
    mutationFn: async (values: any) => {
      const { data } = await api.put(`/companies/${id}`, values);
      return data;
    },
    onSuccess: () => {
      message.success('Company updated successfully');
      queryClient.invalidateQueries({ queryKey: ['company', id] });
      queryClient.invalidateQueries({ queryKey: ['companies'] });
      setEditModalOpen(false);
    },
    onError: (err: any) => {
      message.error(err.response?.data?.message || 'Failed to update company');
    },
  });

  const generateInvoiceMutation = useMutation({
    mutationFn: async () => {
      const now = new Date();
      const billingMonth = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
      const startDate = `${billingMonth}-01`;
      const lastDay = new Date(now.getFullYear(), now.getMonth() + 1, 0).getDate();
      const endDate = `${billingMonth}-${lastDay}`;

      const { data } = await api.post('/invoices/generate', {
        companyId: Number(id),
        billingMonth,
        startDate,
        endDate,
      });
      return data;
    },
    onSuccess: () => {
      message.success('Invoice generated successfully');
      queryClient.invalidateQueries({ queryKey: ['invoices'] });
    },
    onError: (err: any) => {
      message.error(err.response?.data?.message || 'Failed to generate invoice. Ensure there are approved tickets.');
    },
  });

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
            {[company.address, company.city, company.state, company.pinCode].filter(Boolean).join(', ') || '—'}
          </Descriptions.Item>
          <Descriptions.Item label="Billing Cycle">{company.billingCycle?.replace('_', ' ') || '—'}</Descriptions.Item>
          <Descriptions.Item label="Service Charge %">{company.serviceChargePct}%</Descriptions.Item>
        </Descriptions>
      </Card>
    </div>
  );

  const TicketsTab = () => {
    const columns = [
      { title: 'Date', dataIndex: 'travelDate', key: 'travelDate' },
      { title: 'PNR', dataIndex: 'pnrNumber', key: 'pnr', render: (t: string) => <span className="font-mono">{t}</span> },
      { title: 'Passenger', dataIndex: 'passengerName', key: 'passenger' },
      { title: 'Status', dataIndex: 'status', key: 'status', render: (s: string) => <Tag>{s}</Tag> },
      { title: 'Amount', dataIndex: 'totalAmount', key: 'amt', render: (a: number) => `₹${a?.toFixed(2) || '0.00'}` }
    ];

    return (
      <Card className="shadow-none border border-gray-100">
        <Table dataSource={ticketsData?.content} columns={columns} rowKey="id" pagination={false} loading={ticketsLoading} />
      </Card>
    );
  };

  const InvoicesTab = () => {
    const columns = [
      { title: 'Invoice #', dataIndex: 'invoiceNumber', key: 'num', render: (t: string) => <span className="font-mono font-semibold">{t}</span> },
      { title: 'Date', dataIndex: 'invoiceDate', key: 'date' },
      { title: 'Grand Total', dataIndex: 'grandTotal', key: 'total', render: (a: number) => `₹${a?.toFixed(2) || '0.00'}` },
      { title: 'Status', dataIndex: 'status', key: 'status', render: (s: string) => <Tag>{s}</Tag> },
    ];

    return (
      <Card className="shadow-none border border-gray-100">
        <Table dataSource={invoicesData?.content} columns={columns} rowKey="id" pagination={false} loading={invoicesLoading} />
      </Card>
    );
  };

  const tabs = [
    { key: 'overview', label: <span className="flex items-center gap-2"><Building2 size={16} /> Overview</span>, children: <OverviewTab /> },
    { key: 'tickets', label: <span className="flex items-center gap-2"><Ticket size={16} /> Tickets</span>, children: <TicketsTab /> },
    { key: 'invoices', label: <span className="flex items-center gap-2"><Receipt size={16} /> Invoices</span>, children: <InvoicesTab /> },
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
          <Button onClick={() => {
            editForm.setFieldsValue(company);
            setEditModalOpen(true);
          }}>Edit Company</Button>
          <Button 
            type="primary" 
            className="bg-brand-dark" 
            onClick={() => generateInvoiceMutation.mutate()}
            loading={generateInvoiceMutation.isPending}
          >
            Generate Invoice
          </Button>
        </div>
      </div>

      <Tabs activeKey={activeTab} onChange={setActiveTab} items={tabs} className="bg-white p-2 rounded-xl" />

      <Modal
        title="Edit Company"
        open={editModalOpen}
        onCancel={() => setEditModalOpen(false)}
        onOk={() => editForm.submit()}
        confirmLoading={updateCompanyMutation.isPending}
        okButtonProps={{ className: 'bg-brand-dark' }}
        width={600}
      >
        <Form form={editForm} layout="vertical" onFinish={(values) => updateCompanyMutation.mutate(values)}>
          <div className="grid grid-cols-2 gap-x-4">
            <Form.Item name="name" label="Company Name" rules={[{ required: true }]}>
              <Input />
            </Form.Item>
            <Form.Item name="gstNumber" label="GST Number" rules={[{ required: true }]}>
              <Input className="font-mono" />
            </Form.Item>
            <Form.Item name="billingEmail" label="Billing Email" rules={[{ required: true, type: 'email' }]}>
              <Input />
            </Form.Item>
            <Form.Item name="contactName" label="Contact Name">
              <Input />
            </Form.Item>
            <Form.Item name="phone" label="Phone">
              <Input />
            </Form.Item>
            <Form.Item name="city" label="City">
              <Input />
            </Form.Item>
            <Form.Item name="state" label="State">
              <Input />
            </Form.Item>
            <Form.Item name="pinCode" label="PIN Code">
              <Input />
            </Form.Item>
            <Form.Item name="serviceChargePct" label="Service Charge (%)" rules={[{ required: true }]}>
              <InputNumber className="w-full" min={0} max={100} step={0.5} />
            </Form.Item>
            <Form.Item name="billingCycle" label="Billing Cycle" rules={[{ required: true }]}>
              <Select options={[
                { value: 'MONTHLY', label: 'Monthly' },
                { value: 'BIWEEKLY', label: 'Bi-Weekly' },
                { value: 'WEEKLY', label: 'Weekly' },
              ]} />
            </Form.Item>
            <Form.Item name="address" label="Address" className="col-span-2">
              <Input.TextArea rows={2} />
            </Form.Item>
          </div>
        </Form>
      </Modal>
    </div>
  );
};

export default CompanyDetail;
