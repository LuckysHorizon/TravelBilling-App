import React, { useEffect, useRef, useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import { AppDispatch, RootState } from '../store';
import { logout } from '../store/slices/authSlice';
import iconSimple from '../assets/icon-simple.svg';
import { 
  LayoutDashboard, Building2, Ticket, FileText, BarChart3, Settings, 
  LogOut, User as UserIcon, Menu as MenuIcon, Upload, ClipboardList, 
  Users, Settings2, Activity, UserRoundSearch, Globe, ChevronDown, ChevronRight,
  Search, Bell
} from 'lucide-react';
import { cn } from "../lib/utils";

interface AppLayoutProps {
  children: React.ReactNode;
}

export const AppLayout = ({ children }: AppLayoutProps) => {
  const [collapsed, setCollapsed] = useState(false);
  const [openGroups, setOpenGroups] = useState<string[]>(['settingsGroup']);
  const [pendingNavigation, setPendingNavigation] = useState<string | null>(null);
  const [profileMenuOpen, setProfileMenuOpen] = useState(false);
  const profileMenuRef = useRef<HTMLDivElement>(null);
  const { user } = useSelector((state: RootState) => state.auth);
  const navigate = useNavigate();
  const location = useLocation();
  const dispatch = useDispatch<AppDispatch>();

  const handleLogout = async () => {
    setProfileMenuOpen(false);
    await dispatch(logout());
    navigate('/login', { replace: true });
  };

  useEffect(() => {
    if (!profileMenuOpen) return;

    const closeOnOutsideClick = (event: MouseEvent) => {
      if (!profileMenuRef.current?.contains(event.target as Node)) {
        setProfileMenuOpen(false);
      }
    };
    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') setProfileMenuOpen(false);
    };

    document.addEventListener('mousedown', closeOnOutsideClick);
    document.addEventListener('keydown', closeOnEscape);
    return () => {
      document.removeEventListener('mousedown', closeOnOutsideClick);
      document.removeEventListener('keydown', closeOnEscape);
    };
  }, [profileMenuOpen]);

  useEffect(() => {
    setPendingNavigation(null);
  }, [location.pathname]);

  const getMenuGroups = () => {
    const groups: any[] = [
      {
        label: 'Main',
        items: [
          { key: '/dashboard', icon: <LayoutDashboard size={18} />, label: 'Dashboard' },
          { key: '/companies', icon: <Building2 size={18} />, label: 'Companies' },
          { key: '/tickets', icon: <ClipboardList size={18} />, label: 'All tickets' },
          ...(user?.role !== 'VIEWER' ? [
            { key: '/tickets/upload', icon: <Upload size={18} />, label: 'Upload tickets' }
          ] : [])
        ]
      },
      {
        label: 'Billing',
        items: [
          { key: '/invoices', icon: <FileText size={18} />, label: 'Invoices' },
          { key: '/billing-panels', icon: <ClipboardList size={18} />, label: 'Billing panels' },
          { key: '/employee-billing', icon: <UserRoundSearch size={18} />, label: 'Employee billing' },
          { key: '/reports', icon: <BarChart3 size={18} />, label: 'Reports' },
        ]
      }
    ];

    const adminItems = [];
    if (user?.role === 'ADMIN') {
      adminItems.push({
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

    if (user?.role === 'SUPER_ADMIN') {
      adminItems.push({ key: '/super-admin', icon: <Globe size={18} />, label: 'Super Admin' });
    }

    if (adminItems.length > 0) {
      groups.push({
        label: 'Admin',
        items: adminItems
      });
    }

    return groups;
  };

  const toggleGroup = (key: string) => {
    setOpenGroups(prev => prev.includes(key) ? prev.filter(k => k !== key) : [...prev, key]);
  };

  const getBreadcrumbs = () => {
    const paths = location.pathname.split('/').filter(Boolean);
    if (paths.length === 0) return [{ label: 'Dashboard', path: '/dashboard' }];
    
    let currentPath = '';
    return paths.map((path) => {
      currentPath += `/${path}`;
      const label = path.split('-').map(word => word.charAt(0).toUpperCase() + word.slice(1)).join(' ');
      return { label, path: currentPath };
    });
  };

  const isActive = (key: string) => {
    if (key === '/tickets') {
      return location.pathname === '/tickets' || location.pathname.startsWith('/tickets/review');
    }
    if (location.pathname.startsWith(key)) return true;
    return false;
  };

  const navigateTo = (key: string) => {
    setPendingNavigation(key);
    navigate(key);
  };

  const renderMenuItem = (item: any, depth = 0) => {
    if (item.children) {
      const isOpen = openGroups.includes(item.key) && !collapsed;
      return (
        <div key={item.key} className="w-full">
          <button
            onClick={() => !collapsed && toggleGroup(item.key)}
            className={cn(
              "nav-item flex items-center w-full px-4 py-2.5 my-1 rounded-[999px] text-[#5C6B79] hover:bg-white/40",
              collapsed ? "justify-center" : "justify-between"
            )}
          >
            <div className="flex items-center gap-3">
              <span className="text-[#5C6B79]">{item.icon}</span>
              {!collapsed && <span className="font-medium text-[14px]">{item.label}</span>}
            </div>
            {!collapsed && (
              <span className="text-[#5C6B79]">
                {isOpen ? <ChevronDown size={14} /> : <ChevronRight size={14} />}
              </span>
            )}
          </button>
          {isOpen && !collapsed && (
            <div className="pl-4 space-y-1 mt-1 mb-2">
              {item.children.map((child: any) => renderMenuItem(child, depth + 1))}
            </div>
          )}
        </div>
      );
    }

    const active = pendingNavigation === item.key || isActive(item.key);
    return (
      <button
        key={item.key}
        onClick={() => navigateTo(item.key)}
        className={cn(
          "nav-item flex items-center w-full px-4 py-2.5 my-1 rounded-[999px]",
          active 
            ? "nav-item--active text-[#12202D] font-medium" 
            : "text-[#5C6B79] hover:text-[#2B3A48] hover:bg-white/40",
          collapsed ? "justify-center" : "justify-between"
        )}
      >
        <div className="flex items-center gap-3">
          <span className={cn("nav-item-icon", active ? "text-[#0284C7]" : "text-[#5C6B79]")}>{item.icon}</span>
          {!collapsed && <span className={cn("nav-item-label text-[14px]", active ? "font-semibold" : "font-medium")}>{item.label}</span>}
        </div>
        {!collapsed && item.badge && (
          <span className="ml-auto text-[11px] font-bold px-2 py-0.5 rounded-full" style={{ background: 'rgba(217,119,6,0.15)', color: '#92400E' }}>
            {item.badge}
          </span>
        )}
      </button>
    );
  };

  return (
    <div className="flex h-screen w-full bg-brand-paper p-4 overflow-hidden">
      <div 
        className="flex w-full h-full rounded-[18px] shadow-sm overflow-hidden"
        style={{
          background: 'rgba(255, 255, 255, 0.5)',
          backdropFilter: 'blur(18px)',
          WebkitBackdropFilter: 'blur(18px)',
          border: '1px solid rgba(255, 255, 255, 0.85)'
        }}
      >
        {/* Sidebar */}
        <aside 
          className={cn(
            "relative flex flex-col transition-all duration-300 z-20 border-r",
            collapsed ? "w-[80px]" : "w-[260px]"
          )}
          style={{ borderColor: 'rgba(255, 255, 255, 0.85)' }}
        >
          <div className="h-16 flex items-center justify-center p-6 mt-2 relative z-10">
          {!collapsed ? (
            <div className="flex items-center gap-3 w-full">
              <div className="w-10 h-10 rounded-lg shrink-0 shadow-sm overflow-hidden">
                <img src={iconSimple} alt="TravelBill icon" className="w-full h-full block" />
              </div>
              <div className="flex flex-col">
                <h1 className="text-[#12202D] text-[16px] font-bold leading-tight">TravelBill Pro</h1>
                <span className="text-[#5C6B79] text-[12px]">Agency workspace</span>
              </div>
            </div>
          ) : (
            <div className="w-10 h-10 rounded-lg shrink-0 shadow-sm overflow-hidden">
              <img src={iconSimple} alt="TravelBill icon" className="w-full h-full block" />
            </div>
          )}
        </div>
        <div className="flex-1 overflow-y-auto overflow-x-hidden px-4 pb-4 [&::-webkit-scrollbar]:hidden [-ms-overflow-style:none] [scrollbar-width:none]">
          <div className="space-y-6">
            {getMenuGroups().map((group, idx) => (
              <div key={idx} className="space-y-1">
                {!collapsed && (
                  <h3 className="px-4 text-[11px] font-semibold text-[#5C6B79]/80 mb-2">{group.label}</h3>
                )}
                {group.items.map((item: any) => renderMenuItem(item))}
              </div>
            ))}
          </div>
        </div>
        {/* Sidebar Footer - Profile */}
        <div className="relative p-4 mt-auto" ref={profileMenuRef}>
          {profileMenuOpen && (
            <div
              role="menu"
              aria-label="Account options"
              className={cn(
                "absolute bottom-[calc(100%+0.5rem)] z-30 w-52 overflow-hidden rounded-xl bg-white p-1 shadow-lg ring-1 ring-black/5",
                collapsed ? "left-4" : "inset-x-4"
              )}
            >
              <p className="px-3 py-2 text-xs font-medium text-[#5C6B79]">My Account</p>
              <div className="my-1 h-px bg-[#5C6B79]/15" />
              <button
                type="button"
                role="menuitem"
                onClick={() => {
                  setProfileMenuOpen(false);
                  navigate('/settings/users');
                }}
                className="flex w-full items-center gap-2 rounded-lg px-3 py-2 text-left text-sm text-[#2B3A48] transition-colors hover:bg-[#F1F5F9] focus-visible:bg-[#F1F5F9] focus-visible:outline-none"
              >
                <UserIcon size={16} />
                User Management
              </button>
              <button
                type="button"
                role="menuitem"
                onClick={handleLogout}
                className="flex w-full items-center gap-2 rounded-lg px-3 py-2 text-left text-sm font-medium text-red-700 transition-colors hover:bg-red-50 focus-visible:bg-red-50 focus-visible:outline-none"
              >
                <LogOut size={16} />
                Logout
              </button>
            </div>
          )}
          <button
            type="button"
            aria-expanded={profileMenuOpen}
            aria-haspopup="menu"
            onClick={() => setProfileMenuOpen((open) => !open)}
            className={cn(
              "flex w-full items-center transition-all duration-300 shadow-sm hover:shadow-md focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#0284C7] focus-visible:ring-offset-2",
              collapsed ? "p-1.5 justify-center rounded-[999px]" : "gap-3 p-1.5 pr-4 rounded-[999px]"
            )}
            style={{
              background: 'rgba(255, 255, 255, 0.7)',
              backdropFilter: 'blur(12px)',
              WebkitBackdropFilter: 'blur(12px)',
              border: '1px solid rgba(255, 255, 255, 0.9)'
            }}
          >
            <div className="h-9 w-9 shrink-0 rounded-full bg-black flex items-center justify-center text-white font-bold text-sm">
              {user?.username?.substring(0, 2).toUpperCase() || <UserIcon size={16} />}
            </div>
            {!collapsed && (
              <>
                <div className="flex flex-col text-left overflow-hidden flex-1">
                  <span className="text-[13px] font-semibold leading-none text-[#12202D] truncate">{user?.username}</span>
                  <span className="text-[11px] text-[#5C6B79] capitalize mt-1 truncate">{user?.role?.replace('_', ' ').toLowerCase()}</span>
                </div>
                <ChevronRight size={14} className={cn("text-[#5C6B79] shrink-0 transition-transform", profileMenuOpen && "rotate-[-90deg]")} />
              </>
            )}
          </button>
        </div>
      </aside>

      {/* Main Content Area */}
      <div className="flex-1 flex flex-col min-w-0 overflow-hidden">
        {/* Header */}
        <header 
          className="h-16 px-6 flex items-center justify-between border-b z-10"
          style={{ borderColor: 'rgba(255, 255, 255, 0.85)' }}
        >
          <div className="flex items-center gap-3">
            <button 
              onClick={() => setCollapsed(!collapsed)}
              className="text-[#5C6B79] hover:text-[#12202D] transition-colors p-2 -ml-2 rounded-md hover:bg-white/40"
            >
              <MenuIcon size={20} />
            </button>
            
            <div className="flex items-center text-[13px] font-medium hidden sm:flex">
              {getBreadcrumbs().map((crumb, idx, arr) => (
                <React.Fragment key={crumb.path}>
                  <span className={idx === arr.length - 1 ? "text-[#12202D]" : "text-[#5C6B79]"}>
                    {crumb.label}
                  </span>
                  {idx < arr.length - 1 && (
                    <ChevronRight size={14} className="mx-1 text-[#5C6B79]/50" />
                  )}
                </React.Fragment>
              ))}
            </div>
          </div>

          <div className="flex items-center gap-4">
            <button className="text-[#5C6B79] hover:text-[#12202D] transition-colors">
              <Search size={18} />
            </button>
            <button className="text-[#5C6B79] hover:text-[#12202D] transition-colors relative">
              <Bell size={18} />
              <span className="absolute top-0 right-0.5 w-1.5 h-1.5 bg-red-500 rounded-full"></span>
            </button>
            <div className="w-px h-5 bg-[#5C6B79]/20 hidden sm:block mx-1"></div>
            <span className="text-[12px] font-medium text-[#5C6B79] hidden sm:block">
              {new Date().toLocaleDateString('en-US', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' })}
            </span>
          </div>
        </header>

        {/* Page Content */}
        <main className="flex-1 overflow-y-auto p-6 md:p-8 lg:p-10">
          <div className="max-w-[1600px] w-full mx-auto animate-fade-in">
            {children}
          </div>
        </main>
      </div>
    </div>
  </div>
  );
};
