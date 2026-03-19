import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Card, Form, Input, Button, Progress, message, Switch, DatePicker } from 'antd';
import { CheckCircle, AlertTriangle, FileImage } from 'lucide-react';
import dayjs from 'dayjs';

// Dummy data for visual review. Hook this to API later.
const dummyTicketData = {
  id: 101,
  batchId: 'BATCH_1234',
  pnrNumber: 'VIST678',
  pnrConfidence: 0.95,
  passengerName: 'JOHN DOE',
  passengerConfidence: 0.92,
  travelDate: '2023-11-20',
  dateConfidence: 0.88,
  origin: 'DEL',
  originConfidence: 0.99,
  destination: 'BOM',
  destConfidence: 0.99,
  baseFare: 4500.00,
  baseFareConfidence: 0.55,
  filePath: 'dummy.pdf'
};

const TicketReview = () => {
  const { batchId } = useParams();
  const navigate = useNavigate();
  const [form] = Form.useForm();
  
  // Set initial form values based on mock
  const [initialValues] = useState({
    ...dummyTicketData,
    travelDate: dayjs(dummyTicketData.travelDate)
  });

  const ConfidenceIndicator = ({ score }: { score: number }) => {
    const percent = Math.round(score * 100);
    let color = '#10b981'; // green
    if (percent < 60) color = '#ef4444'; // red
    else if (percent < 85) color = '#f59e0b'; // amber

    return (
      <div className="flex items-center gap-2 mt-1">
        <Progress percent={percent} size="small" strokeColor={color} showInfo={false} className="flex-1" />
        <span className="text-xs text-gray-500 font-mono">{percent}%</span>
      </div>
    );
  };

  const onFinish = (values: any) => {
    message.success('Ticket data approved and saved.');
    // Move to next ticket or back to list
    navigate('/tickets');
  };

  return (
    <div className="space-y-6 max-h-screen flex flex-col">
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-2xl font-serif text-brand-dark mb-1">Review Extraction: {batchId}</h1>
          <p className="text-gray-500 text-sm">Please verify the fields extracted by the AI pipeline. Red fields require attention.</p>
        </div>
        <div className="flex items-center gap-3">
          <span className="text-sm font-medium text-gray-600">Ticket 1 of 3</span>
          <Button onClick={() => navigate('/tickets')}>Save for Later</Button>
          <Button type="primary" className="bg-brand-dark" onClick={() => form.submit()}>Confirm & Next</Button>
        </div>
      </div>

      <div className="flex gap-6 h-[calc(100vh-180px)]">
        {/* Left pane: Document Viewer */}
        <Card className="flex-1 overflow-hidden flex flex-col" bodyStyle={{ height: '100%', padding: 0 }}>
          <div className="bg-gray-100 h-full flex flex-col items-center justify-center p-8">
             <FileImage size={48} className="text-gray-400 mb-4" />
             <p className="text-gray-500 font-medium">Document Preview</p>
             <p className="text-xs text-gray-400">PDF Viewer goes here. (Assuming endpoint provides file proxy)</p>
          </div>
        </Card>

        {/* Right pane: Extracted Fields Form */}
        <Card className="flex-1 overflow-y-auto" title={<span className="font-serif">Extracted Data</span>}>
          <Form
            form={form}
            layout="vertical"
            initialValues={initialValues}
            onFinish={onFinish}
            requiredMark={false}
          >
            <div className="grid grid-cols-2 gap-x-4">
              <Form.Item label="PNR / Booking Ref">
                <Form.Item name="pnrNumber" noStyle rules={[{ required: true }]}>
                  <Input className="font-mono" />
                </Form.Item>
                <ConfidenceIndicator score={dummyTicketData.pnrConfidence} />
              </Form.Item>

              <Form.Item label="Passenger Name">
                <Form.Item name="passengerName" noStyle>
                  <Input />
                </Form.Item>
                <ConfidenceIndicator score={dummyTicketData.passengerConfidence} />
              </Form.Item>

              <Form.Item label="Travel Date">
                <Form.Item name="travelDate" noStyle rules={[{ required: true }]}>
                  <DatePicker className="w-full" />
                </Form.Item>
                <ConfidenceIndicator score={dummyTicketData.dateConfidence} />
              </Form.Item>
              
              <Form.Item label="Operator">
                <Form.Item name="operatorName" noStyle>
                  <Input />
                </Form.Item>
                <ConfidenceIndicator score={0.7} />
              </Form.Item>

              <Form.Item label="Origin">
                <Form.Item name="origin" noStyle>
                  <Input />
                </Form.Item>
                <ConfidenceIndicator score={dummyTicketData.originConfidence} />
              </Form.Item>

              <Form.Item label="Destination">
                <Form.Item name="destination" noStyle>
                  <Input />
                </Form.Item>
                <ConfidenceIndicator score={dummyTicketData.destConfidence} />
              </Form.Item>

              <Form.Item label="Base Fare (₹)">
                <Form.Item name="baseFare" noStyle rules={[{ required: true }]}>
                  <Input type="number" step="0.01" className="font-mono" />
                </Form.Item>
                <ConfidenceIndicator score={dummyTicketData.baseFareConfidence} />
                {dummyTicketData.baseFareConfidence < 0.6 && (
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
