import { ConfigProvider } from 'antd';
import { ReactNode } from 'react';

interface ThemeProviderProps {
  children: ReactNode;
}

export const ThemeProvider = ({ children }: ThemeProviderProps) => {
  return (
    <ConfigProvider
      theme={{
        token: {
          colorPrimary: '#1a1a1a',       // brand-dark
          colorInfo: '#1a1a1a',
          colorSuccess: '#10b981',
          colorWarning: '#f59e0b',
          colorError: '#ef4444',
          fontFamily: '"Plus Jakarta Sans", sans-serif',
          borderRadius: 8,
          wireframe: false,
          colorBgLayout: '#f9fafb',      // brand-paper
          motionDurationMid: '0.2s',     // Faster animations
          motionEaseInOut: 'cubic-bezier(0.25, 1, 0.5, 1)',
        },
        components: {
          Button: {
            colorPrimary: '#1a1a1a',
            colorPrimaryHover: '#000000',
            colorPrimaryActive: '#000000',
            borderRadius: 6,
            controlHeight: 40,
            paddingInline: 20,
          },
          Card: {
            colorBgContainer: '#ffffff',
            borderRadiusLG: 16,
            boxShadowTertiary: '0 4px 6px -1px rgba(0, 0, 0, 0.05), 0 2px 4px -1px rgba(0, 0, 0, 0.03)',
            paddingLG: 24,
          },
          Input: {
            controlHeight: 40,
            borderRadius: 6,
            colorBorder: '#e5e7eb',
            activeBorderColor: '#1a1a1a',
            hoverBorderColor: '#1a1a1a',
          },
          InputNumber: {
            controlHeight: 40,
            borderRadius: 6,
            colorBorder: '#e5e7eb',
            activeBorderColor: '#1a1a1a',
            hoverBorderColor: '#1a1a1a',
          },
          Select: {
            controlHeight: 40,
            borderRadius: 6,
            colorBorder: '#e5e7eb',
            colorPrimary: '#1a1a1a',
            colorPrimaryHover: '#1a1a1a',
          },
          DatePicker: {
            controlHeight: 40,
            borderRadius: 6,
            colorBorder: '#e5e7eb',
            activeBorderColor: '#1a1a1a',
            hoverBorderColor: '#1a1a1a',
          },
          Menu: {
            darkItemBg: '#1a1a1a',
            darkItemSelectedBg: '#333333',
          },
          Layout: {
            siderBg: '#1a1a1a',
          },
          Table: {
            headerBg: '#f9fafb',
            rowHoverBg: '#f9fafb',
            paddingContentVertical: 12, // Tighter rows
            borderRadiusLG: 12,
          },
          Tag: {
            borderRadiusSM: 4,
            fontWeightStrong: 600,
          },
          Modal: {
            borderRadiusLG: 16,
            paddingContentHorizontal: 24,
            paddingMD: 24,
            boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04)',
          }
        },
      }}
    >
      {children}
    </ConfigProvider>
  );
};
