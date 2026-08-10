import { toast } from "sonner";
import { useState } from 'react';
import { Card, Table, Button, Input, Tag, Tooltip, Modal, Form, InputNumber, Empty } from 'antd';
import { Search, Download, Mail, FileSpreadsheet, FileText, Edit3 } from 'lucide-react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import api from '../api/axiosInstance';
import { getStatusTag, formatCurrency } from '../lib/statusUtils';

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
      toast.success(data.message || 'Invoice emailed successfully');
      queryClient.invalidateQueries({ queryKey: ['invoices'] });
    },
    onError: (err: any) => {
      toast.error(err.response?.data?.message || 'Failed to send email');
    },
  });

  const markPaidMutation = useMutation({
    mutationFn: async (id: number) => {
      await api.post(`/invoices/${id}/mark-paid`);
    },
    onSuccess: () => {
      toast.success('Invoice marked as paid');
      queryClient.invalidateQueries({ queryKey: ['invoices'] });
    },
    onError: (err: any) => {
      toast.error(err.response?.data?.message || 'Failed to mark as paid');
    },
  });

  const updateInvoiceMutation = useMutation({
    mutationFn: async ({ id, values }: { id: number; values: any }) => {
      const { data } = await api.put(`/invoices/${id}`, values);
      return data;
    },
    onSuccess: () => {
      toast.success('Invoice updated and PDF regenerated');
      setEditModalOpen(false);
      setEditingInvoice(null);
      queryClient.invalidateQueries({ queryKey: ['invoices'] });
    },
    onError: (err: any) => {
      toast.error(err.response?.data?.message || 'Failed to update invoice');
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
      toast.error('Failed to download PDF');
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
      toast.error('Failed to download Excel');
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
      render: (status: string) => {
        const tag = getStatusTag(status);
        return <Tag color={tag.color}>{tag.label}</Tag>;
      }
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
              icon={<FileText size={16} className="text-gray-400 hover:text-brand-dark transition-colors" />} 
              onClick={() => handleDownloadPdf(record.id)}
              className="w-8 h-8 flex items-center justify-center p-0"
            />
          </Tooltip>
          <Tooltip title="Download Excel">
            <Button 
              type="text" 
              icon={<FileSpreadsheet size={16} className="text-gray-400 hover:text-green-600 transition-colors" />} 
              onClick={() => handleDownloadExcel(record.id)}
              className="w-8 h-8 flex items-center justify-center p-0"
            />
          </Tooltip>
          {/* Edit button — only for DRAFT/GENERATED */}
          {(record.status === 'DRAFT' || record.status === 'GENERATED') && (
            <Tooltip title="Edit Invoice Totals">
              <Button 
                type="text" 
                icon={<Edit3 size={16} className="text-gray-400 hover:text-brand-gold transition-colors" />} 
                onClick={() => handleEditInvoice(record)}
                className="w-8 h-8 flex items-center justify-center p-0"
              />
            </Tooltip>
          )}
          {record.status !== 'PAID' && (
            <>
              <Tooltip title="Send Email">
                <Button 
                  type="text" 
                  icon={<Mail size={16} className="text-gray-400 hover:text-brand-accent transition-colors" />} 
                  onClick={() => sendEmailMutation.mutate(record.id)}
                  loading={sendEmailMutation.isPending}
                  className="w-8 h-8 flex items-center justify-center p-0"
                />
              </Tooltip>
              <Tooltip title="Mark as Paid">
                <Button 
                  type="text"
                  className="text-status-success hover:text-status-success hover:bg-green-50 text-xs px-2 h-8 font-medium ml-1"
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
    <div className="space-y-6 animate-fade-in">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-3xl font-serif text-brand-dark mb-1">Invoices</h1>
          <p className="text-gray-500">View, download, edit, and manage billed invoices.</p>
        </div>
      </div>

      <Card className="min-h-[500px] animate-slide-up" style={{ animationDelay: '100ms', animationFillMode: 'both' }}>
        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-6">
          <Input 
            prefix={<Search className="text-gray-400" size={16} />} 
            placeholder="Search by Invoice Number..." 
            className="max-w-sm rounded-lg hover:border-brand-dark focus:border-brand-dark transition-colors"
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
            emptyText: <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="No invoices found" />
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
