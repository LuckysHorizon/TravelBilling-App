import { useState } from 'react';
import { Card, Table, Tag, Input, Empty, Popover } from 'antd';
import { Search } from 'lucide-react';
import { useQuery } from '@tanstack/react-query';
import api from '../api/axiosInstance';
import { getStatusTag } from '../lib/statusUtils';

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
    // Map to statusUtils compatible strings
    let status = 'default';
    if (action.includes('GENERATED') || action.includes('CREATED')) status = 'GENERATED';
    else if (action.includes('UPDATED')) status = 'UPDATED';
    else if (action.includes('DELETED') || action.includes('FAILED')) status = 'FAILED';
    else if (action.includes('LOGIN')) status = 'LOGIN';
    else status = action;
    
    const tag = getStatusTag(status);
    return <Tag className={tag.className}>{action}</Tag>;
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
          const prettyText = JSON.stringify(parsed, null, 2);
          const preview = Object.keys(parsed).join(', ');
          
          return (
            <Popover 
              content={<pre className="text-xs text-gray-700 max-w-sm overflow-x-auto p-2 bg-gray-50 rounded border border-gray-100">{prettyText}</pre>} 
              title="Change Details"
              trigger="hover"
            >
              <span className="text-xs text-gray-600 font-mono cursor-pointer hover:text-brand-dark hover:underline bg-gray-50 px-1.5 py-0.5 rounded border border-gray-100 inline-block truncate max-w-[200px]">
                {preview || 'Details'}
              </span>
            </Popover>
          );
        } catch {
          return <span className="text-xs text-gray-600 truncate max-w-[200px] inline-block" title={text}>{text}</span>;
        }
      }
    }
  ];

  return (
    <div className="space-y-6 animate-fade-in">
      <div>
        <h1 className="text-3xl font-serif text-brand-dark mb-1">Audit Logs</h1>
        <p className="text-gray-500">Immutable record of all system activities and data mutations.</p>
      </div>

      <Card className="min-h-[600px] animate-slide-up" style={{ animationDelay: '100ms', animationFillMode: 'both' }}>
        <div className="flex flex-col sm:flex-row gap-4 mb-6">
          <Input 
            prefix={<Search size={16} className="text-gray-400" />} 
            placeholder="Search details or entity ID..." 
            className="sm:max-w-xs rounded-lg hover:border-brand-dark focus:border-brand-dark transition-colors"
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
          locale={{
            emptyText: <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="No audit logs found" />
          }}
        />
      </Card>
    </div>
  );
};

export default AuditLogs;
