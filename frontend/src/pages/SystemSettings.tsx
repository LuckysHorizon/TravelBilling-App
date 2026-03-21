import React, { useEffect } from 'react';
import { Card, Form, Input, Button, InputNumber, Divider, message, Spin } from 'antd';
import { Settings, Percent, Building2, Server } from 'lucide-react';
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

  // Fetch GST config
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
        agencyName: systemConfig?.agencyName || '',
        gstin: systemConfig?.gstin || '',
        address: systemConfig?.address || '',
        cgstRate: gstConfig?.cgstRate || 9,
        sgstRate: gstConfig?.sgstRate || 9,
        igstRate: systemConfig?.igstRate || '',
        smtpHost: systemConfig?.smtpHost || '',
        smtpPort: systemConfig?.smtpPort ? Number(systemConfig.smtpPort) : undefined,
        smtpUsername: systemConfig?.smtpUsername || '',
        smtpPassword: systemConfig?.smtpPassword || '',
      });
    }
  }, [systemConfig, gstConfig, form]);

  const saveConfigMutation = useMutation({
    mutationFn: async (values: any) => {
      // Save system config
      const configPayload: Record<string, string> = {};
      if (values.agencyName) configPayload['agencyName'] = values.agencyName;
      if (values.gstin) configPayload['gstin'] = values.gstin;
      if (values.address) configPayload['address'] = values.address;
      if (values.igstRate !== undefined) configPayload['igstRate'] = String(values.igstRate);
      if (values.smtpHost) configPayload['smtpHost'] = values.smtpHost;
      if (values.smtpPort !== undefined) configPayload['smtpPort'] = String(values.smtpPort);
      if (values.smtpUsername) configPayload['smtpUsername'] = values.smtpUsername;
      if (values.smtpPassword) configPayload['smtpPassword'] = values.smtpPassword;

      await api.put('/admin/system-config', configPayload);

      // Save GST config
      if (values.cgstRate !== undefined && values.sgstRate !== undefined) {
        await api.put('/admin/gst-config', {
          cgstRate: values.cgstRate,
          sgstRate: values.sgstRate,
        });
      }
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
        <p className="text-gray-500">Configure agency defaults, GST rates, and SMTP configurations.</p>
      </div>

      <Form
        form={form}
        layout="vertical"
        onFinish={(values) => saveConfigMutation.mutate(values)}
      >
        <Card title={<span className="flex items-center gap-2 font-serif text-lg"><Building2 size={18}/> Agency Profile</span>} className="mb-6">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-x-6">
            <Form.Item label="Agency Name" name="agencyName" rules={[{ required: true }]}>
              <Input />
            </Form.Item>
            <Form.Item label="Agency GSTIN" name="gstin" rules={[{ required: true }]}>
              <Input className="font-mono" />
            </Form.Item>
            <Form.Item label="Registered Address" name="address" className="md:col-span-2">
              <Input.TextArea rows={3} />
            </Form.Item>
          </div>
        </Card>

        <Card title={<span className="flex items-center gap-2 font-serif text-lg"><Percent size={18}/> Global GST Configuration</span>} className="mb-6">
          <p className="text-gray-500 text-sm mb-4">These rates will apply to all newly generated invoices. Ensure changes comply with current tax laws.</p>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-x-6">
            <Form.Item label="Default CGST (%)" name="cgstRate" rules={[{ required: true }]}>
              <InputNumber className="w-full" min={0} max={100} step={0.1} />
            </Form.Item>
            <Form.Item label="Default SGST (%)" name="sgstRate" rules={[{ required: true }]}>
              <InputNumber className="w-full" min={0} max={100} step={0.1} />
            </Form.Item>
            <Form.Item label="Default IGST (%)" name="igstRate">
              <InputNumber className="w-full" min={0} max={100} step={0.1} />
            </Form.Item>
          </div>
        </Card>

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
