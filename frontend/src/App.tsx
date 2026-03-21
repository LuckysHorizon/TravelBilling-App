import React, { useEffect } from 'react';
import { BrowserRouter, Routes, Route, Navigate, Outlet } from 'react-router-dom';
import { useDispatch } from 'react-redux';
import { AppDispatch } from './store';
import { checkAuthStatus } from './store/slices/authSlice';
import { ProtectedRoute } from './components/ProtectedRoute';
import { AppLayout } from './layouts/AppLayout';

import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import CompanyList from './pages/CompanyList';
import CompanyDetail from './pages/CompanyDetail';
import TicketList from './pages/TicketList';
import TicketUpload from './pages/TicketUpload';
import TicketReview from './pages/TicketReview';
import InvoiceList from './pages/InvoiceList';
import Reports from './pages/Reports';
import UserManagement from './pages/UserManagement';
import SystemSettings from './pages/SystemSettings';
import AuditLogs from './pages/AuditLogs';
import BillingPanels from './pages/BillingPanels';

function App() {
  const dispatch = useDispatch<AppDispatch>();

  useEffect(() => {
    dispatch(checkAuthStatus());
  }, [dispatch]);

  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<Login />} />
        
        {/* Protected Routes */}
        <Route element={<ProtectedRoute />}>
          <Route element={<AppLayout><Outlet /></AppLayout>}>
            <Route path="/dashboard" element={<Dashboard />} />
            
            {/* Roles: ADMIN, BILLING_STAFF, VIEWER */}
            <Route path="/invoices" element={<InvoiceList />} />
            <Route path="/tickets" element={<TicketList />} />
            <Route path="/companies" element={<CompanyList />} />
            <Route path="/companies/:id" element={<CompanyDetail />} />
            <Route path="/reports" element={<Reports />} />
            <Route path="/billing-panels" element={<BillingPanels />} />

            {/* Roles: ADMIN, BILLING_STAFF */}
            <Route element={<ProtectedRoute allowedRoles={['ADMIN', 'BILLING_STAFF']} />}>
              <Route path="/tickets/upload" element={<TicketUpload />} />
              <Route path="/tickets/review/:batchId" element={<TicketReview />} />
            </Route>

            {/* Roles: ADMIN Only */}
            <Route element={<ProtectedRoute allowedRoles={['ADMIN']} />}>
              <Route path="/settings/users" element={<UserManagement />} />
              <Route path="/settings/system" element={<SystemSettings />} />
              <Route path="/audit-logs" element={<AuditLogs />} />
            </Route>
            
            <Route path="/" element={<Navigate to="/dashboard" replace />} />
          </Route>
        </Route>
        
        <Route path="*" element={<Navigate to="/dashboard" replace />} />
      </Routes>
    </BrowserRouter>
  );
}



export default App;
