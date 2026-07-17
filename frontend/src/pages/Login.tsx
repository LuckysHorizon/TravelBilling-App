import { useState, useEffect } from 'react';
import { Form, Input, Button, Alert } from 'antd';
import { User, Lock } from 'lucide-react';
import { useDispatch, useSelector } from 'react-redux';
import { useNavigate, useLocation } from 'react-router-dom';
import { AppDispatch, RootState } from '../store';
import { login, clearError } from '../store/slices/authSlice';

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
    <div className="min-h-screen bg-brand-paper flex flex-col justify-center py-12 sm:px-6 lg:px-8">
      <div className="sm:mx-auto sm:w-full sm:max-w-md animate-fade-in">
        <h2 className="mt-6 text-center text-3xl font-extrabold font-serif text-brand-dark">
          TravelBill Pro
        </h2>
        <p className="mt-2 text-center text-sm text-gray-500">
          Sign in to your agency workspace
        </p>
      </div>

      <div className="mt-8 sm:mx-auto sm:w-full sm:max-w-md animate-slide-up">
        <div className="bg-white py-8 px-4 shadow-elevated rounded-2xl sm:px-10 border border-gray-200/60 backdrop-blur-sm relative overflow-hidden">
          {/* Subtle top accent line */}
          <div className="absolute top-0 left-0 right-0 h-1 bg-gradient-to-r from-brand-gold/50 via-brand-gold to-brand-gold/50" />
          
          {error && (
            <Alert 
              message={error} 
              type="error" 
              showIcon 
              className="mb-6 rounded-lg"
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
                className="rounded-lg hover:border-brand-dark focus:border-brand-dark transition-colors"
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
                className="rounded-lg hover:border-brand-dark focus:border-brand-dark transition-colors"
                autoComplete="current-password"
              />
            </Form.Item>

            <Form.Item className="mt-8 mb-0">
              <Button 
                type="primary" 
                htmlType="submit" 
                className="w-full bg-brand-dark hover:bg-black border-none rounded-lg h-11 text-base font-semibold transition-all shadow-md hover:shadow-lg hover:-translate-y-0.5"
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
