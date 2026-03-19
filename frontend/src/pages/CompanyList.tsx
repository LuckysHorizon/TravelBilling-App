import { useState } from 'react';
import { Card, Table, Button, Input, Tag } from 'antd';
import { Search, Plus, Building2, MapPin } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { useCompanies } from '../api/queries';

const CompanyList = () => {
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const { data, isLoading } = useCompanies(page, size);
  const navigate = useNavigate();

  const columns = [
    {
      title: 'Company Name',
      key: 'name',
      render: (record: any) => (
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
      render: (text: string) => <Tag color="processing">{text.replace('_', ' ')}</Tag>
    },
    {
      title: 'Actions',
      key: 'actions',
      render: (record: any) => (
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
    </div>
  );
};

export default CompanyList;
