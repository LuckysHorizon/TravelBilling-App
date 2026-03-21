import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Card, Form, Input, Button, Progress, message, DatePicker, Spin, Alert } from 'antd';
import { CheckCircle, AlertTriangle, FileImage } from 'lucide-react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '../api/axiosInstance';
import dayjs from 'dayjs';

const TicketReview = () => {
  const { batchId } = useParams();
  const navigate = useNavigate();
  const [form] = Form.useForm();
  const queryClient = useQueryClient();

  // Fetch tickets - if batchId is actually a ticket id, fetch single ticket
  // The route param is batchId but we'll treat it as ticket id for now
  const { data: ticket, isLoading, error } = useQuery({
    queryKey: ['ticket', batchId],
    queryFn: async () => {
      const { data } = await api.get(`/tickets/${batchId}`);
      return data;
    },
    enabled: !!batchId,
  });

  const approveMutation = useMutation({
    mutationFn: async (values: any) => {
      const payload = {
        pnrNumber: values.pnrNumber,
        ticketType: ticket?.ticketType || 'FLIGHT',
        passengerName: values.passengerName,
        travelDate: values.travelDate ? dayjs(values.travelDate).format('YYYY-MM-DD') : null,
        origin: values.origin,
        destination: values.destination,
        operatorName: values.operatorName,
        baseFare: values.baseFare,
      };
      const { data } = await api.put(`/tickets/${batchId}/approve`, payload);
      return data;
    },
    onSuccess: () => {
      message.success('Ticket approved and saved.');
      queryClient.invalidateQueries({ queryKey: ['tickets'] });
      navigate('/tickets');
    },
    onError: (err: any) => {
      message.error(err.response?.data?.message || 'Failed to approve ticket');
    },
  });

  useEffect(() => {
    if (ticket) {
      form.setFieldsValue({
        pnrNumber: ticket.pnrNumber,
        passengerName: ticket.passengerName,
        travelDate: ticket.travelDate ? dayjs(ticket.travelDate) : null,
        origin: ticket.origin,
        destination: ticket.destination,
        operatorName: ticket.operatorName,
        baseFare: ticket.baseFare,
      });
    }
  }, [ticket, form]);

  const ConfidenceIndicator = ({ score }: { score: number }) => {
    const percent = Math.round((score || 0) * 100);
    let color = '#10b981';
    if (percent < 60) color = '#ef4444';
    else if (percent < 85) color = '#f59e0b';

    return (
      <div className="flex items-center gap-2 mt-1">
        <Progress percent={percent} size="small" strokeColor={color} showInfo={false} className="flex-1" />
        <span className="text-xs text-gray-500 font-mono">{percent}%</span>
      </div>
    );
  };

  if (isLoading) return <div className="flex justify-center p-20"><Spin size="large" /></div>;
  if (error || !ticket) return <Alert type="error" message="Ticket not found or failed to load" showIcon className="m-6" />;

  const confidence = ticket.aiConfidence ? Number(ticket.aiConfidence) : 0;

  return (
    <div className="space-y-6 max-h-screen flex flex-col">
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-2xl font-serif text-brand-dark mb-1">Review Ticket #{ticket.id}</h1>
          <p className="text-gray-500 text-sm">Please verify the fields extracted by the AI pipeline. Red fields require attention.</p>
        </div>
        <div className="flex items-center gap-3">
          <Button onClick={() => navigate('/tickets')}>Back to List</Button>
          <Button type="primary" className="bg-brand-dark" onClick={() => form.submit()} loading={approveMutation.isPending}>
            Approve & Save
          </Button>
        </div>
      </div>

      <div className="flex gap-6 h-[calc(100vh-180px)]">
        {/* Left pane: Document Viewer */}
        <Card className="flex-1 overflow-hidden flex flex-col" styles={{ body: { height: '100%', padding: 0 } }}>
          {ticket.filePath ? (
            <iframe
              src={`http://localhost:8080/api/files/${ticket.filePath}`}
              className="w-full h-full"
              title="Document Preview"
            />
          ) : (
            <div className="bg-gray-100 h-full flex flex-col items-center justify-center p-8">
              <FileImage size={48} className="text-gray-400 mb-4" />
              <p className="text-gray-500 font-medium">No Document Available</p>
            </div>
          )}
        </Card>

        {/* Right pane: Extracted Fields Form */}
        <Card className="flex-1 overflow-y-auto" title={<span className="font-serif">Extracted Data</span>}>
          <Form
            form={form}
            layout="vertical"
            onFinish={(values) => approveMutation.mutate(values)}
            requiredMark={false}
          >
            <div className="grid grid-cols-2 gap-x-4">
              <Form.Item label="PNR / Booking Ref">
                <Form.Item name="pnrNumber" noStyle rules={[{ required: true }]}>
                  <Input className="font-mono" />
                </Form.Item>
                <ConfidenceIndicator score={confidence} />
              </Form.Item>

              <Form.Item label="Passenger Name">
                <Form.Item name="passengerName" noStyle>
                  <Input />
                </Form.Item>
                <ConfidenceIndicator score={confidence} />
              </Form.Item>

              <Form.Item label="Travel Date">
                <Form.Item name="travelDate" noStyle rules={[{ required: true }]}>
                  <DatePicker className="w-full" />
                </Form.Item>
                <ConfidenceIndicator score={confidence} />
              </Form.Item>
              
              <Form.Item label="Operator">
                <Form.Item name="operatorName" noStyle>
                  <Input />
                </Form.Item>
              </Form.Item>

              <Form.Item label="Origin">
                <Form.Item name="origin" noStyle>
                  <Input />
                </Form.Item>
              </Form.Item>

              <Form.Item label="Destination">
                <Form.Item name="destination" noStyle>
                  <Input />
                </Form.Item>
              </Form.Item>

              <Form.Item label="Base Fare (₹)">
                <Form.Item name="baseFare" noStyle rules={[{ required: true }]}>
                  <Input type="number" step="0.01" className="font-mono" />
                </Form.Item>
                <ConfidenceIndicator score={confidence} />
                {confidence < 0.6 && (
                  <p className="text-xs text-red-500 mt-1 flex items-center gap-1"><AlertTriangle size={12}/> AI Confidence is low. Please verify.</p>
                )}
              </Form.Item>
            </div>
          </Form>
        </Card>
      </div>
    </div>
  );
};

export default TicketReview;
