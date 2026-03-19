import React, { useState } from 'react';
import { Card, Table, Button, Input, Tag, Select } from 'antd';
import { Search, Plus, UserCog } from 'lucide-react';

const UserManagement = () => {
  const [page, setPage] = useState(0);

  // Mock data since API is not fully hooked up here yet
  const users = [
    { id: 1, name: 'Admin User', email: 'admin@travelbillpro.com', role: 'ADMIN', active: true },
    { id: 2, name: 'Sarah Connor', email: 'sarah@travelbillpro.com', role: 'BILLING_STAFF', active: true },
    { id: 3, name: 'John Smith', email: 'john@travelbillpro.com', role: 'VIEWER', active: false },
  ];

  const getRoleTag = (role: string) => {
    switch(role) {
      case 'ADMIN': return <Tag color="red">Admin</Tag>;
      case 'BILLING_STAFF': return <Tag color="blue">Billing</Tag>;
      case 'VIEWER': return <Tag color="default">Viewer</Tag>;
      default: return <Tag>{role}</Tag>;
    }
  };

  const columns = [
    {
      title: 'Name',
      dataIndex: 'name',
      key: 'name',
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
      render: (role: string) => getRoleTag(role)
    },
    {
      title: 'Status',
      dataIndex: 'active',
      key: 'status',
      render: (active: boolean) => active ? <Tag color="success">Active</Tag> : <Tag color="error">Deactivated</Tag>
    },
    {
      title: 'Actions',
      key: 'actions',
      render: () => (
        <div className="flex gap-2">
          <Button type="link" className="text-brand-accent px-0">Edit</Button>
          <Button type="link" danger className="px-0">Deactivate</Button>
        </div>
      )
    }
  ];

  return (
    <div className="space-y-6 max-w-5xl mx-auto">
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-3xl font-serif text-brand-dark mb-1">User Management</h1>
          <p className="text-gray-500">Manage agency staff access and roles.</p>
        </div>
        <Button type="primary" icon={<Plus size={16} />} className="bg-brand-dark">
          Add User
        </Button>
      </div>

      <Card className="min-h-[500px]">
        <div className="flex justify-between items-center mb-6">
          <Input 
            prefix={<Search size={16} className="text-gray-400" />} 
            placeholder="Search users..." 
            className="max-w-md rounded-lg"
          />
          <Select defaultValue="all" className="w-40">
            <Select.Option value="all">All Roles</Select.Option>
            <Select.Option value="ADMIN">Admin</Select.Option>
            <Select.Option value="BILLING_STAFF">Billing</Select.Option>
          </Select>
        </div>

        <Table 
          columns={columns} 
          dataSource={users} 
          rowKey="id"
          pagination={{
            current: page + 1,
            pageSize: 10,
            onChange: (p) => setPage(p - 1),
          }}
        />
      </Card>
    </div>
  );
};

export default UserManagement;
