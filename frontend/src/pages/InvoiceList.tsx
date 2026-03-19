import { useState } from 'react';
import { Card, Table, Button, Input, Tag } from 'antd';
import { Search, Download, Mail, Filter } from 'lucide-react';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import api from '../api/axiosInstance';

const InvoiceList = () => {
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const navigate = useNavigate();

  const { data, isLoading } = useQuery({
    queryKey: ['invoices', page, size],
    queryFn: async () => {
      const { data } = await api.get(`/invoices?page=${page}&size=${size}`);
      return data;
    },
  });

  const getStatusTag = (status: string) => {
    switch(status) {
      case 'DRAFT': return <Tag color="default">Draft</Tag>;
      case 'GENERATED': return <Tag color="processing">Generated</Tag>;
      case 'SENT': return <Tag color="blue">Sent</Tag>;
      case 'PAID': return <Tag color="success">Paid</Tag>;
      case 'CANCELLED': return <Tag color="error">Cancelled</Tag>;
      default: return <Tag color="default">{status}</Tag>;
    }
  };

  const columns = [
    {
      title: 'Invoice Number',
      dataIndex: 'invoiceNumber',
      key: 'invNum',
      render: (text: string, record: any) => (
        <a 
          onClick={() => navigate(`/invoices/${record.id}`)}
          className="font-mono font-semibold text-brand-dark"
        >
          {text}
        </a>
      )
    },
    {
      title: 'Company',
      dataIndex: 'companyName',
      key: 'company',
      render: (text: string) => <span className="font-medium">{text}</span>
    },
    {
      title: 'Date',
      dataIndex: 'invoiceDate',
      key: 'date',
    },
    {
      title: 'Grand Total',
      dataIndex: 'grandTotal',
      key: 'amount',
      render: (amount: number) => new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' }).format(amount)
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => getStatusTag(status)
    },
    {
      title: 'Actions',
      key: 'actions',
      render: (record: any) => (
        <div className="flex items-center gap-2">
           <Button type="text" title="Download PDF" icon={<Download size={16} className="text-gray-500 hover:text-brand-dark" />} />
           {record.status !== 'PAID' && (
             <Button type="text" title="Send Email" icon={<Mail size={16} className="text-gray-500 hover:text-blue-600" />} />
           )}
        </div>
      )
    }
  ];

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-3xl font-serif text-brand-dark mb-1">Invoices</h1>
          <p className="text-gray-500">View and manage billed invoices.</p>
        </div>
      </div>

      <Card className="min-h-[500px]">
        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-6">
          <Input 
            prefix={<Search className="text-gray-400" size={16} />} 
            placeholder="Search by Invoice Number..." 
            className="max-w-sm rounded-lg"
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

export default InvoiceList;
