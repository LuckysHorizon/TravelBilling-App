import { useState } from 'react';
import { Card, Table, Button, Input, Tag, Tooltip, Modal, Form, InputNumber, message } from 'antd';
import { Search, Download, Mail, FileSpreadsheet, FileText, Edit3 } from 'lucide-react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import api from '../api/axiosInstance';

const InvoiceList = () => {
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [editModalOpen, setEditModalOpen] = useState(false);
  const [editingInvoice, setEditingInvoice] = useState<any>(null);
  const [editForm] = Form.useForm();
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

  const updateInvoiceMutation = useMutation({
    mutationFn: async ({ id, values }: { id: number; values: any }) => {
      const { data } = await api.put(`/invoices/${id}`, values);
      return data;
    },
    onSuccess: () => {
      message.success('Invoice updated and PDF regenerated');
      setEditModalOpen(false);
      setEditingInvoice(null);
      queryClient.invalidateQueries({ queryKey: ['invoices'] });
    },
    onError: (err: any) => {
      message.error(err.response?.data?.message || 'Failed to update invoice');
    },
  });

  // ─── Authenticated blob download (fixes HTML download issue) ───
  const handleDownloadPdf = async (id: number) => {
    try {
      const response = await api.get(`/invoices/${id}/download-pdf`, { responseType: 'blob' });
      const url = URL.createObjectURL(response.data);
      const a = document.createElement('a');
      a.href = url;
      a.download = `invoice-${id}.pdf`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
    } catch (err: any) {
      message.error('Failed to download PDF');
    }
  };

  const handleDownloadExcel = async (id: number) => {
    try {
      const response = await api.get(`/invoices/${id}/download-excel`, { responseType: 'blob' });
      const url = URL.createObjectURL(response.data);
      const a = document.createElement('a');
      a.href = url;
      a.download = `invoice-${id}.xlsx`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
    } catch (err: any) {
      message.error('Failed to download Excel');
    }
  };

  const handleEditInvoice = (record: any) => {
    setEditingInvoice(record);
    editForm.setFieldsValue({
      serviceCharge: record.serviceCharge,
      cgstTotal: record.cgstTotal,
      sgstTotal: record.sgstTotal,
      grandTotal: record.grandTotal,
    });
    setEditModalOpen(true);
  };

  const handleEditSubmit = () => {
    editForm.validateFields().then((values) => {
      updateInvoiceMutation.mutate({ id: editingInvoice.id, values });
    });
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

  const formatCurrency = (amount: number | null) =>
    amount != null
      ? new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' }).format(amount)
      : '—';

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
      render: (amount: number) => formatCurrency(amount)
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
      width: 240,
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
          {/* Edit button — only for DRAFT/GENERATED */}
          {(record.status === 'DRAFT' || record.status === 'GENERATED') && (
            <Tooltip title="Edit Invoice Totals">
              <Button 
                type="text" 
                icon={<Edit3 size={15} className="text-orange-500 hover:text-orange-700" />} 
                onClick={() => handleEditInvoice(record)}
                size="small"
              />
            </Tooltip>
          )}
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
          <p className="text-gray-500">View, download, edit, and manage billed invoices.</p>
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

      {/* ─── Edit Invoice Totals Modal ─── */}
      <Modal
        title="Edit Invoice Totals"
        open={editModalOpen}
        onCancel={() => { setEditModalOpen(false); setEditingInvoice(null); }}
        onOk={handleEditSubmit}
        confirmLoading={updateInvoiceMutation.isPending}
        okText="Save & Regenerate PDF"
        width={520}
      >
        <p className="text-gray-500 text-sm mb-4">
          Modify the invoice totals below. The PDF will be automatically regenerated.
        </p>
        <Form form={editForm} layout="vertical">
          <div className="grid grid-cols-2 gap-x-4">
            <Form.Item label="Service Charge (₹)" name="serviceCharge" rules={[{ required: true }]}>
              <InputNumber className="w-full" min={0} step={10} prefix="₹" />
            </Form.Item>
            <Form.Item label="CGST Total (₹)" name="cgstTotal" rules={[{ required: true }]}>
              <InputNumber className="w-full" min={0} step={1} prefix="₹" />
            </Form.Item>
            <Form.Item label="SGST Total (₹)" name="sgstTotal" rules={[{ required: true }]}>
              <InputNumber className="w-full" min={0} step={1} prefix="₹" />
            </Form.Item>
            <Form.Item label="Grand Total (₹)" name="grandTotal" rules={[{ required: true }]}>
              <InputNumber className="w-full" min={0} step={1} prefix="₹" />
            </Form.Item>
          </div>
        </Form>
      </Modal>
    </div>
  );
};

export default InvoiceList;
