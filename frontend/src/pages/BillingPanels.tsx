import { useState } from 'react';
import { Card, Table, Button, Modal, Form, Input, Select, Tag, Statistic, Empty, Popconfirm, message } from 'antd';
import { Plus, FileText, Trash2, Receipt } from 'lucide-react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '../api/axiosInstance';

const BillingPanels = () => {
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [addTicketsModalOpen, setAddTicketsModalOpen] = useState(false);
  const [selectedPanelId, setSelectedPanelId] = useState<number | null>(null);
  const [selectedTicketIds, setSelectedTicketIds] = useState<React.Key[]>([]);
  const [form] = Form.useForm();
  const queryClient = useQueryClient();

  // Fetch all panels
  const { data: panels, isLoading } = useQuery({
    queryKey: ['billing-panels'],
    queryFn: async () => {
      const { data } = await api.get('/billing-panels');
      return data;
    },
  });

  // Fetch companies for the create modal
  const { data: companiesData } = useQuery({
    queryKey: ['companies', 0, 100],
    queryFn: async () => {
      const { data } = await api.get('/companies?page=0&size=100');
      return data;
    },
  });

  // Fetch approved unassigned tickets for a company (for adding to panel)
  const { data: availableTickets, isLoading: ticketsLoading } = useQuery({
    queryKey: ['available-tickets', selectedPanelId],
    queryFn: async () => {
      if (!selectedPanelId) return { content: [] };
      const panel = panels?.find((p: any) => p.id === selectedPanelId);
      if (!panel) return { content: [] };
      const { data } = await api.get(`/tickets/company/${panel.companyId}?page=0&size=100&sort=travelDate,desc`);
      // Filter client-side: only APPROVED + not assigned to a panel
      const filtered = data.content?.filter((t: any) => 
        t.status === 'APPROVED' && !panels?.some((p: any) => p.tickets?.some((pt: any) => pt.id === t.id))
      ) || [];
      return { content: filtered };
    },
    enabled: !!selectedPanelId && addTicketsModalOpen,
  });

  const createMutation = useMutation({
    mutationFn: async (values: any) => {
      const { data } = await api.post('/billing-panels', values);
      return data;
    },
    onSuccess: () => {
      message.success('Billing panel created');
      queryClient.invalidateQueries({ queryKey: ['billing-panels'] });
      setCreateModalOpen(false);
      form.resetFields();
    },
    onError: (err: any) => message.error(err.response?.data?.message || 'Failed to create panel'),
  });

  const addTicketsMutation = useMutation({
    mutationFn: async ({ panelId, ticketIds }: { panelId: number; ticketIds: number[] }) => {
      const { data } = await api.post(`/billing-panels/${panelId}/tickets`, { ticketIds });
      return data;
    },
    onSuccess: () => {
      message.success('Tickets added to panel');
      queryClient.invalidateQueries({ queryKey: ['billing-panels'] });
      setAddTicketsModalOpen(false);
      setSelectedTicketIds([]);
    },
    onError: (err: any) => message.error(err.response?.data?.message || 'Failed to add tickets'),
  });

  const removeTicketMutation = useMutation({
    mutationFn: async ({ panelId, ticketId }: { panelId: number; ticketId: number }) => {
      const { data } = await api.delete(`/billing-panels/${panelId}/tickets/${ticketId}`);
      return data;
    },
    onSuccess: () => {
      message.success('Ticket removed from panel');
      queryClient.invalidateQueries({ queryKey: ['billing-panels'] });
    },
    onError: (err: any) => message.error(err.response?.data?.message || 'Failed to remove ticket'),
  });

  const generateInvoiceMutation = useMutation({
    mutationFn: async (panelId: number) => {
      const { data } = await api.post(`/billing-panels/${panelId}/generate-invoice`);
      return data;
    },
    onSuccess: () => {
      message.success('Invoice generated from panel!');
      queryClient.invalidateQueries({ queryKey: ['billing-panels'] });
      queryClient.invalidateQueries({ queryKey: ['invoices'] });
    },
    onError: (err: any) => message.error(err.response?.data?.message || 'Failed to generate invoice'),
  });

  const deletePanelMutation = useMutation({
    mutationFn: async (panelId: number) => {
      await api.delete(`/billing-panels/${panelId}`);
    },
    onSuccess: () => {
      message.success('Panel deleted');
      queryClient.invalidateQueries({ queryKey: ['billing-panels'] });
    },
    onError: (err: any) => message.error(err.response?.data?.message || 'Failed to delete panel'),
  });

  const formatCurrency = (amount: number) => 
    new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' }).format(amount || 0);

  const getStatusTag = (status: string) => {
    switch(status) {
      case 'OPEN': return <Tag color="green">Open</Tag>;
      case 'CLOSED': return <Tag color="default">Closed</Tag>;
      case 'INVOICED': return <Tag color="blue">Invoiced</Tag>;
      default: return <Tag>{status}</Tag>;
    }
  };

  const ticketColumns = [
    { title: 'Date', dataIndex: 'travelDate', key: 'date' },
    { title: 'PNR', dataIndex: 'pnrNumber', key: 'pnr', render: (t: string) => <span className="font-mono">{t}</span> },
    { title: 'Passenger', dataIndex: 'passengerName', key: 'passenger' },
    { title: 'Route', key: 'route', render: (r: any) => `${r.origin || ''} → ${r.destination || ''}` },
    { title: 'Base Fare', dataIndex: 'baseFare', key: 'fare', render: (a: number) => formatCurrency(a) },
  ];

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-3xl font-serif text-brand-dark mb-1">Billing Panels</h1>
          <p className="text-gray-500">Group approved tickets and generate consolidated invoices.</p>
        </div>
        <Button
          type="primary"
          icon={<Plus size={16} />}
          className="bg-brand-dark hover:bg-black font-medium"
          onClick={() => setCreateModalOpen(true)}
        >
          New Panel
        </Button>
      </div>

      {/* Panel Cards */}
      {isLoading ? (
        <Card loading />
      ) : !panels || panels.length === 0 ? (
        <Card>
          <Empty description="No billing panels yet. Create one to start grouping tickets." />
        </Card>
      ) : (
        <div className="space-y-4">
          {panels.map((panel: any) => (
            <Card
              key={panel.id}
              title={
                <div className="flex items-center gap-3">
                  <Receipt size={18} className="text-brand-dark" />
                  <span className="text-lg font-serif">{panel.label}</span>
                  {getStatusTag(panel.status)}
                </div>
              }
              extra={
                <div className="flex items-center gap-2">
                  <span className="text-sm text-gray-500 mr-2">{panel.companyName}</span>
                  {panel.status === 'OPEN' && (
                    <>
                      <Button size="small" onClick={() => { setSelectedPanelId(panel.id); setAddTicketsModalOpen(true); }}>
                        + Add Tickets
                      </Button>
                      <Popconfirm
                        title="Generate Invoice"
                        description="This will create an invoice from all tickets in this panel. Continue?"
                        onConfirm={() => generateInvoiceMutation.mutate(panel.id)}
                        okText="Generate"
                        okButtonProps={{ className: 'bg-brand-dark' }}
                      >
                        <Button type="primary" size="small" className="bg-brand-dark" loading={generateInvoiceMutation.isPending}>
                          Generate Invoice
                        </Button>
                      </Popconfirm>
                      <Popconfirm
                        title="Delete Panel"
                        description="This will unlink all tickets and delete the panel."
                        onConfirm={() => deletePanelMutation.mutate(panel.id)}
                        okText="Delete"
                        okButtonProps={{ danger: true }}
                      >
                        <Button type="text" danger size="small" icon={<Trash2 size={14} />} />
                      </Popconfirm>
                    </>
                  )}
                  {panel.status === 'INVOICED' && panel.invoiceNumber && (
                    <Tag color="blue">Invoice: {panel.invoiceNumber}</Tag>
                  )}
                </div>
              }
            >
              <div className="flex items-center gap-8 mb-4">
                <Statistic title="Tickets" value={panel.ticketCount} />
                <Statistic title="Total Amount" value={panel.totalAmount || 0} precision={2} prefix="₹" />
              </div>

              {panel.tickets && panel.tickets.length > 0 ? (
                <Table
                  dataSource={panel.tickets}
                  columns={[
                    ...ticketColumns,
                    ...(panel.status === 'OPEN' ? [{
                      title: '',
                      key: 'action',
                      width: 50,
                      render: (record: any) => (
                        <Button
                          type="text"
                          danger
                          size="small"
                          icon={<Trash2 size={12} />}
                          onClick={() => removeTicketMutation.mutate({ panelId: panel.id, ticketId: record.id })}
                        />
                      )
                    }] : [])
                  ]}
                  rowKey="id"
                  pagination={false}
                  size="small"
                />
              ) : (
                <Empty description="No tickets added yet" image={Empty.PRESENTED_IMAGE_SIMPLE} />
              )}
            </Card>
          ))}
        </div>
      )}

      {/* Create Panel Modal */}
      <Modal
        title="Create Billing Panel"
        open={createModalOpen}
        onCancel={() => setCreateModalOpen(false)}
        onOk={() => form.submit()}
        confirmLoading={createMutation.isPending}
        okButtonProps={{ className: 'bg-brand-dark' }}
      >
        <Form form={form} layout="vertical" onFinish={(values) => createMutation.mutate(values)}>
          <Form.Item name="label" label="Panel Label" rules={[{ required: true, message: 'Enter a label (e.g. March 2026)' }]}>
            <Input placeholder="e.g. March 2026 — Company X" />
          </Form.Item>
          <Form.Item name="companyId" label="Company" rules={[{ required: true, message: 'Select a company' }]}>
            <Select
              placeholder="Select company"
              showSearch
              optionFilterProp="label"
              options={companiesData?.content?.map((c: any) => ({ value: c.id, label: c.name })) || []}
            />
          </Form.Item>
        </Form>
      </Modal>

      {/* Add Tickets Modal */}
      <Modal
        title="Add Approved Tickets to Panel"
        open={addTicketsModalOpen}
        onCancel={() => { setAddTicketsModalOpen(false); setSelectedTicketIds([]); }}
        width={800}
        onOk={() => {
          if (selectedPanelId && selectedTicketIds.length > 0) {
            addTicketsMutation.mutate({ panelId: selectedPanelId, ticketIds: selectedTicketIds.map(Number) });
          }
        }}
        okText={`Add ${selectedTicketIds.length} Ticket(s)`}
        okButtonProps={{ className: 'bg-brand-dark', disabled: selectedTicketIds.length === 0 }}
        confirmLoading={addTicketsMutation.isPending}
      >
        <p className="text-gray-500 mb-4">Select approved tickets to add to this panel:</p>
        <Table
          dataSource={availableTickets?.content || []}
          columns={ticketColumns}
          rowKey="id"
          loading={ticketsLoading}
          size="small"
          rowSelection={{
            selectedRowKeys: selectedTicketIds,
            onChange: (keys) => setSelectedTicketIds(keys),
          }}
          pagination={{ pageSize: 10 }}
        />
      </Modal>
    </div>
  );
};

export default BillingPanels;
