import React, { useState } from 'react';
import { Layout, Menu, Avatar, Dropdown, theme } from 'antd';
import { useNavigate, useLocation } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import { AppDispatch, RootState } from '../store';
import { logout } from '../store/slices/authSlice';
import { 
  LayoutDashboard, 
  Building2, 
  Ticket, 
  FileText, 
  BarChart3, 
  Settings, 
  LogOut, 
  User as UserIcon,
  Menu as MenuIcon,
  Upload,
  ClipboardList,
  Users,
  Settings2,
  Activity
} from 'lucide-react';

const { Header, Sider, Content } = Layout;

interface AppLayoutProps {
  children: React.ReactNode;
}

export const AppLayout = ({ children }: AppLayoutProps) => {
  const [collapsed, setCollapsed] = useState(false);
  const { user } = useSelector((state: RootState) => state.auth);
  const navigate = useNavigate();
  const location = useLocation();
  const dispatch = useDispatch<AppDispatch>();

  const handleLogout = async () => {
    await dispatch(logout());
    navigate('/login');
  };

  const getMenuItems = () => {
    const items = [
      {
        key: '/dashboard',
        icon: <LayoutDashboard size={18} />,
        label: 'Dashboard',
      },
      {
        key: '/companies',
        icon: <Building2 size={18} />,
        label: 'Companies',
      },
      {
        key: 'ticketsGroup',
        icon: <Ticket size={18} />,
        label: 'Tickets',
        children: [
          { key: '/tickets', label: 'All Tickets', icon: <ClipboardList size={16} /> },
          ...(user?.role !== 'VIEWER' ? [
            { key: '/tickets/upload', label: 'Upload Tickets', icon: <Upload size={16} /> }
          ] : [])
        ]
      },
      {
        key: '/invoices',
        icon: <FileText size={18} />,
        label: 'Invoices',
      },
      {
        key: '/reports',
        icon: <BarChart3 size={18} />,
        label: 'Reports',
      },
    ];

    if (user?.role === 'ADMIN') {
      items.push({
        key: 'settingsGroup',
        icon: <Settings size={18} />,
        label: 'Settings',
        children: [
          { key: '/settings/users', label: 'User Management', icon: <Users size={16} /> },
          { key: '/settings/system', label: 'System Settings', icon: <Settings2 size={16} /> },
          { key: '/audit-logs', label: 'Audit Logs', icon: <Activity size={16} /> }
        ]
      });
    }

    return items;
  };

  // Find active menu key based on location (handling nested routes)
  const getSelectedKeys = () => {
    const path = location.pathname;
    if (path.startsWith('/tickets/upload')) return ['/tickets/upload'];
    if (path.startsWith('/tickets/review')) return ['/tickets']; // Highlight parent
    if (path.startsWith('/tickets')) return ['/tickets'];
    if (path.startsWith('/companies')) return ['/companies'];
    if (path.startsWith('/invoices')) return ['/invoices'];
    if (path.startsWith('/settings/users')) return ['/settings/users'];
    if (path.startsWith('/settings')) return ['/settings'];
    if (path.startsWith('/reports')) return ['/reports'];
    if (path.startsWith('/audit-logs')) return ['/audit-logs'];
    return ['/dashboard'];
  };

  const userMenu = {
    items: [
      {
        key: 'profile',
        icon: <UserIcon size={16} />,
        label: 'Profile',
      },
      {
        key: 'logout',
        icon: <LogOut size={16} />,
        label: 'Logout',
        onClick: handleLogout,
      },
    ],
  };

  return (
    <Layout className="min-h-screen">
      <Sider 
        trigger={null} 
        collapsible 
        collapsed={collapsed}
        width={240}
        className="shadow-xl z-20"
      >
        <div className="h-16 flex items-center justify-center p-4">
          {!collapsed ? (
            <h1 className="text-white text-xl font-bold font-serif whitespace-nowrap">TravelBill Pro</h1>
          ) : (
            <h1 className="text-white text-xl font-bold font-serif">TB</h1>
          )}
        </div>
        
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={getSelectedKeys()}
          defaultOpenKeys={['ticketsGroup', 'settingsGroup']}
          items={getMenuItems()}
          onClick={({ key }) => navigate(key)}
          className="border-none py-4"
        />
      </Sider>

      <Layout className="bg-brand-paper">
        <Header className="h-16 px-6 bg-white flex items-center justify-between shadow-sm sticky top-0 z-10 border-b border-gray-100">
          <button 
            onClick={() => setCollapsed(!collapsed)}
            className="text-gray-500 hover:text-brand-dark transition-colors"
          >
            <MenuIcon size={20} />
          </button>

          <div className="flex items-center gap-4">
            <span className="text-sm font-medium text-gray-700 hidden sm:block">
              {new Date().toLocaleDateString('en-US', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' })}
            </span>
            <div className="w-px h-6 bg-gray-200 hidden sm:block"></div>
            
            <Dropdown menu={userMenu} placement="bottomRight">
              <div className="flex items-center gap-3 cursor-pointer hover:bg-gray-50 p-1.5 pr-3 rounded-full transition-colors border border-transparent hover:border-gray-200">
                <Avatar className="bg-brand-dark" icon={<UserIcon size={16} />} />
                <div className="flex flex-col">
                  <span className="text-sm font-semibold leading-none">{user?.username}</span>
                  <span className="text-xs text-gray-500 capitalize">{user?.role?.replace('_', ' ').toLowerCase()}</span>
                </div>
              </div>
            </Dropdown>
          </div>
        </Header>

        <Content className="p-6 md:p-8 lg:p-10 max-w-[1600px] w-full mx-auto">
          {children}
        </Content>
      </Layout>
    </Layout>
  );
};
