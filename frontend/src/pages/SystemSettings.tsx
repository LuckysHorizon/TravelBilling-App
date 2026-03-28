import React, { useEffect } from 'react';
import { Card, Form, Input, Button, InputNumber, Divider, message, Spin } from 'antd';
import { Settings, IndianRupee, Building2, Server, Landmark } from 'lucide-react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '../api/axiosInstance';

const SystemSettings = () => {
  const [form] = Form.useForm();
  const queryClient = useQueryClient();

  // Fetch system config
  const { data: systemConfig, isLoading: configLoading } = useQuery({
    queryKey: ['systemConfig'],
    queryFn: async () => {
      const { data } = await api.get('/admin/system-config');
      return data;
    },
  });

  // Fetch GST / Billing config
  const { data: gstConfig, isLoading: gstLoading } = useQuery({
    queryKey: ['gstConfig'],
    queryFn: async () => {
      const { data } = await api.get('/admin/gst-config');
      return data;
    },
  });

  // Set form values when data loads
  useEffect(() => {
    if (systemConfig || gstConfig) {
      form.setFieldsValue({
        // Agency Profile
        agencyName: systemConfig?.agencyName || '',
        orgAddressLine1: systemConfig?.orgAddressLine1 || '',
        orgAddressLine2: systemConfig?.orgAddressLine2 || '',
        gstin: systemConfig?.gstin || '',
        panNumber: systemConfig?.panNumber || '',
        // Bank Details
        bankAccountName: systemConfig?.bankAccountName || '',
        bankAccountNumber: systemConfig?.bankAccountNumber || '',
        bankName: systemConfig?.bankName || '',
        bankBranch: systemConfig?.bankBranch || '',
        bankIfsc: systemConfig?.bankIfsc || '',
        // Billing Config
        serviceChargePerTicket: gstConfig?.serviceChargePerTicket || 0,
        cgstRate: gstConfig?.cgstRate || 0,
        sgstRate: gstConfig?.sgstRate || 0,
        // SMTP
        smtpHost: systemConfig?.smtpHost || '',
        smtpPort: systemConfig?.smtpPort ? Number(systemConfig.smtpPort) : undefined,
        smtpUsername: systemConfig?.smtpUsername || '',
        smtpPassword: systemConfig?.smtpPassword || '',
      });
    }
  }, [systemConfig, gstConfig, form]);

  const saveConfigMutation = useMutation({
    mutationFn: async (values: any) => {
      // Save system config (key-value store)
      const configPayload: Record<string, string> = {};
      // Agency Profile
      if (values.agencyName) configPayload['agencyName'] = values.agencyName;
      if (values.orgAddressLine1) configPayload['orgAddressLine1'] = values.orgAddressLine1;
      if (values.orgAddressLine2) configPayload['orgAddressLine2'] = values.orgAddressLine2;
      if (values.gstin) configPayload['gstin'] = values.gstin;
      if (values.panNumber) configPayload['panNumber'] = values.panNumber;
      // Bank Details
      if (values.bankAccountName) configPayload['bankAccountName'] = values.bankAccountName;
      if (values.bankAccountNumber) configPayload['bankAccountNumber'] = values.bankAccountNumber;
      if (values.bankName) configPayload['bankName'] = values.bankName;
      if (values.bankBranch) configPayload['bankBranch'] = values.bankBranch;
      if (values.bankIfsc) configPayload['bankIfsc'] = values.bankIfsc;
      // SMTP
      if (values.smtpHost) configPayload['smtpHost'] = values.smtpHost;
      if (values.smtpPort !== undefined) configPayload['smtpPort'] = String(values.smtpPort);
      if (values.smtpUsername) configPayload['smtpUsername'] = values.smtpUsername;
      if (values.smtpPassword) configPayload['smtpPassword'] = values.smtpPassword;

      await api.put('/admin/system-config', configPayload);

      // Save GST / Billing config
      await api.put('/admin/gst-config', {
        cgstRate: values.cgstRate,
        sgstRate: values.sgstRate,
        serviceChargePerTicket: values.serviceChargePerTicket,
      });
    },
    onSuccess: () => {
      message.success('System settings updated successfully.');
      queryClient.invalidateQueries({ queryKey: ['systemConfig'] });
      queryClient.invalidateQueries({ queryKey: ['gstConfig'] });
    },
    onError: (err: any) => {
      message.error(err.response?.data?.message || 'Failed to save settings');
    },
  });

  const isLoading = configLoading || gstLoading;

  if (isLoading) {
    return <div className="flex justify-center p-20"><Spin size="large" /></div>;
  }

  return (
    <div className="space-y-6 max-w-4xl mx-auto">
      <div>
        <h1 className="text-3xl font-serif text-brand-dark mb-1">System Settings</h1>
        <p className="text-gray-500">Configure organization profile, billing charges, bank details, and SMTP.</p>
      </div>

      <Form
        form={form}
        layout="vertical"
        onFinish={(values) => saveConfigMutation.mutate(values)}
      >
        {/* ── Agency / Organization Profile ── */}
        <Card title={<span className="flex items-center gap-2 font-serif text-lg"><Building2 size={18}/> Organization Profile</span>} className="mb-6">
          <p className="text-gray-400 text-xs mb-4">These details appear on all generated invoices (header, footer, and signatory line).</p>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-x-6">
            <Form.Item label="Organization Name" name="agencyName" rules={[{ required: true, message: 'Organization name is required' }]}
              tooltip="Displayed in invoice header and footer signatory">
              <Input placeholder="e.g. RAMNET SOLUTIONS" />
            </Form.Item>
            <Form.Item label="GSTIN" name="gstin" rules={[{ required: true, message: 'GSTIN is required' }]}
              tooltip="GSTIN shown in invoice header">
              <Input className="font-mono" placeholder="e.g. 36AMWPB0052D1ZE" />
            </Form.Item>
            <Form.Item label="PAN Number" name="panNumber" rules={[{ required: true, message: 'PAN is required' }]}
              tooltip="PAN number shown alongside GSTIN in invoices">
              <Input className="font-mono" placeholder="e.g. AMWPB0052D" />
            </Form.Item>
            <div /> {/* spacer */}
            <Form.Item label="Address Line 1" name="orgAddressLine1" rules={[{ required: true, message: 'Address line 1 is required' }]}
              tooltip="First line of address on invoices">
              <Input placeholder="e.g. Shop No. 3134, Road No. 2, MIG PHASE II," />
            </Form.Item>
            <Form.Item label="Address Line 2" name="orgAddressLine2"
              tooltip="Second line of address on invoices (city, pin code)">
              <Input placeholder="e.g. BHEL, Hyderabad - 502032." />
            </Form.Item>
          </div>
        </Card>

        {/* ── Bank Details ── */}
        <Card title={<span className="flex items-center gap-2 font-serif text-lg"><Landmark size={18}/> Bank Details</span>} className="mb-6">
          <p className="text-gray-400 text-xs mb-4">Bank details printed in the "OUR BANK DETAILS" section of every invoice.</p>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-x-6">
            <Form.Item label="A/C Holder Name" name="bankAccountName" rules={[{ required: true, message: 'Account holder name is required' }]}>
              <Input placeholder="e.g. RAMNETSOLUTIONS" />
            </Form.Item>
            <Form.Item label="Account Number" name="bankAccountNumber" rules={[{ required: true, message: 'Account number is required' }]}>
              <Input className="font-mono" placeholder="e.g. 32602154473" />
            </Form.Item>
            <Form.Item label="Bank Name" name="bankName" rules={[{ required: true, message: 'Bank name is required' }]}>
              <Input placeholder="e.g. SBI" />
            </Form.Item>
            <Form.Item label="Branch" name="bankBranch">
              <Input placeholder="e.g. TELLAPUR" />
            </Form.Item>
            <Form.Item label="IFSC Code" name="bankIfsc" rules={[{ required: true, message: 'IFSC is required' }]}>
              <Input className="font-mono" placeholder="e.g. SBIN0013071" />
            </Form.Item>
          </div>
        </Card>

        {/* ── Billing Configuration ── */}
        <Card title={<span className="flex items-center gap-2 font-serif text-lg"><IndianRupee size={18}/> Billing Configuration (₹ per Ticket)</span>} className="mb-6">
          <p className="text-gray-500 text-sm mb-4">These flat ₹ amounts will be applied to each ticket during extraction and billing. Update these to change how new tickets are calculated.</p>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-x-6">
            <Form.Item label="Service Charge per Ticket (₹)" name="serviceChargePerTicket" rules={[{ required: true }]}>
              <InputNumber className="w-full" min={0} step={10} prefix="₹" />
            </Form.Item>
            <Form.Item label="CGST per Ticket (₹)" name="cgstRate" rules={[{ required: true }]}>
              <InputNumber className="w-full" min={0} step={1} prefix="₹" />
            </Form.Item>
            <Form.Item label="SGST per Ticket (₹)" name="sgstRate" rules={[{ required: true }]}>
              <InputNumber className="w-full" min={0} step={1} prefix="₹" />
            </Form.Item>
          </div>
        </Card>

        {/* ── SMTP Configuration ── */}
        <Card title={<span className="flex items-center gap-2 font-serif text-lg"><Server size={18}/> SMTP Configuration</span>} className="mb-6">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-x-6">
            <Form.Item label="SMTP Host" name="smtpHost">
              <Input />
            </Form.Item>
            <Form.Item label="SMTP Port" name="smtpPort">
              <InputNumber className="w-full" />
            </Form.Item>
            <Form.Item label="SMTP Username" name="smtpUsername">
              <Input />
            </Form.Item>
            <Form.Item label="SMTP Password" name="smtpPassword">
              <Input.Password />
            </Form.Item>
          </div>
        </Card>

        <div className="flex justify-end gap-3 pb-12">
          <Button onClick={() => form.resetFields()}>Discard Changes</Button>
          <Button type="primary" htmlType="submit" className="bg-brand-dark" loading={saveConfigMutation.isPending}>
            Save Configuration
          </Button>
        </div>
      </Form>
    </div>
  );
};

export default SystemSettings;
