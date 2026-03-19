import React from 'react';
import { Card, Form, Input, Button, InputNumber, Divider, message } from 'antd';
import { Settings, Percent, Building2, Server } from 'lucide-react';

const SystemSettings = () => {
  const [form] = Form.useForm();

  const handleSave = (values: any) => {
    console.log('Saved settings:', values);
    message.success('System settings updated successfully.');
  };

  return (
    <div className="space-y-6 max-w-4xl mx-auto">
      <div>
        <h1 className="text-3xl font-serif text-brand-dark mb-1">System Settings</h1>
        <p className="text-gray-500">Configure agency defaults, GST rates, and SMTP configurations.</p>
      </div>

      <Form
        form={form}
        layout="vertical"
        onFinish={handleSave}
        initialValues={{
          agencyName: 'TravelBill Pro Agency',
          gstin: '27AADCA2230EA1',
          cgstRate: 9,
          sgstRate: 9,
          smtpHost: 'smtp.gmail.com',
          smtpPort: 587,
        }}
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
          <Button type="primary" htmlType="submit" className="bg-brand-dark">Save Configuration</Button>
        </div>
      </Form>
    </div>
  );
};

export default SystemSettings;
