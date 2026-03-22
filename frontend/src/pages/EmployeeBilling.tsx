import { useState, useEffect, useCallback, useMemo } from 'react';
import { Card, Select, DatePicker, Table, Button, Input, InputNumber, Popconfirm, message, Tag, Space, Divider, Typography, Empty, Spin } from 'antd';
import { Calendar, Plus, Trash2, Download, FileText, RotateCcw, UserRoundSearch, Building2 } from 'lucide-react';
import api from '../api/axiosInstance';
import dayjs from 'dayjs';

const { RangePicker } = DatePicker;
const { Title, Text } = Typography;

interface Company {
  id: number;
  name: string;
  address?: string;
  gstNumber?: string;
}

interface LineItem {
  key: string;
  ticketId?: number;
  particulars: string;
  sacCode: string;
  taxableValue: number;
  nonTaxableValue: number;
  total: number;
  isManuallyAdded: boolean;
}

const EmployeeBilling = () => {
  // Step 1: Company
  const [companies, setCompanies] = useState<Company[]>([]);
  const [selectedCompanyId, setSelectedCompanyId] = useState<number | null>(null);
  const [selectedCompany, setSelectedCompany] = useState<Company | null>(null);
  const [companiesLoading, setCompaniesLoading] = useState(false);

  // Step 2: Passenger (from approved tickets)
  const [passengers, setPassengers] = useState<string[]>([]);
  const [selectedPassenger, setSelectedPassenger] = useState<string | null>(null);
  const [passengersLoading, setPassengersLoading] = useState(false);

  // Step 3: Date range
  const [dateRange, setDateRange] = useState<[dayjs.Dayjs, dayjs.Dayjs] | null>(null);

  // Step 4: Line items
  const [lineItems, setLineItems] = useState<LineItem[]>([]);
  const [originalLineItems, setOriginalLineItems] = useState<LineItem[]>([]);
  const [ticketsLoading, setTicketsLoading] = useState(false);

  // Invoice details
  const [invoiceNumber, setInvoiceNumber] = useState('');
  const [invoiceDate, setInvoiceDate] = useState(dayjs());
  const [cgstRate, setCgstRate] = useState(9);
  const [sgstRate, setSgstRate] = useState(9);

  // State
  const [loading, setLoading] = useState(false);
  const [createdInvoiceId, setCreatedInvoiceId] = useState<number | null>(null);

  // Load companies on mount
  useEffect(() => {
    setCompaniesLoading(true);
    api.get('/companies')
      .then((res: any) => {
        const data = res.data?.content || res.data || [];
        setCompanies(Array.isArray(data) ? data : []);
      })
      .catch(() => message.error('Failed to load companies'))
      .finally(() => setCompaniesLoading(false));
  }, []);

  // Auto-fetch next invoice number
  useEffect(() => {
    api.get('/employee-billing/invoices/next-number')
      .then((res: any) => setInvoiceNumber(res.data.nextNumber))
      .catch(() => {});
  }, []);

  // When company changes, load passengers from approved tickets
  useEffect(() => {
    if (!selectedCompanyId) {
      setPassengers([]);
      setSelectedPassenger(null);
      return;
    }
    const company = companies.find(c => c.id === selectedCompanyId);
    setSelectedCompany(company || null);
    setSelectedPassenger(null);
    setLineItems([]);
    setDateRange(null);
    setCreatedInvoiceId(null);

    setPassengersLoading(true);
    api.get(`/employee-billing/passengers?companyId=${selectedCompanyId}`)
      .then((res: any) => setPassengers(res.data || []))
      .catch(() => {
        message.error('Failed to load passengers');
        setPassengers([]);
      })
      .finally(() => setPassengersLoading(false));
  }, [selectedCompanyId, companies]);

  // Reset downstream when passenger changes
  useEffect(() => {
    setLineItems([]);
    setDateRange(null);
    setCreatedInvoiceId(null);
  }, [selectedPassenger]);

  // Fetch tickets when date range changes
  const handleDateRangeChange = useCallback(async (dates: any) => {
    if (!dates || !selectedCompanyId || !selectedPassenger) return;
    setDateRange(dates);
    setTicketsLoading(true);
    setCreatedInvoiceId(null);
    try {
      const from = dates[0].format('YYYY-MM-DD');
      const to = dates[1].format('YYYY-MM-DD');
      const { data } = await api.get(
        `/employee-billing/tickets?companyId=${selectedCompanyId}&passengerName=${encodeURIComponent(selectedPassenger)}&from=${from}&to=${to}`
      );
      const items = convertTicketsToLineItems(data);
      setLineItems(items);
      setOriginalLineItems(JSON.parse(JSON.stringify(items)));
      if (items.length === 0) {
        message.info('No tickets found for this employee in the selected period');
      }
    } catch (err) {
      message.error('Failed to fetch tickets');
    }
    setTicketsLoading(false);
  }, [selectedCompanyId, selectedPassenger]);

  // Convert tickets to line items (each ticket becomes one row with base fare)
  const convertTicketsToLineItems = (tickets: any[]): LineItem[] => {
    return tickets.map((t: any, idx: number) => ({
      key: `ticket-${t.id}-${idx}`,
      ticketId: t.id,
      particulars: `${t.origin || ''} → ${t.destination || ''} (PNR: ${t.pnr || ''}) — ${t.dateOfTravel || ''}`,
      sacCode: '996425',
      taxableValue: 0,
      nonTaxableValue: Number(t.baseFare || 0),
      total: Number(t.baseFare || 0),
      isManuallyAdded: false,
    }));
  };

  // Live calculations
  const totals = useMemo(() => {
    const totalTaxable = lineItems.reduce((sum, li) => sum + (li.taxableValue || 0), 0);
    const totalNonTaxable = lineItems.reduce((sum, li) => sum + (li.nonTaxableValue || 0), 0);
    const subTotal = lineItems.reduce((sum, li) => sum + (li.total || 0), 0);
    const cgstAmount = +(totalTaxable * cgstRate / 100).toFixed(2);
    const sgstAmount = +(totalTaxable * sgstRate / 100).toFixed(2);
    const grandTotal = +(subTotal + cgstAmount + sgstAmount).toFixed(2);
    return { totalTaxable, totalNonTaxable, subTotal, cgstAmount, sgstAmount, grandTotal };
  }, [lineItems, cgstRate, sgstRate]);

  // Inline editing
  const handleCellChange = (key: string, field: keyof LineItem, value: any) => {
    setLineItems(prev => prev.map(item => {
      if (item.key !== key) return item;
      const updated = { ...item, [field]: value };
      if (field === 'taxableValue' || field === 'nonTaxableValue') {
        updated.total = (updated.taxableValue || 0) + (updated.nonTaxableValue || 0);
      }
      return updated;
    }));
  };

  const handleAddRow = () => {
    setLineItems(prev => [...prev, {
      key: `manual-${Date.now()}`,
      particulars: '',
      sacCode: '',
      taxableValue: 0,
      nonTaxableValue: 0,
      total: 0,
      isManuallyAdded: true,
    }]);
  };

  const handleDeleteRow = (key: string) => {
    setLineItems(prev => prev.filter(item => item.key !== key));
  };

  const handleReset = () => {
    setLineItems(JSON.parse(JSON.stringify(originalLineItems)));
    message.info('Reset to original data');
  };

  // Generate invoice and export
  const handleGenerate = async (exportType: 'xlsx' | 'pdf') => {
    if (!selectedCompanyId || lineItems.length === 0) {
      message.error('Please complete all steps before generating');
      return;
    }
    setLoading(true);
    try {
      let invoiceId = createdInvoiceId;
      if (!invoiceId) {
        const payload = {
          companyId: selectedCompanyId,
          passengerName: selectedPassenger,
          invoiceNumber,
          invoiceDate: invoiceDate.format('YYYY-MM-DD'),
          billingPeriodFrom: dateRange?.[0]?.format('YYYY-MM-DD'),
          billingPeriodTo: dateRange?.[1]?.format('YYYY-MM-DD'),
          cgstRate,
          sgstRate,
          lineItems: lineItems.map(li => ({
            ticketId: li.ticketId || null,
            particulars: li.particulars,
            sacCode: li.sacCode,
            taxableValue: li.taxableValue,
            nonTaxableValue: li.nonTaxableValue,
            total: li.total,
            isManuallyAdded: li.isManuallyAdded,
          })),
        };
        const { data } = await api.post('/employee-billing/invoices', payload);
        invoiceId = data.id;
        setCreatedInvoiceId(data.id);
        message.success(`Invoice #${data.invoiceNumber} created`);
      }

      const { data: fileData } = await api.get(`/employee-billing/invoices/${invoiceId}/export/${exportType}`, {
        responseType: 'blob',
      });
      const blob = new Blob([fileData], {
        type: exportType === 'xlsx'
          ? 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
          : 'application/pdf',
      });
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `Invoice_${invoiceNumber}_${(selectedPassenger || 'Employee').replace(/\s+/g, '_')}.${exportType}`;
      a.click();
      window.URL.revokeObjectURL(url);
      message.success(`${exportType.toUpperCase()} downloaded!`);
    } catch (err: any) {
      message.error(err?.response?.data?.message || 'Failed to generate invoice');
    }
    setLoading(false);
  };

  // Table columns with inline editing
  const columns = [
    {
      title: '#',
      key: 'index',
      width: 50,
      render: (_: any, __: any, index: number) => index + 1,
    },
    {
      title: 'Particulars',
      dataIndex: 'particulars',
      key: 'particulars',
      width: 280,
      render: (text: string, record: LineItem) => (
        <Input
          value={text}
          onChange={(e: any) => handleCellChange(record.key, 'particulars', e.target.value)}
          variant="borderless"
          className="editable-cell-input"
        />
      ),
    },
    {
      title: 'SAC Code',
      dataIndex: 'sacCode',
      key: 'sacCode',
      width: 100,
      render: (text: string, record: LineItem) => (
        <Input
          value={text}
          onChange={(e: any) => handleCellChange(record.key, 'sacCode', e.target.value)}
          variant="borderless"
          className="editable-cell-input"
        />
      ),
    },
    {
      title: 'Taxable Value',
      dataIndex: 'taxableValue',
      key: 'taxableValue',
      width: 140,
      render: (val: number, record: LineItem) => (
        <InputNumber
          value={val}
          onChange={(v: any) => handleCellChange(record.key, 'taxableValue', v || 0)}
          variant="borderless"
          formatter={(v: any) => `₹ ${v}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')}
          parser={(v: any) => v!.replace(/₹\s?|(,*)/g, '')}
          className="editable-cell-input"
          style={{ width: '100%' }}
        />
      ),
    },
    {
      title: 'Non-Taxable / Exempt',
      dataIndex: 'nonTaxableValue',
      key: 'nonTaxableValue',
      width: 160,
      render: (val: number, record: LineItem) => (
        <InputNumber
          value={val}
          onChange={(v: any) => handleCellChange(record.key, 'nonTaxableValue', v || 0)}
          variant="borderless"
          formatter={(v: any) => `₹ ${v}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')}
          parser={(v: any) => v!.replace(/₹\s?|(,*)/g, '')}
          className="editable-cell-input"
          style={{ width: '100%' }}
        />
      ),
    },
    {
      title: 'Total',
      dataIndex: 'total',
      key: 'total',
      width: 130,
      render: (val: number) => (
        <Text strong>₹ {(val || 0).toLocaleString('en-IN', { minimumFractionDigits: 2 })}</Text>
      ),
    },
    {
      title: '',
      key: 'actions',
      width: 50,
      render: (_: any, record: LineItem) => (
        <Popconfirm title="Delete this row?" onConfirm={() => handleDeleteRow(record.key)}>
          <Button type="text" danger icon={<Trash2 size={14} />} size="small" />
        </Popconfirm>
      ),
    },
  ];

  return (
    <div style={{ padding: '24px', maxWidth: '1200px', margin: '0 auto' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '24px' }}>
        <UserRoundSearch size={28} color="#1677ff" />
        <Title level={3} style={{ margin: 0 }}>Employee Travel Billing</Title>
      </div>

      {/* Step 1: Select Company */}
      <Card title={<><Building2 size={16} style={{ marginRight: 8 }} />Step 1: Select Company</>} size="small" style={{ marginBottom: 16 }}>
        <Select
          showSearch
          placeholder="Search and select a company..."
          style={{ width: '100%', maxWidth: 500 }}
          loading={companiesLoading}
          value={selectedCompanyId}
          onChange={(value: number) => setSelectedCompanyId(value)}
          filterOption={(input: string, option: any) =>
            (option?.label ?? '').toLowerCase().includes(input.toLowerCase())
          }
          options={companies.map((c: Company) => ({ value: c.id, label: c.name }))}
          allowClear
          onClear={() => setSelectedCompanyId(null)}
        />
        {selectedCompany && (
          <div style={{ marginTop: 12, padding: '8px 12px', background: '#f6f8fa', borderRadius: 6 }}>
            <Text type="secondary">Address: </Text><Text>{selectedCompany.address || '—'}</Text>
            <br />
            <Text type="secondary">GSTIN: </Text><Text>{selectedCompany.gstNumber || '—'}</Text>
          </div>
        )}
      </Card>

      {/* Step 2: Select Employee/Passenger */}
      {selectedCompanyId && (
        <Card title="Step 2: Select Employee (from Approved Tickets)" size="small" style={{ marginBottom: 16 }}>
          {passengersLoading ? (
            <Spin />
          ) : passengers.length === 0 ? (
            <Empty description="No approved tickets found for this company" />
          ) : (
            <Select
              showSearch
              placeholder="Select a passenger / employee..."
              style={{ width: '100%', maxWidth: 500 }}
              value={selectedPassenger}
              onChange={(value: string) => setSelectedPassenger(value)}
              filterOption={(input: string, option: any) =>
                (option?.label ?? '').toLowerCase().includes(input.toLowerCase())
              }
              options={passengers.map((p: string) => ({ value: p, label: p }))}
              allowClear
              onClear={() => setSelectedPassenger(null)}
            />
          )}
          {selectedPassenger && (
            <Tag color="blue" style={{ marginTop: 8 }}>
              {passengers.length} employees found | Selected: {selectedPassenger}
            </Tag>
          )}
        </Card>
      )}

      {/* Step 3: Billing Period */}
      {selectedPassenger && (
        <Card title="Step 3: Select Billing Period" size="small" style={{ marginBottom: 16 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <Calendar size={18} color="#8c8c8c" />
            <RangePicker
              onChange={handleDateRangeChange}
              format="DD/MM/YYYY"
              style={{ width: 320 }}
            />
          </div>
        </Card>
      )}

      {/* Step 4: Invoice Details + Editable Table */}
      {dateRange && (lineItems.length > 0 || ticketsLoading) && (
        <>
          <Card title="Step 4: Invoice Details" size="small" style={{ marginBottom: 16 }}>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 16 }}>
              <div>
                <Text type="secondary" style={{ fontSize: 12 }}>Invoice Number</Text>
                <Input value={invoiceNumber} onChange={(e: any) => setInvoiceNumber(e.target.value)} />
              </div>
              <div>
                <Text type="secondary" style={{ fontSize: 12 }}>Invoice Date</Text>
                <DatePicker value={invoiceDate} onChange={(d: any) => d && setInvoiceDate(d)} style={{ width: '100%' }} format="DD/MM/YYYY" />
              </div>
              <div>
                <Text type="secondary" style={{ fontSize: 12 }}>CGST Rate (%)</Text>
                <InputNumber value={cgstRate} onChange={(v: any) => setCgstRate(v || 0)} style={{ width: '100%' }} min={0} max={100} />
              </div>
              <div>
                <Text type="secondary" style={{ fontSize: 12 }}>SGST Rate (%)</Text>
                <InputNumber value={sgstRate} onChange={(v: any) => setSgstRate(v || 0)} style={{ width: '100%' }} min={0} max={100} />
              </div>
            </div>
          </Card>

          <Card
            title="Step 5: Review & Edit Line Items"
            size="small"
            extra={
              <Space>
                <Button icon={<Plus size={14} />} onClick={handleAddRow} type="dashed">Add Row</Button>
                <Button icon={<RotateCcw size={14} />} onClick={handleReset}>Reset</Button>
              </Space>
            }
          >
            <style>{`
              .editable-cell-input { padding: 0 !important; }
              .editable-cell-input input { padding: 4px 8px !important; }
              .ant-table-cell { padding: 4px 8px !important; }
            `}</style>
            <Table
              dataSource={lineItems}
              columns={columns}
              pagination={false}
              bordered
              size="small"
              loading={ticketsLoading}
              rowKey="key"
              summary={() => (
                <>
                  <Table.Summary.Row style={{ background: '#f5f5f5' }}>
                    <Table.Summary.Cell index={0} colSpan={3} align="right"><Text strong>SUB TOTAL</Text></Table.Summary.Cell>
                    <Table.Summary.Cell index={1} align="right">
                      <Text strong>₹ {totals.totalTaxable.toLocaleString('en-IN', { minimumFractionDigits: 2 })}</Text>
                    </Table.Summary.Cell>
                    <Table.Summary.Cell index={2} align="right">
                      <Text strong>₹ {totals.totalNonTaxable.toLocaleString('en-IN', { minimumFractionDigits: 2 })}</Text>
                    </Table.Summary.Cell>
                    <Table.Summary.Cell index={3} align="right">
                      <Text strong>₹ {totals.subTotal.toLocaleString('en-IN', { minimumFractionDigits: 2 })}</Text>
                    </Table.Summary.Cell>
                    <Table.Summary.Cell index={4} />
                  </Table.Summary.Row>
                  <Table.Summary.Row>
                    <Table.Summary.Cell index={0} colSpan={5} align="right">CGST ({cgstRate}%)</Table.Summary.Cell>
                    <Table.Summary.Cell index={1} align="right">
                      ₹ {totals.cgstAmount.toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                    </Table.Summary.Cell>
                    <Table.Summary.Cell index={2} />
                  </Table.Summary.Row>
                  <Table.Summary.Row>
                    <Table.Summary.Cell index={0} colSpan={5} align="right">SGST ({sgstRate}%)</Table.Summary.Cell>
                    <Table.Summary.Cell index={1} align="right">
                      ₹ {totals.sgstAmount.toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                    </Table.Summary.Cell>
                    <Table.Summary.Cell index={2} />
                  </Table.Summary.Row>
                  <Table.Summary.Row style={{ background: '#e6f4ff' }}>
                    <Table.Summary.Cell index={0} colSpan={5} align="right">
                      <Text strong style={{ fontSize: 16 }}>GRAND TOTAL</Text>
                    </Table.Summary.Cell>
                    <Table.Summary.Cell index={1} align="right">
                      <Text strong style={{ fontSize: 16, color: '#1677ff' }}>
                        ₹ {totals.grandTotal.toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                      </Text>
                    </Table.Summary.Cell>
                    <Table.Summary.Cell index={2} />
                  </Table.Summary.Row>
                </>
              )}
            />

            <Divider />

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 12 }}>
              <Button
                type="primary"
                icon={<Download size={16} />}
                onClick={() => handleGenerate('xlsx')}
                loading={loading}
                size="large"
                style={{ background: '#52c41a', borderColor: '#52c41a' }}
              >
                Download Excel (.xlsx)
              </Button>
              <Button
                type="primary"
                icon={<FileText size={16} />}
                onClick={() => handleGenerate('pdf')}
                loading={loading}
                size="large"
              >
                Export PDF Invoice
              </Button>
            </div>
          </Card>
        </>
      )}

      {/* Empty state */}
      {selectedPassenger && dateRange && lineItems.length === 0 && !ticketsLoading && (
        <Card style={{ textAlign: 'center', padding: '32px 0' }}>
          <Empty description={`No tickets found for ${selectedPassenger} in the selected period.`} />
          <Button type="dashed" icon={<Plus size={14} />} onClick={handleAddRow} style={{ marginTop: 16 }}>
            Add Manual Entry
          </Button>
        </Card>
      )}
    </div>
  );
};

export default EmployeeBilling;
