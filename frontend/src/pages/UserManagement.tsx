import { toast } from "sonner";
import { useState } from 'react';
import { Card, Table, Button, Input, Tag, Select, Modal, Form, Empty } from 'antd';
import { Search, Plus } from 'lucide-react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '../api/axiosInstance';
import { getStatusTag } from '../lib/statusUtils';

const UserManagement = () => {
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [modalOpen, setModalOpen] = useState(false);
  const [editingUser, setEditingUser] = useState<any>(null);
  const [form] = Form.useForm();
  const queryClient = useQueryClient();

  const { data, isLoading } = useQuery({
    queryKey: ['users', page, size],
    queryFn: async () => {
      const { data } = await api.get(`/admin/users?page=${page}&size=${size}`);
      return data;
    },
  });

  const createUserMutation = useMutation({
    mutationFn: async (values: any) => {
      const { data } = await api.post('/admin/users', values);
      return data;
    },
    onSuccess: () => {
      toast.success('User created successfully');
      queryClient.invalidateQueries({ queryKey: ['users'] });
      setModalOpen(false);
      form.resetFields();
    },
    onError: (err: any) => {
      toast.error(err.response?.data?.message || 'Failed to create user');
    },
  });

  const updateUserMutation = useMutation({
    mutationFn: async ({ id, values }: { id: number; values: any }) => {
      const { data } = await api.put(`/admin/users/${id}`, values);
      return data;
    },
    onSuccess: () => {
      toast.success('User updated successfully');
      queryClient.invalidateQueries({ queryKey: ['users'] });
      setModalOpen(false);
      setEditingUser(null);
      form.resetFields();
    },
    onError: (err: any) => {
      toast.error(err.response?.data?.message || 'Failed to update user');
    },
  });

  const deactivateUserMutation = useMutation({
    mutationFn: async (id: number) => {
      const { data } = await api.put(`/admin/users/${id}`, { isActive: false });
      return data;
    },
    onSuccess: () => {
      toast.success('User deactivated');
      queryClient.invalidateQueries({ queryKey: ['users'] });
    },
  });



  const columns = [
    {
      title: 'Name',
      dataIndex: 'username',
      key: 'username',
      render: (text: string) => <span className="font-medium text-brand-dark">{text}</span>
    },
    {
      title: 'Email',
      dataIndex: 'email',
      key: 'email',
    },
    {
      title: 'Role',
      dataIndex: 'role',
      key: 'role',
      render: (role: string) => {
        const tag = getStatusTag(role);
        return <Tag className={tag.className}>{tag.label}</Tag>;
      }
    },
    {
      title: 'Status',
      dataIndex: 'isActive',
      key: 'status',
      render: (active: boolean) => {
        const tag = getStatusTag(active ? 'ACTIVE' : 'INACTIVE');
        return <Tag className={tag.className}>{tag.label}</Tag>;
      }
    },
    {
      title: 'Actions',
      key: 'actions',
      render: (_: any, record: any) => (
        <div className="flex gap-2">
          <Button type="link" className="text-brand-dark hover:text-brand-gold font-medium px-0 transition-colors" onClick={() => {
            setEditingUser(record);
            form.setFieldsValue({ email: record.email, role: record.role });
            setModalOpen(true);
          }}>Edit</Button>
          {record.isActive && record.username !== 'admin' && (
            <Button type="link" danger className="px-0 font-medium" onClick={() => deactivateUserMutation.mutate(record.id)}>Deactivate</Button>
          )}
        </div>
      )
    }
  ];

  const handleSubmit = (values: any) => {
    if (editingUser) {
      updateUserMutation.mutate({ id: editingUser.id, values: { email: values.email, role: values.role } });
    } else {
      createUserMutation.mutate(values);
    }
  };

  return (
    <div className="space-y-6 max-w-5xl mx-auto animate-fade-in">
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-3xl font-serif text-brand-dark mb-1">User Management</h1>
          <p className="text-gray-500">Manage agency staff access and roles.</p>
        </div>
        <Button 
          type="primary" 
          icon={<Plus size={16} />} 
          className="bg-brand-dark hover:bg-black font-medium shadow-sm hover:shadow-md transition-all hover:-translate-y-0.5" 
          onClick={() => {
            setEditingUser(null);
            form.resetFields();
            setModalOpen(true);
          }}
        >
          Add User
        </Button>
      </div>

      <Card className="min-h-[500px] animate-slide-up" style={{ animationDelay: '100ms', animationFillMode: 'both' }}>
        <div className="flex justify-between items-center mb-6">
          <Input 
            prefix={<Search size={16} className="text-gray-400" />} 
            placeholder="Search users..." 
            className="max-w-md rounded-lg hover:border-brand-dark focus:border-brand-dark transition-colors"
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
          locale={{
            emptyText: <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="No users found" />
          }}
        />
      </Card>

      <Modal
        title={editingUser ? 'Edit User' : 'Create New User'}
        open={modalOpen}
        onCancel={() => { setModalOpen(false); setEditingUser(null); form.resetFields(); }}
        onOk={() => form.submit()}
        confirmLoading={createUserMutation.isPending || updateUserMutation.isPending}
        okButtonProps={{ className: 'bg-brand-dark' }}
      >
        <Form form={form} layout="vertical" onFinish={handleSubmit}>
          {!editingUser && (
            <Form.Item name="username" label="Username" rules={[{ required: true }]}>
              <Input />
            </Form.Item>
          )}
          <Form.Item name="email" label="Email" rules={[{ required: true, type: 'email' }]}>
            <Input />
          </Form.Item>
          {!editingUser && (
            <Form.Item name="password" label="Password" rules={[{ required: true, min: 6 }]}>
              <Input.Password />
            </Form.Item>
          )}
          <Form.Item name="role" label="Role" rules={[{ required: true }]}>
            <Select options={[
              { value: 'ADMIN', label: 'Admin' },
              { value: 'BILLING_STAFF', label: 'Billing Staff' },
              { value: 'VIEWER', label: 'Viewer' },
            ]} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default UserManagement;
