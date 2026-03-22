import { useState, useEffect, useCallback } from 'react';
import { Card, Table, Button, Modal, Form, Input, Tag, Space, Typography, Tooltip, message, Popconfirm, Result } from 'antd';
import { Plus, Building2, Globe, Database, Check, X, Pause, Play, TestTube, RefreshCw, Trash2 } from 'lucide-react';
import api from '../api/axiosInstance';

const { Title, Text, Paragraph } = Typography;

interface Organization {
  id: number;
  name: string;
  slug: string;
  dbUrl: string;
  adminEmail: string;
  status: 'PROVISIONING' | 'ACTIVE' | 'SUSPENDED' | 'FAILED';
  planTier: string;
  provisioningLog: string | null;
  createdAt: string;
}

const statusColors: Record<string, string> = {
  ACTIVE: 'green',
  PROVISIONING: 'blue',
  SUSPENDED: 'orange',
  FAILED: 'red',
};

const SuperAdmin = () => {
  const [orgs, setOrgs] = useState<Organization[]>([]);
  const [loading, setLoading] = useState(false);
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [logModal, setLogModal] = useState<{ open: boolean; log: string }>({ open: false, log: '' });
  const [testResult, setTestResult] = useState<{ success?: boolean; message?: string } | null>(null);
  const [creating, setCreating] = useState(false);
  const [form] = Form.useForm();

  const fetchOrgs = useCallback(async () => {
    setLoading(true);
    try {
      const { data } = await api.get('/super-admin/organizations');
      setOrgs(data);
    } catch { message.error('Failed to load organizations'); }
    setLoading(false);
  }, []);

  useEffect(() => { fetchOrgs(); }, [fetchOrgs]);

  const handleCreate = async (values: any) => {
    setCreating(true);
    try {
      await api.post('/super-admin/organizations', values);
      message.success(`Organization "${values.name}" created and provisioned!`);
      setCreateModalOpen(false);
      form.resetFields();
      fetchOrgs();
    } catch (err: any) {
      message.error(err?.response?.data?.message || 'Failed to create organization');
    }
    setCreating(false);
  };

  const handleTestConnection = async () => {
    const dbUrl = form.getFieldValue('dbUrl');
    if (!dbUrl) { message.warning('Enter a DB URL first'); return; }
    setTestResult(null);
    try {
      const { data } = await api.post('/super-admin/test-connection', { dbUrl });
      setTestResult(data);
    } catch {
      setTestResult({ success: false, message: 'Connection test failed' });
    }
  };

  const handleSuspend = async (id: number) => {
    try {
      await api.put(`/super-admin/organizations/${id}/suspend`);
      message.success('Organization suspended');
      fetchOrgs();
    } catch (err: any) {
      message.error(err?.response?.data?.message || 'Failed to suspend');
    }
  };

  const handleActivate = async (id: number) => {
    try {
      await api.put(`/super-admin/organizations/${id}/activate`);
      message.success('Organization activated');
      fetchOrgs();
    } catch (err: any) {
      message.error(err?.response?.data?.message || 'Failed to activate');
    }
  };

  const handleDelete = async (id: number, name: string) => {
    try {
      await api.delete(`/super-admin/organizations/${id}`);
      message.success(`Organization "${name}" deleted successfully`);
      fetchOrgs();
    } catch (err: any) {
      message.error(err?.response?.data?.message || 'Failed to delete organization');
    }
  };

  const columns = [
    {
      title: 'Organization',
      dataIndex: 'name',
      key: 'name',
      render: (name: string, record: Organization) => (
        <div>
          <Text strong>{name}</Text>
          <div><Text type="secondary" style={{ fontSize: 12 }}>/{record.slug}</Text></div>
        </div>
      ),
    },
    {
      title: 'Admin Email',
      dataIndex: 'adminEmail',
      key: 'adminEmail',
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => <Tag color={statusColors[status] || 'default'}>{status}</Tag>,
    },
    {
      title: 'Plan',
      dataIndex: 'planTier',
      key: 'planTier',
      render: (plan: string) => <Tag>{plan || 'STANDARD'}</Tag>,
    },
    {
      title: 'Created',
      dataIndex: 'createdAt',
      key: 'createdAt',
      render: (d: string) => d ? new Date(d).toLocaleDateString() : '-',
    },
    {
      title: 'Actions',
      key: 'actions',
      render: (_: any, record: Organization) => (
        <Space size="small">
          {record.provisioningLog && (
            <Tooltip title="View log">
              <Button size="small" icon={<Database size={14} />}
                onClick={() => setLogModal({ open: true, log: record.provisioningLog || '' })} />
            </Tooltip>
          )}
          {record.status === 'ACTIVE' && record.slug !== 'default' && (
            <Popconfirm title="Suspend this organization?" onConfirm={() => handleSuspend(record.id)}>
              <Tooltip title="Suspend"><Button size="small" danger icon={<Pause size={14} />} /></Tooltip>
            </Popconfirm>
          )}
          {record.status === 'SUSPENDED' && (
            <Tooltip title="Activate">
              <Button size="small" type="primary" icon={<Play size={14} />} onClick={() => handleActivate(record.id)} />
            </Tooltip>
          )}
          {record.slug !== 'default' && (
            <Popconfirm
              title="Delete this organization?"
              description="This will drop ALL tables in the tenant database and remove all associated users. This action cannot be undone."
              onConfirm={() => handleDelete(record.id, record.name)}
              okText="Yes, Delete"
              okButtonProps={{ danger: true }}
            >
              <Tooltip title="Delete">
                <Button size="small" danger icon={<Trash2 size={14} />} />
              </Tooltip>
            </Popconfirm>
          )}
        </Space>
      ),
    },
  ];

  return (
    <div style={{ padding: 24, maxWidth: 1200, margin: '0 auto' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <Globe size={28} color="#722ed1" />
          <Title level={3} style={{ margin: 0, color: '#722ed1' }}>Super Admin Console</Title>
        </div>
        <Space>
          <Button icon={<RefreshCw size={14} />} onClick={fetchOrgs} loading={loading}>Refresh</Button>
          <Button type="primary" icon={<Plus size={16} />} onClick={() => setCreateModalOpen(true)}
            style={{ background: '#722ed1', borderColor: '#722ed1' }}>
            New Organization
          </Button>
        </Space>
      </div>

      {/* Stats */}
      <div style={{ display: 'flex', gap: 16, marginBottom: 20 }}>
        {[
          { label: 'Total Orgs', value: orgs.length, color: '#722ed1' },
          { label: 'Active', value: orgs.filter(o => o.status === 'ACTIVE').length, color: '#52c41a' },
          { label: 'Suspended', value: orgs.filter(o => o.status === 'SUSPENDED').length, color: '#faad14' },
          { label: 'Failed', value: orgs.filter(o => o.status === 'FAILED').length, color: '#ff4d4f' },
        ].map((stat, i) => (
          <Card key={i} size="small" style={{ flex: 1, borderTop: `3px solid ${stat.color}` }}>
            <Text type="secondary" style={{ fontSize: 12 }}>{stat.label}</Text>
            <div style={{ fontSize: 28, fontWeight: 'bold', color: stat.color }}>{stat.value}</div>
          </Card>
        ))}
      </div>

      <Card title={<><Building2 size={16} style={{ marginRight: 8 }} />Organizations</>}>
        <Table dataSource={orgs} columns={columns} rowKey="id" loading={loading}
          pagination={false} size="middle" />
      </Card>

      {/* Create Org Modal */}
      <Modal title="Create New Organization" open={createModalOpen}
        onCancel={() => { setCreateModalOpen(false); form.resetFields(); setTestResult(null); }}
        footer={null} width={560}>
        <Form form={form} layout="vertical" onFinish={handleCreate}>
          <Form.Item name="name" label="Organization Name" rules={[{ required: true }]}>
            <Input placeholder="Acme Corp" />
          </Form.Item>
          <Form.Item name="slug" label="Slug (URL identifier)" rules={[{ required: true, pattern: /^[a-z0-9-]+$/, message: 'Lowercase letters, numbers, hyphens only' }]}>
            <Input placeholder="acme-corp" addonBefore="/" />
          </Form.Item>
          <Form.Item name="dbUrl" label="Supabase Database URL" rules={[{ required: true }]}
            extra={
              <div style={{ marginTop: 4 }}>
                <Text type="secondary" style={{ fontSize: 11 }}>Format: postgresql://user:pass@host:port/dbname</Text>
                {testResult && (
                  <Tag color={testResult.success ? 'green' : 'red'} style={{ marginLeft: 8 }}>
                    {testResult.success ? '✓ Connected' : '✗ ' + testResult.message}
                  </Tag>
                )}
              </div>
            }>
            <Input.Search placeholder="postgresql://postgres:password@db.xxx.supabase.co:5432/postgres"
              enterButton={<><TestTube size={14} /> Test</>} onSearch={handleTestConnection} />
          </Form.Item>

          <div style={{ display: 'flex', gap: 16 }}>
            <Form.Item name="adminUsername" label="Org Admin Username" rules={[{ required: true }]} style={{ flex: 1 }}>
              <Input placeholder="orgadmin" />
            </Form.Item>
            <Form.Item name="adminPassword" label="Org Admin Password" rules={[{ required: true, min: 6 }]} style={{ flex: 1 }}>
              <Input.Password placeholder="••••••" />
            </Form.Item>
          </div>
          <Form.Item name="adminEmail" label="Org Admin Email" rules={[{ required: true, type: 'email' }]}>
            <Input placeholder="admin@acmecorp.com" />
          </Form.Item>

          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 8, marginTop: 16 }}>
            <Button onClick={() => { setCreateModalOpen(false); form.resetFields(); }}>Cancel</Button>
            <Button type="primary" htmlType="submit" loading={creating}
              style={{ background: '#722ed1', borderColor: '#722ed1' }}>
              Create & Provision
            </Button>
          </div>
        </Form>
      </Modal>

      {/* Provisioning Log Modal */}
      <Modal title="Provisioning Log" open={logModal.open}
        onCancel={() => setLogModal({ open: false, log: '' })} footer={null} width={600}>
        <pre style={{ background: '#1a1a2e', color: '#0f0', padding: 16, borderRadius: 8, maxHeight: 400, overflow: 'auto', fontSize: 12, lineHeight: 1.6, fontFamily: 'monospace' }}>
          {logModal.log || 'No log available'}
        </pre>
      </Modal>
    </div>
  );
};

export default SuperAdmin;
