import { useState } from 'react';
import { Card, Table, Button, Input, Tag, Modal, Form, Select, InputNumber, message } from 'antd';
import { Search, Plus, Building2, MapPin } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { useCompanies } from '../api/queries';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import api from '../api/axiosInstance';

const CompanyList = () => {
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [modalOpen, setModalOpen] = useState(false);
  const [form] = Form.useForm();
  const { data, isLoading } = useCompanies(page, size);
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const createCompanyMutation = useMutation({
    mutationFn: async (values: any) => {
      const { data } = await api.post('/companies', values);
      return data;
    },
    onSuccess: () => {
      message.success('Company created successfully');
      queryClient.invalidateQueries({ queryKey: ['companies'] });
      setModalOpen(false);
      form.resetFields();
    },
    onError: (err: any) => {
      message.error(err.response?.data?.message || 'Failed to create company');
    },
  });

  const columns = [
    {
      title: 'Company Name',
      key: 'name',
      render: (_: any, record: any) => (
        <div className="flex items-center gap-3">
          <div className="bg-brand-paper p-2 rounded-lg text-brand-dark">
            <Building2 size={16} />
          </div>
          <div>
            <div className="font-semibold text-brand-dark">{record.name}</div>
            <div className="text-xs text-gray-500">Contact: {record.contactName || 'N/A'}</div>
          </div>
        </div>
      )
    },
    {
      title: 'GSTIN',
      dataIndex: 'gstNumber',
      key: 'gst',
      render: (text: string) => <span className="font-mono bg-gray-50 px-2 py-1 rounded text-sm border border-gray-200">{text}</span>
    },
    {
      title: 'City',
      dataIndex: 'city',
      key: 'city',
      render: (text: string) => (
        <span className="flex items-center gap-1 text-gray-600">
          <MapPin size={14} /> {text || 'N/A'}
        </span>
      )
    },
    {
      title: 'Status',
      dataIndex: 'active',
      key: 'status',
      render: (active: boolean) => active ? <Tag color="success">Active</Tag> : <Tag color="error">Inactive</Tag>
    },
    {
      title: 'Billing Cycle',
      dataIndex: 'billingCycle',
      key: 'cycle',
      render: (text: string) => text ? <Tag color="processing">{text.replace('_', ' ')}</Tag> : '—'
    },
    {
      title: 'Actions',
      key: 'actions',
      render: (_: any, record: any) => (
        <Button 
          type="link" 
          onClick={() => navigate(`/companies/${record.id}`)}
          className="text-brand-accent hover:text-blue-700"
        >
          View Details
        </Button>
      )
    }
  ];

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-3xl font-serif text-brand-dark mb-1">Companies</h1>
          <p className="text-gray-500">Manage client organizations and billing settings.</p>
        </div>
        <Button 
          type="primary" 
          icon={<Plus size={16} />} 
          className="bg-brand-dark hover:bg-black font-medium"
          onClick={() => { form.resetFields(); setModalOpen(true); }}
        >
          Add Company
        </Button>
      </div>

      <Card className="min-h-[500px]">
        <div className="flex justify-between items-center mb-6">
          <Input 
            prefix={<Search className="text-gray-400" size={16} />} 
            placeholder="Search companies by name or GSTIN..." 
            className="max-w-md rounded-lg"
          />
        </div>

        <Table 
          columns={columns} 
          dataSource={data?.content} 
          rowKey="id"
          loading={isLoading}
          pagination={{
            current: page + 1,
            pageSize: size,
            total: data?.totalElements,
            onChange: (p, s) => {
              setPage(p - 1);
              setSize(s);
            },
            showSizeChanger: true
          }}
        />
      </Card>

      <Modal
        title="Add New Company"
        open={modalOpen}
        onCancel={() => { setModalOpen(false); form.resetFields(); }}
        onOk={() => form.submit()}
        confirmLoading={createCompanyMutation.isPending}
        okButtonProps={{ className: 'bg-brand-dark' }}
        width={600}
      >
        <Form form={form} layout="vertical" onFinish={(values) => createCompanyMutation.mutate(values)}>
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
            <Form.Item name="pdfStoragePath" label="Custom PDF Storage Path" className="col-span-2" tooltip="Absolute path on the server where PDFs will be stored for this organization. Leave blank to use default system storage.">
              <Input placeholder="e.g. C:\travelbill\pdfs\company1" />
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

export default CompanyList;
