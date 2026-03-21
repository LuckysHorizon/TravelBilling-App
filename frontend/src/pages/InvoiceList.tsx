import { useState } from 'react';
import { Card, Table, Button, Input, Tag, Dropdown, message, Tooltip } from 'antd';
import { Search, Download, Mail, FileSpreadsheet, FileText } from 'lucide-react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import api from '../api/axiosInstance';

const InvoiceList = () => {
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const { data, isLoading } = useQuery({
    queryKey: ['invoices', page, size],
    queryFn: async () => {
      const { data } = await api.get(`/invoices?page=${page}&size=${size}&sort=createdAt,desc`);
      return data;
    },
  });

  const sendEmailMutation = useMutation({
    mutationFn: async (id: number) => {
      const { data } = await api.post(`/invoices/${id}/send-email`);
      return data;
    },
    onSuccess: (data) => {
      message.success(data.message || 'Invoice emailed successfully');
      queryClient.invalidateQueries({ queryKey: ['invoices'] });
    },
    onError: (err: any) => {
      message.error(err.response?.data?.message || 'Failed to send email');
    },
  });

  const markPaidMutation = useMutation({
    mutationFn: async (id: number) => {
      await api.post(`/invoices/${id}/mark-paid`);
    },
    onSuccess: () => {
      message.success('Invoice marked as paid');
      queryClient.invalidateQueries({ queryKey: ['invoices'] });
    },
    onError: (err: any) => {
      message.error(err.response?.data?.message || 'Failed to mark as paid');
    },
  });

  const handleDownloadPdf = (id: number) => {
    window.open(`http://localhost:8080/api/invoices/${id}/download-pdf`, '_blank');
  };

  const handleDownloadExcel = (id: number) => {
    window.open(`http://localhost:8080/api/invoices/${id}/download-excel`, '_blank');
  };

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
          className="font-mono font-semibold text-brand-dark cursor-pointer hover:underline"
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
      title: 'Tickets',
      dataIndex: 'ticketCount',
      key: 'ticketCount',
      render: (count: number) => count || 0,
    },
    {
      title: 'Grand Total',
      dataIndex: 'grandTotal',
      key: 'amount',
      render: (amount: number) => amount != null 
        ? new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' }).format(amount)
        : '—'
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
      width: 200,
      render: (record: any) => (
        <div className="flex items-center gap-1">
          <Tooltip title="Download PDF">
            <Button 
              type="text" 
              icon={<FileText size={15} className="text-gray-500 hover:text-brand-dark" />} 
              onClick={() => handleDownloadPdf(record.id)}
              size="small"
            />
          </Tooltip>
          <Tooltip title="Download Excel">
            <Button 
              type="text" 
              icon={<FileSpreadsheet size={15} className="text-green-600 hover:text-green-800" />} 
              onClick={() => handleDownloadExcel(record.id)}
              size="small"
            />
          </Tooltip>
          {record.status !== 'PAID' && (
            <>
              <Tooltip title="Send Email">
                <Button 
                  type="text" 
                  icon={<Mail size={15} className="text-blue-500 hover:text-blue-700" />} 
                  onClick={() => sendEmailMutation.mutate(record.id)}
                  loading={sendEmailMutation.isPending}
                  size="small"
                />
              </Tooltip>
              <Tooltip title="Mark as Paid">
                <Button 
                  type="link"
                  size="small"
                  className="text-green-600 hover:text-green-800 text-xs p-0"
                  onClick={() => markPaidMutation.mutate(record.id)}
                >
                  Mark Paid
                </Button>
              </Tooltip>
            </>
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
          <p className="text-gray-500">View, download, and manage billed invoices.</p>
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
