import React, { useState } from 'react';
import { Card, Table, Tag, Input, Select, DatePicker } from 'antd';
import { Search, Activity } from 'lucide-react';

const { RangePicker } = DatePicker;

const AuditLogs = () => {
  const [page, setPage] = useState(0);

  // Mock data for audit logs
  const logs = [
    { id: 1, action: 'INVOICE_GENERATED', entityType: 'Invoice', entityId: 'TBP/2023-24/00452', user: 'Admin User', timestamp: '2023-11-20 14:32:00', details: 'Generated invoice for PNR VIST678' },
    { id: 2, action: 'STATUS_UPDATED', entityType: 'Ticket', entityId: '105', user: 'Sarah Connor', timestamp: '2023-11-20 12:15:00', details: 'Status changed from PENDING_REVIEW to APPROVED' },
    { id: 3, action: 'USER_LOGIN', entityType: 'User', entityId: '1', user: 'Admin User', timestamp: '2023-11-20 09:00:00', details: 'Successful login from 192.168.1.100' },
  ];

  const getActionTag = (action: string) => {
    if (action.includes('GENERATED') || action.includes('CREATED')) return <Tag color="success">{action}</Tag>;
    if (action.includes('UPDATED')) return <Tag color="blue">{action}</Tag>;
    if (action.includes('DELETED') || action.includes('FAILED')) return <Tag color="error">{action}</Tag>;
    return <Tag>{action}</Tag>;
  };

  const columns = [
    {
      title: 'Timestamp',
      dataIndex: 'timestamp',
      key: 'timestamp',
      width: 180,
      render: (text: string) => <span className="text-gray-500 text-sm">{text}</span>
    },
    {
      title: 'Action',
      dataIndex: 'action',
      key: 'action',
      width: 180,
      render: (action: string) => getActionTag(action)
    },
    {
      title: 'Entity',
      key: 'entity',
      render: (record: any) => (
        <span className="font-mono text-xs bg-gray-50 px-2 py-1 border border-gray-200 rounded">
          {record.entityType} #{record.entityId}
        </span>
      )
    },
    {
      title: 'User',
      dataIndex: 'user',
      key: 'user',
      width: 150,
      render: (text: string) => <span className="font-medium text-brand-dark">{text}</span>
    },
    {
      title: 'Details',
      dataIndex: 'details',
      key: 'details',
    }
  ];

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-serif text-brand-dark mb-1">Audit Logs</h1>
        <p className="text-gray-500">Immutable record of all system activities and data mutations.</p>
      </div>

      <Card className="min-h-[600px]">
        <div className="flex flex-col sm:flex-row gap-4 mb-6">
          <Input 
            prefix={<Search size={16} className="text-gray-400" />} 
            placeholder="Search details or entity ID..." 
            className="sm:max-w-xs rounded-lg"
          />
          <Select placeholder="Action Type" allowClear className="w-48">
            <Select.Option value="LOGIN">Authentication</Select.Option>
            <Select.Option value="TICKET">Ticket Mutation</Select.Option>
            <Select.Option value="INVOICE">Invoice Generation</Select.Option>
          </Select>
          <RangePicker className="rounded-lg" />
        </div>

        <Table 
          columns={columns} 
          dataSource={logs} 
          rowKey="id"
          size="small"
          pagination={{
            current: page + 1,
            pageSize: 15,
            onChange: (p) => setPage(p - 1),
          }}
        />
      </Card>
    </div>
  );
};

export default AuditLogs;
