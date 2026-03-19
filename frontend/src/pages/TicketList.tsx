import { useState } from 'react';
import { Card, Table, Button, Input, Tag, Select } from 'antd';
import { Search, Plus, Filter } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { useTickets } from '../api/queries';

const TicketList = () => {
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [statusFilter, setStatusFilter] = useState<string | undefined>(undefined);
  const { data, isLoading } = useTickets(page, size, statusFilter);
  const navigate = useNavigate();

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
        <Button 
          type="link" 
          onClick={() => {
            if (record.status === 'PENDING_REVIEW') {
              navigate(`/tickets/review/${record.batchId}`);
            } else {
              // open detail modal or page
            }
          }}
          className="text-brand-accent hover:text-blue-700"
        >
          {record.status === 'PENDING_REVIEW' ? 'Review & Confirm' : 'View'}
        </Button>
      )
    }
  ];

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-3xl font-serif text-brand-dark mb-1">Tickets</h1>
          <p className="text-gray-500">Manage parsed tickets and review pending extractions.</p>
        </div>
        <Button 
          type="primary" 
          icon={<Plus size={16} />} 
          className="bg-brand-dark hover:bg-black font-medium"
          onClick={() => navigate('/tickets/upload')}
        >
          Upload Tickets
        </Button>
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
