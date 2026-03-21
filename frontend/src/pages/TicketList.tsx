import { useState } from 'react';
import { Card, Table, Button, Input, Tag, Select, Popconfirm, message } from 'antd';
import { Search, Plus, Filter, Trash2 } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { useTickets } from '../api/queries';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import api from '../api/axiosInstance';

const TicketList = () => {
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [statusFilter, setStatusFilter] = useState<string | undefined>(undefined);
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);
  const { data, isLoading } = useTickets(page, size, statusFilter);
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const deleteMutation = useMutation({
    mutationFn: async (id: number) => {
      await api.delete(`/tickets/${id}`);
    },
    onSuccess: () => {
      message.success('Ticket deleted successfully');
      queryClient.invalidateQueries({ queryKey: ['tickets'] });
    },
    onError: (err: any) => {
      message.error(err.response?.data?.message || 'Failed to delete ticket');
    },
  });

  const batchDeleteMutation = useMutation({
    mutationFn: async (ids: number[]) => {
      const { data } = await api.post('/tickets/batch-delete', { ids });
      return data;
    },
    onSuccess: (data) => {
      message.success(`Deleted ${data.deleted} of ${data.total} tickets`);
      setSelectedRowKeys([]);
      queryClient.invalidateQueries({ queryKey: ['tickets'] });
    },
    onError: (err: any) => {
      message.error(err.response?.data?.message || 'Failed to delete tickets');
    },
  });

  const getStatusTag = (status: string) => {
    switch(status) {
      case 'QUEUED': return <Tag color="default">Queued</Tag>;
      case 'EXTRACTING': return <Tag color="processing">Extracting</Tag>;
      case 'EXTRACTION_FAILED': return <Tag color="error">Failed</Tag>;
      case 'PENDING_REVIEW': return <Tag color="warning">Pending Review</Tag>;
      case 'APPROVED': return <Tag color="blue">Approved</Tag>;
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
      render: (text: string) => <span className="font-mono font-medium text-brand-dark">{text || 'PENDING'}</span>
    },
    {
      title: 'Passenger',
      dataIndex: 'passengerName',
      key: 'passenger',
      render: (text: string) => text || '—'
    },
    {
      title: 'Company',
      dataIndex: 'companyName',
      key: 'company',
      render: (text: string) => <span className="font-medium">{text}</span>
    },
    {
      title: 'Date',
      dataIndex: 'travelDate',
      key: 'date',
      render: (text: string) => text || '—'
    },
    {
      title: 'Base Fare',
      dataIndex: 'baseFare',
      key: 'baseFare',
      render: (amount: number) => amount ? new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' }).format(amount) : '—'
    },
    {
      title: 'Grand Total',
      dataIndex: 'totalAmount',
      key: 'amount',
      render: (amount: number) => amount ? new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' }).format(amount) : '—'
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
          <Button 
            type="link" 
            onClick={() => navigate(`/tickets/review/${record.id}`)}
            className="text-brand-accent hover:text-blue-700 p-0"
          >
            {record.status === 'PENDING_REVIEW' ? 'Review' : 'View'}
          </Button>
          {(record.status !== 'BILLED' && record.status !== 'PAID') && (
            <Popconfirm
              title="Delete Ticket"
              description="Are you sure you want to delete this ticket?"
              onConfirm={() => deleteMutation.mutate(record.id)}
              okText="Yes, Delete"
              cancelText="Cancel"
              okButtonProps={{ danger: true }}
            >
              <Button 
                type="text" 
                danger 
                icon={<Trash2 size={14} />}
                size="small"
                loading={deleteMutation.isPending}
              />
            </Popconfirm>
          )}
        </div>
      )
    }
  ];

  const rowSelection = {
    selectedRowKeys,
    onChange: (keys: React.Key[]) => setSelectedRowKeys(keys),
    getCheckboxProps: (record: any) => ({
      disabled: record.status === 'BILLED' || record.status === 'PAID',
    }),
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-3xl font-serif text-brand-dark mb-1">Tickets</h1>
          <p className="text-gray-500">Manage parsed tickets and review pending extractions.</p>
        </div>
        <div className="flex gap-3">
          {selectedRowKeys.length > 0 && (
            <Popconfirm
              title={`Delete ${selectedRowKeys.length} Tickets`}
              description={`Are you sure you want to delete ${selectedRowKeys.length} selected tickets?`}
              onConfirm={() => batchDeleteMutation.mutate(selectedRowKeys.map(Number))}
              okText="Yes, Delete All"
              cancelText="Cancel"
              okButtonProps={{ danger: true }}
            >
              <Button 
                danger
                icon={<Trash2 size={16} />}
                loading={batchDeleteMutation.isPending}
              >
                Delete {selectedRowKeys.length} Selected
              </Button>
            </Popconfirm>
          )}
          <Button 
            type="primary" 
            icon={<Plus size={16} />} 
            className="bg-brand-dark hover:bg-black font-medium"
            onClick={() => navigate('/tickets/upload')}
          >
            Upload Tickets
          </Button>
        </div>
      </div>

      <Card className="min-h-[500px]">
        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-6">
          <Input 
            prefix={<Search className="text-gray-400" size={16} />} 
            placeholder="Search tickets by PNR or Passenger..." 
            className="max-w-sm rounded-lg"
          />
          <div className="flex items-center gap-2 w-full sm:w-auto">
            <Filter size={16} className="text-gray-400" />
            <Select
              className="w-full sm:w-48"
              placeholder="Filter by Status"
              allowClear
              value={statusFilter}
              onChange={(val) => {
                setStatusFilter(val);
                setPage(0);
              }}
              options={[
                { value: 'PENDING_REVIEW', label: 'Pending Review' },
                { value: 'APPROVED', label: 'Approved' },
                { value: 'BILLED', label: 'Billed' },
                { value: 'PAID', label: 'Paid' },
                { value: 'EXTRACTION_FAILED', label: 'Failed' },
              ]}
            />
          </div>
        </div>

        <Table 
          columns={columns} 
          dataSource={data?.content} 
          rowKey="id"
          loading={isLoading}
          rowSelection={rowSelection}
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

export default TicketList;
