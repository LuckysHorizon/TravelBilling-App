import { useState, useEffect, useCallback, useMemo, useRef } from 'react';
import { Card, Select, DatePicker, Button, Input, InputNumber, message, Tag, Space, Divider, Typography, Empty, Spin } from 'antd';
import { Calendar, Plus, Download, FileText, RotateCcw, UserRoundSearch, Building2 } from 'lucide-react';
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

interface InvoiceData {
  invoiceNumber: string;
  invoiceDate: string;
  passengerName: string;
  mobile: string;
  pnr: string;
  dateOfTravel: string;
  fromCity: string;
  toCity: string;
  companyName: string;
  companyAddress: string;
  companyGstin: string;
  // 8 fixed line items: values only
  airTravel1: number;
  airTravel2: number;
  psf: number;
  udc: number;
  agentCharges: number;
  otherCharges: number;
  discount: number;
  sacAir: string;
  sacAgent: string;
  cgstRate: number;
  sgstRate: number;
}



const numberToWords = (amount: number): string => {
  const ones = ['', 'ONE', 'TWO', 'THREE', 'FOUR', 'FIVE', 'SIX', 'SEVEN', 'EIGHT', 'NINE',
    'TEN', 'ELEVEN', 'TWELVE', 'THIRTEEN', 'FOURTEEN', 'FIFTEEN', 'SIXTEEN', 'SEVENTEEN', 'EIGHTEEN', 'NINETEEN'];
  const tens = ['', '', 'TWENTY', 'THIRTY', 'FORTY', 'FIFTY', 'SIXTY', 'SEVENTY', 'EIGHTY', 'NINETY'];
  const twoDigit = (n: number) => n < 20 ? ones[n] : tens[Math.floor(n / 10)] + (n % 10 ? ' ' + ones[n % 10] : '');

  let rupees = Math.floor(amount);
  if (rupees === 0) return 'ZERO ONLY';
  let words = '';
  if (rupees >= 10000000) { words += twoDigit(Math.floor(rupees / 10000000)) + ' CRORE '; rupees %= 10000000; }
  if (rupees >= 100000) { words += twoDigit(Math.floor(rupees / 100000)) + ' LAKH '; rupees %= 100000; }
  if (rupees >= 1000) { words += twoDigit(Math.floor(rupees / 1000)) + ' THOUSAND '; rupees %= 1000; }
  if (rupees >= 100) { words += ones[Math.floor(rupees / 100)] + ' HUNDRED '; rupees %= 100; }
  if (rupees > 0) words += twoDigit(rupees) + ' ';
  return (words + 'ONLY').replace(/\s+/g, ' ').trim();
};

const fmt = (v: number) => v !== 0 ? v.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) : '';

// Editable cell component
const EditableCell = ({ value, onChange, align, bold, style }: {
  value: string | number; onChange: (v: string) => void; align?: string; bold?: boolean; style?: any;
}) => {
  const [editing, setEditing] = useState(false);
  const [val, setVal] = useState(String(value));
  const inputRef = useRef<HTMLInputElement>(null);
  useEffect(() => setVal(String(value)), [value]);
  useEffect(() => { if (editing && inputRef.current) inputRef.current.focus(); }, [editing]);
  if (editing) {
    return (
      <input ref={inputRef} value={val}
        onChange={e => setVal(e.target.value)}
        onBlur={() => { setEditing(false); onChange(val); }}
        onKeyDown={e => { if (e.key === 'Enter') { setEditing(false); onChange(val); } if (e.key === 'Tab') { setEditing(false); onChange(val); } }}
        style={{ border: '1px solid #1677ff', outline: 'none', width: '100%', padding: '1px 4px', fontSize: 'inherit', fontWeight: bold ? 'bold' : 'normal', textAlign: (align as any) || 'left', background: '#e6f4ff', ...style }}
      />
    );
  }
  return (
    <span onClick={() => setEditing(true)}
      style={{ cursor: 'pointer', display: 'block', minHeight: 16, fontWeight: bold ? 'bold' : 'normal', textAlign: (align as any) || 'left', ...style }}
      title="Click to edit"
    >{typeof value === 'number' ? (value !== 0 ? fmt(value) : '') : value || '\u00A0'}</span>
  );
};

const EmployeeBilling = () => {
  const [companies, setCompanies] = useState<Company[]>([]);
  const [selectedCompanyId, setSelectedCompanyId] = useState<number | null>(null);
  const [selectedCompany, setSelectedCompany] = useState<Company | null>(null);
  const [companiesLoading, setCompaniesLoading] = useState(false);
  const [passengers, setPassengers] = useState<string[]>([]);
  const [selectedPassenger, setSelectedPassenger] = useState<string | null>(null);
  const [passengersLoading, setPassengersLoading] = useState(false);
  const [dateRange, setDateRange] = useState<[dayjs.Dayjs, dayjs.Dayjs] | null>(null);
  const [ticketsLoading, setTicketsLoading] = useState(false);
  const [loading, setLoading] = useState(false);
  const [createdInvoiceId, setCreatedInvoiceId] = useState<number | null>(null);

  // GST config from System Settings
  const [gstConfig, setGstConfig] = useState<{ cgstRate: number; sgstRate: number; serviceChargePerTicket: number }>({ cgstRate: 9, sgstRate: 9, serviceChargePerTicket: 0 });

  // Organization profile from System Settings (for WYSIWYG preview)
  const [orgConfig, setOrgConfig] = useState<Record<string, string>>({
    agencyName: '', orgAddressLine1: '', orgAddressLine2: '',
    gstin: '', panNumber: '',
    bankAccountName: '', bankAccountNumber: '',
    bankName: '', bankBranch: '', bankIfsc: '',
  });

  // The main invoice data state (all editable)
  const [inv, setInv] = useState<InvoiceData | null>(null);
  const [originalInv, setOriginalInv] = useState<InvoiceData | null>(null);

  // Load companies and GST config
  useEffect(() => {
    setCompaniesLoading(true);
    api.get('/companies').then((res: any) => {
      const data = res.data?.content || res.data || [];
      setCompanies(Array.isArray(data) ? data : []);
    }).catch(() => message.error('Failed to load companies')).finally(() => setCompaniesLoading(false));

    // Fetch billing config from System Settings
    api.get('/admin/gst-config').then((res: any) => {
      const cfg = res.data;
      setGstConfig({
        cgstRate: Number(cfg.cgstRate ?? 9),
        sgstRate: Number(cfg.sgstRate ?? 9),
        serviceChargePerTicket: Number(cfg.serviceChargePerTicket ?? 0),
      });
    }).catch(() => { /* use defaults */ });

    // Fetch org profile for WYSIWYG invoice preview
    api.get('/admin/system-config').then((res: any) => {
      setOrgConfig((prev: Record<string, string>) => ({ ...prev, ...res.data }));
    }).catch(() => { /* use defaults */ });
  }, []);

  // Load passengers when company changes
  useEffect(() => {
    if (!selectedCompanyId) { setPassengers([]); setSelectedPassenger(null); return; }
    const company = companies.find((c: Company) => c.id === selectedCompanyId);
    setSelectedCompany(company || null);
    setSelectedPassenger(null); setInv(null); setDateRange(null); setCreatedInvoiceId(null);
    setPassengersLoading(true);
    api.get(`/employee-billing/passengers?companyId=${selectedCompanyId}`).then((res: any) => setPassengers(res.data || []))
      .catch(() => setPassengers([])).finally(() => setPassengersLoading(false));
  }, [selectedCompanyId, companies]);

  useEffect(() => { setInv(null); setDateRange(null); setCreatedInvoiceId(null); }, [selectedPassenger]);

  // Fetch tickets → build invoice data
  const handleDateRangeChange = useCallback(async (dates: any) => {
    if (!dates || !selectedCompanyId || !selectedPassenger || !selectedCompany) return;
    setDateRange(dates);
    setTicketsLoading(true);
    setCreatedInvoiceId(null);
    try {
      const from = dates[0].format('YYYY-MM-DD');
      const to = dates[1].format('YYYY-MM-DD');
      const { data: nextNum } = await api.get('/employee-billing/invoices/next-number');
      const { data: tickets } = await api.get(
        `/employee-billing/tickets?companyId=${selectedCompanyId}&passengerName=${encodeURIComponent(selectedPassenger)}&from=${from}&to=${to}`
      );

      // Aggregate tickets into 8 fixed categories
      let airTravel1 = 0, psf = 0, udc = 0, agentCharges = 0, otherCharges = 0, discount = 0;
      let pnr = '', dateOfTravel = '', fromCity = '', toCity = '';

      for (const t of tickets) {
        airTravel1 += Number(t.baseFare || 0);
        psf += Number(t.passengerServiceFee || 0);
        udc += Number(t.userDevelopmentCharges || 0);
        agentCharges += Number(t.agentServiceCharges || 0);
        otherCharges += Number(t.otherCharges || 0);
        discount += Number(t.discount || 0);
        if (!pnr && t.pnr) pnr = t.pnr;
        if (!dateOfTravel && t.dateOfTravel) dateOfTravel = t.dateOfTravel;
        if (!fromCity && t.origin) fromCity = t.origin;
        if (!toCity && t.destination) toCity = t.destination;
      }

      const invoiceData: InvoiceData = {
        invoiceNumber: nextNum.nextNumber || '',
        invoiceDate: dayjs().format('YYYY-MM-DD'),
        passengerName: selectedPassenger,
        mobile: '',
        pnr, dateOfTravel, fromCity, toCity,
        companyName: selectedCompany.name || '',
        companyAddress: selectedCompany.address || '',
        companyGstin: selectedCompany.gstNumber || '',
        airTravel1, airTravel2: 0, psf, udc,
        agentCharges: agentCharges > 0 ? agentCharges : gstConfig.serviceChargePerTicket * tickets.length,
        otherCharges, discount,
        sacAir: '996425', sacAgent: '998551',
        cgstRate: gstConfig.cgstRate, sgstRate: gstConfig.sgstRate,
      };
      setInv(invoiceData);
      setOriginalInv(JSON.parse(JSON.stringify(invoiceData)));

      if (tickets.length === 0) message.info('No tickets found — you can add values manually');
    } catch { message.error('Failed to fetch tickets'); }
    setTicketsLoading(false);
  }, [selectedCompanyId, selectedPassenger, selectedCompany]);

  // Computed totals
  const totals = useMemo(() => {
    if (!inv) return { nonTaxable: 0, taxable: 0, subTotal: 0, cgst: 0, sgst: 0, grandTotal: 0, words: '' };
    const nonTaxable = inv.airTravel1 + inv.airTravel2 + inv.psf + inv.udc + inv.otherCharges;
    const taxable = inv.agentCharges;
    const subTotal = nonTaxable + taxable - inv.discount;
    const cgst = +(taxable * inv.cgstRate / 100).toFixed(2);
    const sgst = +(taxable * inv.sgstRate / 100).toFixed(2);
    const grandTotal = +(subTotal + cgst + sgst).toFixed(2);
    return { nonTaxable, taxable, subTotal, cgst, sgst, grandTotal, words: numberToWords(grandTotal) };
  }, [inv]);

  const upd = (field: keyof InvoiceData, val: string) => {
    setInv((prev: InvoiceData | null) => {
      if (!prev) return prev;
      const numFields = ['airTravel1', 'airTravel2', 'psf', 'udc', 'agentCharges', 'otherCharges', 'discount', 'cgstRate', 'sgstRate'];
      return { ...prev, [field]: numFields.includes(field) ? parseFloat(val) || 0 : val };
    });
  };

  const handleReset = () => { if (originalInv) { setInv(JSON.parse(JSON.stringify(originalInv))); message.info('Reset to original'); } };

  const handleExport = async (type: 'xlsx' | 'pdf') => {
    if (!inv || !selectedCompanyId) return;
    setLoading(true);
    try {
      let invoiceId = createdInvoiceId;
      if (!invoiceId) {
        const payload = {
          companyId: selectedCompanyId,
          passengerName: inv.passengerName, mobile: inv.mobile, pnr: inv.pnr,
          dateOfTravel: inv.dateOfTravel || null, fromCity: inv.fromCity, toCity: inv.toCity,
          invoiceNumber: inv.invoiceNumber, invoiceDate: inv.invoiceDate,
          billingPeriodFrom: dateRange?.[0]?.format('YYYY-MM-DD'),
          billingPeriodTo: dateRange?.[1]?.format('YYYY-MM-DD'),
          cgstRate: inv.cgstRate, sgstRate: inv.sgstRate,
          lineItems: [
            { particulars: 'Air Travel and related charges', sacCode: inv.sacAir, taxableValue: 0, nonTaxableValue: inv.airTravel1, total: inv.airTravel1, isManuallyAdded: false },
            { particulars: 'Air Travel and related charges', sacCode: '', taxableValue: 0, nonTaxableValue: inv.airTravel2, total: inv.airTravel2, isManuallyAdded: false },
            { particulars: 'Passenger Service Fee', sacCode: '', taxableValue: 0, nonTaxableValue: inv.psf, total: inv.psf, isManuallyAdded: false },
            { particulars: 'User Devolopment Charges', sacCode: '', taxableValue: 0, nonTaxableValue: inv.udc, total: inv.udc, isManuallyAdded: false },
            { particulars: 'Agent service charges', sacCode: inv.sacAgent, taxableValue: inv.agentCharges, nonTaxableValue: 0, total: inv.agentCharges, isManuallyAdded: false },
            { particulars: 'Other charges', sacCode: '', taxableValue: 0, nonTaxableValue: inv.otherCharges, total: inv.otherCharges, isManuallyAdded: false },
            { particulars: 'Discount', sacCode: '', taxableValue: 0, nonTaxableValue: -inv.discount, total: -inv.discount, isManuallyAdded: false },
          ].filter((li: any) => li.total !== 0),
        };
        const { data } = await api.post('/employee-billing/invoices', payload);
        invoiceId = data.id;
        setCreatedInvoiceId(data.id);
        message.success(`Invoice #${data.invoiceNumber} created`);
      }
      const { data: fileData } = await api.get(`/employee-billing/invoices/${invoiceId}/export/${type}`, { responseType: 'blob' });
      const blob = new Blob([fileData], {
        type: type === 'xlsx' ? 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' : 'application/pdf',
      });
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `Invoice_${inv.invoiceNumber}_${inv.passengerName.replace(/\s+/g, '_')}.${type}`;
      a.click();
      window.URL.revokeObjectURL(url);
      message.success(`${type.toUpperCase()} downloaded!`);
    } catch (err: any) {
      message.error(err?.response?.data?.message || 'Export failed');
    }
    setLoading(false);
  };

  // === STYLES ===
  const S: Record<string, React.CSSProperties> = {
    wrap: { padding: 24, maxWidth: 1200, margin: '0 auto' },
    invWrap: { background: '#fff', border: '1px solid #d9d9d9', padding: 24, marginTop: 16, fontFamily: 'Calibri, Arial, sans-serif', fontSize: 12, lineHeight: 1.6, maxWidth: 900, margin: '16px auto' },
    headerRow: { display: 'flex', border: '1px solid #000' },
    headerLeft: { flex: 1, padding: '6px 8px', borderRight: '1px solid #000' },
    headerRight: { flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 20, fontWeight: 'bold', padding: 8 },
    twoCol: { display: 'flex', gap: 0, marginTop: 4 },
    col50: { flex: 1, padding: '0 4px' },
    metaRow: { display: 'flex', gap: 8, marginBottom: 2 },
    metaLabel: { fontWeight: 'bold', minWidth: 120 },
    table: { width: '100%', borderCollapse: 'collapse' as const, marginTop: 10 },
    th: { border: '1px solid #000', padding: '4px 6px', fontWeight: 'bold', textAlign: 'center' as const, background: '#f9f9f9', fontSize: 11 },
    td: { borderLeft: '1px solid #000', borderRight: '1px solid #000', borderBottom: '1px solid #000', padding: '3px 6px', fontSize: 11 },
    tdR: { borderLeft: '1px solid #000', borderRight: '1px solid #000', borderBottom: '1px solid #000', padding: '3px 6px', fontSize: 11, textAlign: 'right' as const },
    totalTd: { border: '1px solid #000', padding: '4px 6px', fontWeight: 'bold', fontSize: 11 },
    totalTdR: { border: '1px solid #000', padding: '4px 6px', fontWeight: 'bold', fontSize: 11, textAlign: 'right' as const },
    bankTax: { display: 'flex', marginTop: 10 },
    wordsRow: { border: '1px solid #000', padding: '4px 6px', marginTop: 4, display: 'flex', gap: 8 },
    footerRow: { display: 'flex', marginTop: 2, fontSize: 10 },
  };

  return (
    <div style={S.wrap}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 20 }}>
        <UserRoundSearch size={28} color="#1677ff" />
        <Title level={3} style={{ margin: 0 }}>Employee Travel Billing</Title>
      </div>

      {/* Step 1: Company */}
      <Card title={<><Building2 size={16} style={{ marginRight: 8 }} />Select Company</>} size="small" style={{ marginBottom: 12 }}>
        <Select showSearch placeholder="Search company..." style={{ width: '100%', maxWidth: 500 }}
          loading={companiesLoading} value={selectedCompanyId}
          onChange={(v: number) => setSelectedCompanyId(v)}
          filterOption={(input: string, option: any) => (option?.label ?? '').toLowerCase().includes(input.toLowerCase())}
          options={companies.map((c: Company) => ({ value: c.id, label: c.name }))} allowClear onClear={() => setSelectedCompanyId(null)}
        />
      </Card>

      {/* Step 2: Passenger */}
      {selectedCompanyId && (
        <Card title="Select Employee (from Approved Tickets)" size="small" style={{ marginBottom: 12 }}>
          {passengersLoading ? <Spin /> : passengers.length === 0 ? <Empty description="No approved tickets" /> : (
            <Select showSearch placeholder="Select passenger..." style={{ width: '100%', maxWidth: 500 }}
              value={selectedPassenger} onChange={(v: string) => setSelectedPassenger(v)}
              filterOption={(input: string, option: any) => (option?.label ?? '').toLowerCase().includes(input.toLowerCase())}
              options={passengers.map((p: string) => ({ value: p, label: p }))} allowClear onClear={() => setSelectedPassenger(null)}
            />
          )}
        </Card>
      )}

      {/* Step 3: Date Range */}
      {selectedPassenger && (
        <Card title="Billing Period" size="small" style={{ marginBottom: 12 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <Calendar size={18} color="#8c8c8c" />
            <RangePicker onChange={handleDateRangeChange} format="DD/MM/YYYY" style={{ width: 320 }} />
          </div>
        </Card>
      )}

      {/* WYSIWYG Invoice Preview */}
      {ticketsLoading && <Card style={{ textAlign: 'center', padding: 40 }}><Spin size="large" /></Card>}
      {inv && !ticketsLoading && (
        <>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: 16, marginBottom: 8 }}>
            <Text strong style={{ fontSize: 16 }}>📄 Invoice Preview (click any field to edit)</Text>
            <Space>
              <Button icon={<RotateCcw size={14} />} onClick={handleReset}>Reset</Button>
              <Button type="primary" icon={<Download size={16} />} onClick={() => handleExport('xlsx')} loading={loading}
                style={{ background: '#52c41a', borderColor: '#52c41a' }}>Download Excel</Button>
              <Button type="primary" icon={<FileText size={16} />} onClick={() => handleExport('pdf')} loading={loading}>Download PDF</Button>
            </Space>
          </div>

          <div style={S.invWrap}>
            {/* Header */}
            <div style={S.headerRow}>
              <div style={S.headerLeft}>
                <div style={{ fontSize: 15, fontWeight: 'bold' }}>{orgConfig.agencyName || 'Your Organization'}</div>
                <div>{orgConfig.orgAddressLine1}</div>
                <div>{orgConfig.orgAddressLine2}</div>
              </div>
              <div style={{ ...S.headerRight, border: '2px solid #000' }}>TAX INVOICE</div>
            </div>

            {/* Two columns: Customer + Invoice meta */}
            <div style={S.twoCol}>
              <div style={S.col50}>
                <div style={{ fontWeight: 'bold', textDecoration: 'underline', marginTop: 4 }}>Customer details:</div>
                <EditableCell value={inv.companyName} onChange={v => upd('companyName', v)} bold />
                <EditableCell value={inv.companyAddress} onChange={v => upd('companyAddress', v)} />
                <div style={{ fontWeight: 'bold', marginTop: 6 }}>
                  CUSTOMER GSTIN: <EditableCell value={inv.companyGstin} onChange={v => upd('companyGstin', v)} style={{ display: 'inline', fontWeight: 'normal' }} />
                </div>
              </div>
              <div style={S.col50}>
                <div style={{ fontSize: 10 }}>GSTIN: {orgConfig.gstin} &nbsp; PAN No.: {orgConfig.panNumber}</div>
                <div style={S.metaRow}><span style={S.metaLabel}>INVOICE NO:</span>
                  <span style={{ border: '1px solid #000', padding: '0 6px' }}><EditableCell value={inv.invoiceNumber} onChange={v => upd('invoiceNumber', v)} /></span></div>
                <div style={S.metaRow}><span style={S.metaLabel}>DATE:</span>
                  <span style={{ border: '1px solid #000', padding: '0 6px' }}><EditableCell value={inv.invoiceDate} onChange={v => upd('invoiceDate', v)} /></span></div>
                <div style={S.metaRow}><span style={S.metaLabel}>Passenger Name:</span><EditableCell value={inv.passengerName} onChange={v => upd('passengerName', v)} /></div>
                <div style={S.metaRow}><span style={S.metaLabel}>Mobile No.</span><EditableCell value={inv.mobile} onChange={v => upd('mobile', v)} /></div>
                <div style={S.metaRow}><span style={S.metaLabel}>PNR :</span><EditableCell value={inv.pnr} onChange={v => upd('pnr', v)} /></div>
                <div style={S.metaRow}><span style={S.metaLabel}>DATE OF TRAVEL:</span><EditableCell value={inv.dateOfTravel} onChange={v => upd('dateOfTravel', v)} /></div>
                <div style={S.metaRow}>
                  <span style={S.metaLabel}>FROM:</span><EditableCell value={inv.fromCity} onChange={v => upd('fromCity', v)} />
                  <span style={{ ...S.metaLabel, marginLeft: 20 }}>TO :</span><EditableCell value={inv.toCity} onChange={v => upd('toCity', v)} />
                </div>
              </div>
            </div>

            {/* Line Items Table — 8 fixed rows */}
            <table style={S.table}>
              <thead>
                <tr>
                  <th style={S.th}>Particulars</th>
                  <th style={S.th}>SAC code</th>
                  <th style={S.th}>Taxable Value</th>
                  <th style={S.th}>Non Taxable/Exemp</th>
                  <th style={S.th} colSpan={2}>TOTAL</th>
                </tr>
              </thead>
              <tbody>
                <tr><td style={S.td}>Air Travel and related charges</td><td style={S.tdR}><EditableCell value={inv.sacAir} onChange={v => upd('sacAir', v)} align="right" /></td>
                  <td style={S.tdR}></td><td style={S.tdR}><EditableCell value={inv.airTravel1} onChange={v => upd('airTravel1', v)} align="right" /></td>
                  <td style={S.tdR} colSpan={2}>{fmt(inv.airTravel1)}</td></tr>
                <tr><td style={S.td}>Air Travel and related charges</td><td style={S.tdR}></td>
                  <td style={S.tdR}></td><td style={S.tdR}><EditableCell value={inv.airTravel2} onChange={v => upd('airTravel2', v)} align="right" /></td>
                  <td style={S.tdR} colSpan={2}>{fmt(inv.airTravel2)}</td></tr>
                <tr><td style={S.td}>Passenger Service Fee</td><td style={S.tdR}></td>
                  <td style={S.tdR}></td><td style={S.tdR}><EditableCell value={inv.psf} onChange={v => upd('psf', v)} align="right" /></td>
                  <td style={S.tdR} colSpan={2}>{fmt(inv.psf)}</td></tr>
                <tr><td style={S.td}>User Devolopment Charges</td><td style={S.tdR}></td>
                  <td style={S.tdR}></td><td style={S.tdR}><EditableCell value={inv.udc} onChange={v => upd('udc', v)} align="right" /></td>
                  <td style={S.tdR} colSpan={2}>{fmt(inv.udc)}</td></tr>
                <tr><td style={S.td}>Agent service charges</td><td style={S.tdR}><EditableCell value={inv.sacAgent} onChange={v => upd('sacAgent', v)} align="right" /></td>
                  <td style={S.tdR}><EditableCell value={inv.agentCharges} onChange={v => upd('agentCharges', v)} align="right" /></td><td style={S.tdR}></td>
                  <td style={S.tdR} colSpan={2}>{fmt(inv.agentCharges)}</td></tr>
                <tr><td style={S.td}>Other charges</td><td style={S.tdR}></td>
                  <td style={S.tdR}></td><td style={S.tdR}><EditableCell value={inv.otherCharges} onChange={v => upd('otherCharges', v)} align="right" /></td>
                  <td style={S.tdR} colSpan={2}>{fmt(inv.otherCharges)}</td></tr>
                <tr><td style={S.td}>Discount</td><td style={S.tdR}></td>
                  <td style={S.tdR}></td><td style={S.tdR}><EditableCell value={inv.discount} onChange={v => upd('discount', v)} align="right" /></td>
                  <td style={S.tdR} colSpan={2}>{inv.discount > 0 ? '-' + fmt(inv.discount) : ''}</td></tr>
                <tr><td style={S.totalTd}>TOTAL</td><td style={S.totalTd}></td>
                  <td style={S.totalTdR}>{fmt(totals.taxable)}</td><td style={S.totalTdR}>{fmt(totals.nonTaxable - inv.discount)}</td>
                  <td style={S.totalTdR} colSpan={2}>{fmt(totals.subTotal)}</td></tr>
              </tbody>
            </table>

            {/* Bank Details + Tax Summary */}
            <div style={S.bankTax}>
              <div style={{ flex: '1.4' }}>
                <div style={{ fontWeight: 'bold', textDecoration: 'underline', marginTop: 6 }}>OUR BANK DETAILS:</div>
                <div>A/C HOLDER NAME: {orgConfig.bankAccountName}</div>
                <div>CURRENT A/C NO.: {orgConfig.bankAccountNumber}</div>
                <div>BANK NAME: {orgConfig.bankName}{orgConfig.bankBranch ? `, BRANCH: ${orgConfig.bankBranch}` : ''}</div>
                <div>IFSC : {orgConfig.bankIfsc}</div>
              </div>
              <div style={{ flex: '0.6' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: 6 }}><span style={{ fontWeight: 'bold' }}>CGST</span><span>{fmt(totals.cgst)}</span></div>
                <div style={{ display: 'flex', justifyContent: 'space-between' }}><span style={{ fontWeight: 'bold' }}>SGST</span><span>{fmt(totals.sgst)}</span></div>
                <div style={{ display: 'flex', justifyContent: 'space-between', borderTop: '1px solid #000', marginTop: 4, paddingTop: 4, fontWeight: 'bold' }}>
                  <span>Grand Total</span><span>{fmt(totals.grandTotal)}</span></div>
              </div>
            </div>

            {/* Total in Words */}
            <div style={S.wordsRow}>
              <span style={{ fontWeight: 'bold', whiteSpace: 'nowrap' }}>TOTAL INVOICE VALUE IN WORDS:</span>
              <span>{totals.words}</span>
            </div>

            {/* Footer */}
            <div style={{ marginTop: 10, fontSize: 10 }}>
              <div style={S.footerRow}>
                <div style={{ flex: 1 }}><b>Air Travel and related Charges :-</b> Includes all Charges related to air transportation of passengers</div>
                <div style={{ textAlign: 'right', fontWeight: 'bold', minWidth: 180 }}>For {orgConfig.agencyName || 'Your Organization'}</div>
              </div>
              <div style={S.footerRow}><div style={{ flex: 1 }}><b>Airport Charges :-</b> Includes ADF, UDF and PSF collected on behalf of Airport Operator, as applicable</div></div>
              <div style={S.footerRow}><div style={{ flex: 1 }}><b>Misc. Services :-</b> Includes Charges of Lounge Assistance and Travel Certificate</div></div>
              <div style={S.footerRow}>
                <div style={{ flex: 1 }}><b>Meal :-</b> Includes all prepaid meals purchased before travel</div>
                <div style={{ textAlign: 'right', fontWeight: 'bold', minWidth: 180 }}>Authorised Signatory</div>
              </div>
            </div>
          </div>

          {/* Bottom buttons */}
          <div style={{ display: 'flex', justifyContent: 'center', gap: 16, marginTop: 16, marginBottom: 24 }}>
            <Button icon={<RotateCcw size={14} />} onClick={handleReset} size="large">Reset to Original</Button>
            <Button type="primary" icon={<Download size={16} />} onClick={() => handleExport('xlsx')} loading={loading} size="large"
              style={{ background: '#52c41a', borderColor: '#52c41a' }}>Download Excel (.xlsx)</Button>
            <Button type="primary" icon={<FileText size={16} />} onClick={() => handleExport('pdf')} loading={loading} size="large">Download PDF</Button>
          </div>
        </>
      )}
    </div>
  );
};

export default EmployeeBilling;
