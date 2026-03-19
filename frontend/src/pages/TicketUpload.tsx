import { useState } from 'react';
import { Card, Upload, message, Button, Progress, Steps, Alert } from 'antd';
import { Upload as UploadIcon, FileUp, CheckCircle2, AlertCircle, PlayCircle } from 'lucide-react';
import { useQuery } from '@tanstack/react-query';
import api from '../api/axiosInstance';
import { useNavigate } from 'react-router-dom';

const { Dragger } = Upload;

const TicketUpload = () => {
  const [fileList, setFileList] = useState<any[]>([]);
  const [uploading, setUploading] = useState(false);
  const [selectedCompanyId, setSelectedCompanyId] = useState<number | null>(null);
  
  const navigate = useNavigate();

  // Fetch companies for dropdown
  const { data: companiesData, isLoading: companiesLoading } = useQuery({
    queryKey: ['companies', 'all'],
    queryFn: async () => {
      const { data } = await api.get('/companies?page=0&size=100');
      return data.content;
    },
  });

  const handleUpload = async () => {
    if (!selectedCompanyId) {
      message.error("Please select a company first");
      return;
    }

    const formData = new FormData();
    formData.append('companyId', selectedCompanyId.toString());
    formData.append('ticketType', 'FLIGHT'); // default for now
    fileList.forEach((file) => {
      formData.append('files', file as any);
    });

    setUploading(true);
    try {
      // Replace with actual API call
      const response = await api.post('/tickets/upload', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      });
      message.success('Tickets uploaded successfully. Began OCR pipeline.');
      // Assuming response gives a batchId to review
      if (response.data && response.data.length > 0) {
        // Navigate to the list to see them extract, or to the review page if immediate
        navigate('/tickets'); 
      }
    } catch (err: any) {
      message.error(err.response?.data?.message || 'Upload failed');
    } finally {
      setUploading(false);
      setFileList([]);
    }
  };

  const draggerProps = {
    onRemove: (file: any) => {
      const index = fileList.indexOf(file);
      const newFileList = fileList.slice();
      newFileList.splice(index, 1);
      setFileList(newFileList);
    },
    beforeUpload: (file: any) => {
      const isAllowed = file.type === 'application/pdf' || file.type === 'image/jpeg' || file.type === 'image/png';
      if (!isAllowed) {
        message.error(`${file.name} is not a valid format. Only PDF, JPG, PNG allowed.`);
        return Upload.LIST_IGNORE;
      }
      setFileList([...fileList, file]);
      return false; // Prevent auto-upload
    },
    fileList,
    multiple: true,
  };

  return (
    <div className="space-y-6 max-w-4xl mx-auto">
      <div>
        <h1 className="text-3xl font-serif text-brand-dark mb-1">Upload Tickets</h1>
        <p className="text-gray-500">Drop PDF or Image tickets to begin the OCR and AI extraction pipeline.</p>
      </div>

      <Card>
        <div className="mb-6">
          <label className="block text-sm font-medium text-gray-700 mb-2">Select Company</label>
          <select 
            className="w-full border border-gray-300 rounded-lg p-2.5 outline-none focus:border-brand-dark focus:ring-1 focus:ring-brand-dark text-sm bg-white"
            value={selectedCompanyId || ''}
            onChange={(e) => setSelectedCompanyId(Number(e.target.value))}
            disabled={companiesLoading}
          >
            <option value="" disabled>-- Select a Client --</option>
            {companiesData?.map((company: any) => (
              <option key={company.id} value={company.id}>{company.name} ({company.gstNumber})</option>
            ))}
          </select>
        </div>

        <Dragger {...draggerProps} className="bg-brand-paper hover:bg-gray-100 transition-colors border-2 border-dashed border-gray-300 rounded-xl p-10 mt-4">
          <p className="ant-upload-drag-icon flex justify-center mb-4">
            <div className="p-4 bg-white rounded-full shadow-sm">
              <UploadIcon size={32} className="text-brand-dark" />
            </div>
          </p>
          <p className="text-lg font-semibold text-brand-dark font-serif">Click or drag files to this area</p>
          <p className="text-sm text-gray-500 mt-2">
            Support for a single or bulk upload. Only PDF, PNG, and JPEG files are accepted.
          </p>
        </Dragger>

        <div className="mt-8 pt-6 border-t border-gray-100 flex justify-end gap-3">
          <Button onClick={() => setFileList([])} disabled={fileList.length === 0 || uploading}>
            Clear
          </Button>
          <Button
            type="primary"
            onClick={handleUpload}
            disabled={fileList.length === 0}
            loading={uploading}
            className="bg-brand-dark hover:bg-black border-none"
            icon={<PlayCircle size={16} />}
          >
            {uploading ? 'Processing Pipeline' : `Start Extraction (${fileList.length} files)`}
          </Button>
        </div>
      </Card>
      
      {/* Informational graphic */}
      <Alert
        message="About the Extraction Pipeline"
        description="When you start extraction, the system will securely store the files locally, run Apache PDFBox or Tesseract4J for text extraction, and then use Gemini Pro to structure the ticket fields. You will be able to review the confidence scores before creating the final records."
        type="info"
        showIcon
        className="rounded-lg bg-blue-50 border-blue-200"
      />
    </div>
  );
};

export default TicketUpload;
