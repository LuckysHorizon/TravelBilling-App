import { useState } from 'react';
import { Card, Upload, message, Button, Alert, Select, Spin } from 'antd';
import { Upload as UploadIcon, PlayCircle, FileText, Zap, CheckCircle } from 'lucide-react';
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
    formData.append('ticketType', 'FLIGHT');
    fileList.forEach((file) => {
      formData.append('files', file as any);
    });

    setUploading(true);
    try {
      const response = await api.post('/tickets/upload', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      });
      message.success('Tickets uploaded successfully. OCR + AI extraction in progress.');
      if (response.data && response.data.length > 0) {
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
      return false;
    },
    fileList,
    multiple: true,
  };

  return (
    <div className="space-y-6 max-w-4xl mx-auto px-2 sm:px-0">
      <div>
        <h1 className="text-2xl sm:text-3xl font-serif text-brand-dark mb-1">Upload Tickets</h1>
        <p className="text-gray-500 text-sm sm:text-base">Drop PDF or Image tickets to begin the OCR and AI extraction pipeline.</p>
      </div>

      <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-4 sm:p-8">
        {/* Company Select */}
        <div className="mb-6">
          <label className="block text-sm font-semibold text-brand-dark mb-2">Select Company</label>
          {companiesLoading ? (
            <Spin size="small" />
          ) : (
            <Select
              placeholder="Choose a client company..."
              className="w-full"
              size="large"
              showSearch
              optionFilterProp="label"
              value={selectedCompanyId}
              onChange={(val) => setSelectedCompanyId(val)}
              options={(companiesData || []).map((c: any) => ({
                value: c.id,
                label: `${c.name} (${c.gstNumber})`,
              }))}
              notFoundContent={
                <div className="text-center py-4 text-gray-400">
                  <p>No companies found.</p>
                  <a onClick={() => navigate('/companies')} className="text-brand-accent">Add a company first →</a>
                </div>
              }
            />
          )}
        </div>

        {/* Upload Area */}
        <Dragger
          {...draggerProps}
          className="!bg-gradient-to-br !from-gray-50 !to-white hover:!from-blue-50 hover:!to-white !border-2 !border-dashed !border-gray-300 hover:!border-brand-accent !rounded-xl transition-all duration-200"
          style={{ padding: '2rem 1rem' }}
        >
          <div className="flex flex-col items-center gap-3 py-4">
            <div className="p-4 bg-white rounded-2xl shadow-sm border border-gray-100">
              <UploadIcon size={32} className="text-brand-dark" />
            </div>
            <p className="text-lg font-semibold text-brand-dark font-serif">Click or drag files here</p>
            <p className="text-sm text-gray-400 max-w-sm text-center">
              Supports PDF, PNG, and JPEG. Upload single tickets or bulk batches.
            </p>
          </div>
        </Dragger>

        {/* Action Buttons */}
        <div className="mt-6 pt-5 border-t border-gray-100 flex flex-col sm:flex-row justify-between items-center gap-3">
          <p className="text-sm text-gray-400">
            {fileList.length > 0 ? `${fileList.length} file(s) ready` : 'No files selected'}
          </p>
          <div className="flex gap-3">
            <Button onClick={() => setFileList([])} disabled={fileList.length === 0 || uploading}>
              Clear
            </Button>
            <Button
              type="primary"
              onClick={handleUpload}
              disabled={fileList.length === 0 || !selectedCompanyId}
              loading={uploading}
              className="bg-brand-dark hover:bg-black border-none"
              icon={<PlayCircle size={16} />}
              size="large"
            >
              {uploading ? 'Processing...' : 'Start Extraction'}
            </Button>
          </div>
        </div>
      </div>

      {/* Pipeline Steps */}
      <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-4 sm:p-6">
        <h3 className="font-serif text-brand-dark text-lg mb-4">How the Pipeline Works</h3>
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          <div className="flex items-start gap-3 p-3 bg-gray-50 rounded-xl">
            <div className="p-2 bg-blue-100 text-blue-600 rounded-lg shrink-0">
              <FileText size={20} />
            </div>
            <div>
              <p className="font-semibold text-sm text-brand-dark">1. Text Extraction</p>
              <p className="text-xs text-gray-500 mt-1">PDFBox for digital PDFs, Tesseract4J OCR for scanned images.</p>
            </div>
          </div>
          <div className="flex items-start gap-3 p-3 bg-gray-50 rounded-xl">
            <div className="p-2 bg-purple-100 text-purple-600 rounded-lg shrink-0">
              <Zap size={20} />
            </div>
            <div>
              <p className="font-semibold text-sm text-brand-dark">2. AI Extraction</p>
              <p className="text-xs text-gray-500 mt-1">Gemini Pro structures PNR, passenger, fare, dates with confidence scores.</p>
            </div>
          </div>
          <div className="flex items-start gap-3 p-3 bg-gray-50 rounded-xl">
            <div className="p-2 bg-green-100 text-green-600 rounded-lg shrink-0">
              <CheckCircle size={20} />
            </div>
            <div>
              <p className="font-semibold text-sm text-brand-dark">3. Human Review</p>
              <p className="text-xs text-gray-500 mt-1">Verify AI results, correct low-confidence fields, approve & auto-calculate GST.</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default TicketUpload;
