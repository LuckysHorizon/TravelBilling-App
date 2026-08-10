import { useState, useEffect } from 'react';
import { Form, Input, Button, Alert } from 'antd';
import { User, Lock, LogIn } from 'lucide-react';
import { useDispatch, useSelector } from 'react-redux';
import { useNavigate, useLocation } from 'react-router-dom';
import { AppDispatch, RootState } from '../store';
import { login, clearError } from '../store/slices/authSlice';
import iconSimple from '../assets/icon-simple.svg';

const Login = () => {
  const [form] = Form.useForm();
  const dispatch = useDispatch<AppDispatch>();
  const navigate = useNavigate();
  const location = useLocation();
  
  const { isLoading, error, isAuthenticated } = useSelector((state: RootState) => state.auth);

  useEffect(() => {
    // Clear any existing errors when mounting
    dispatch(clearError());
  }, [dispatch]);

  useEffect(() => {
    if (isAuthenticated) {
      const from = location.state?.from?.pathname || '/dashboard';
      navigate(from, { replace: true });
    }
  }, [isAuthenticated, navigate, location]);

  const onFinish = async (values: any) => {
    await dispatch(login(values));
  };

  return (
    <div className="login-page min-h-screen flex flex-col justify-center py-12 sm:px-6 lg:px-8 relative overflow-hidden">
      <div aria-hidden className="login-clouds">
        <span className="login-cloud login-cloud--1" />
        <span className="login-cloud login-cloud--2" />
        <span className="login-cloud login-cloud--3" />
        <span className="login-cloud login-cloud--4" />
      </div>
      <div aria-hidden className="login-rings">
        <span className="login-ring login-ring--1" />
        <span className="login-ring login-ring--2" />
        <span className="login-ring login-ring--3" />
      </div>

      <div className="fixed top-6 left-6 sm:left-8 z-20">
        <div className="flex items-center gap-3">
          <div className="w-8 h-8 rounded-lg shrink-0 shadow-sm overflow-hidden">
            <img src={iconSimple} alt="TravelBill icon" className="w-full h-full block" />
          </div>
          <h1 className="text-[#12202D] text-[17px] font-black uppercase tracking-[0.15em] font-sans leading-tight mt-0.5">TRAVELBILL PRO</h1>
        </div>
      </div>

      <div className="mt-8 sm:mx-auto sm:w-full sm:max-w-md animate-slide-up relative z-10">
        <div className="bg-gradient-to-b from-white/90 to-[#DCF0FF]/70 backdrop-blur-[20px] shadow-[0_20px_60px_rgba(100,180,255,0.15)] rounded-[28px] py-10 px-6 sm:px-10 relative overflow-hidden mx-4 sm:mx-0">
          
          <div className="mx-auto mb-5 flex h-12 w-12 items-center justify-center rounded-2xl bg-white/80 backdrop-blur-md shadow-sm border border-white/60">
            <LogIn size={24} className="text-gray-800" strokeWidth={2.5} />
          </div>

          <h2 className="text-center text-[28px] font-extrabold font-serif text-brand-dark leading-none">
            Sign in
          </h2>
          <p className="mt-3 mb-8 text-center text-[15px] text-slate-500 max-w-[320px] mx-auto leading-relaxed">
            Manage travel tickets and billing for your agency
          </p>
          
          {error && (
            <Alert 
              message={error} 
              type="error" 
              showIcon 
              className="mb-6 rounded-xl border-red-200 bg-red-50"
            />
          )}

          <Form
            form={form}
            name="login_form"
            layout="vertical"
            onFinish={onFinish}
            size="large"
            requiredMark={false}
          >
            <Form.Item
              name="username"
              label={<span className="font-medium text-gray-700">Username</span>}
              rules={[{ required: true, message: 'Please input your Username!' }]}
            >
              <Input 
                prefix={<User size={18} className="text-gray-400 mr-1" />} 
                placeholder="admin" 
                className="!rounded-xl !bg-slate-100/90 !border-slate-200 hover:!border-slate-300 focus-within:!border-slate-400 !shadow-none transition-colors"
                autoComplete="username"
              />
            </Form.Item>

            <Form.Item
              name="password"
              label={<span className="font-medium text-gray-700">Password</span>}
              rules={[{ required: true, message: 'Please input your Password!' }]}
            >
              <Input.Password 
                prefix={<Lock size={18} className="text-gray-400 mr-1" />} 
                placeholder="••••••••" 
                className="!rounded-xl !bg-slate-100/90 !border-slate-200 hover:!border-slate-300 focus-within:!border-slate-400 !shadow-none transition-colors"
                autoComplete="current-password"
              />
            </Form.Item>

            <Form.Item className="mt-7 mb-0">
              <Button 
                type="primary" 
                htmlType="submit" 
                className="w-full !h-11 !rounded-xl !border-0 !bg-gradient-to-b !from-[#252733] !to-[#161821] hover:!from-[#1F2230] hover:!to-[#12131B] !text-white text-base font-semibold transition-all shadow-md hover:shadow-lg hover:-translate-y-0.5"
                loading={isLoading}
              >
                Sign in
              </Button>
            </Form.Item>
          </Form>
        </div>
        
        <p className="mt-8 text-center text-xs text-gray-400">
          &copy; {new Date().getFullYear()} TravelBill Pro. All rights reserved.
        </p>
      </div>
    </div>
  );
};

export default Login;
