import React, { useState } from 'react';
import { Card, Table, Tag, Input, Select, DatePicker } from 'antd';
import { Search } from 'lucide-react';
import { useQuery } from '@tanstack/react-query';
import api from '../api/axiosInstance';

const { RangePicker } = DatePicker;

const AuditLogs = () => {
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(15);

  const { data, isLoading } = useQuery({
    queryKey: ['auditLogs', page, size],
    queryFn: async () => {
      const { data } = await api.get(`/admin/audit-logs?page=${page}&size=${size}&sort=createdAt,desc`);
      return data;
    },
  });

  const getActionTag = (action: string) => {
    if (action.includes('GENERATED') || action.includes('CREATED')) return <Tag color="success">{action}</Tag>;
    if (action.includes('UPDATED')) return <Tag color="blue">{action}</Tag>;
    if (action.includes('DELETED') || action.includes('FAILED')) return <Tag color="error">{action}</Tag>;
    if (action.includes('LOGIN')) return <Tag color="default">{action}</Tag>;
    return <Tag>{action}</Tag>;
  };

  const columns = [
    {
      title: 'Timestamp',
      dataIndex: 'timestamp',
      key: 'timestamp',
      width: 180,
      render: (text: string) => <span className="text-gray-500 text-sm">{text ? new Date(text).toLocaleString() : '—'}</span>
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
      render: (_: any, record: any) => (
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
      ellipsis: true,
      render: (text: string) => {
        if (!text) return '—';
        try {
          const parsed = JSON.parse(text);
          return <span className="text-xs text-gray-600">{JSON.stringify(parsed).substring(0, 120)}...</span>;
        } catch {
          return <span className="text-xs text-gray-600">{text.substring(0, 120)}</span>;
        }
      }
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
        </div>

        <Table 
          columns={columns} 
          dataSource={data?.content} 
          rowKey="id"
          size="small"
          loading={isLoading}
          pagination={{
            current: page + 1,
            pageSize: size,
            total: data?.totalElements,
            onChange: (p, s) => {
              setPage(p - 1);
              setSize(s);
            },
            showSizeChanger: true,
          }}
        />
      </Card>
    </div>
  );
};

export default AuditLogs;
