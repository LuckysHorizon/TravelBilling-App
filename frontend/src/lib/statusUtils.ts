const badgeBaseClass = '!rounded-full !border-0 !px-2 !py-0.5 !font-medium';

const statusBadgeClasses: Record<string, string> = {
  PAID: 'bg-emerald-100 text-emerald-700',
  ACTIVE: 'bg-emerald-100 text-emerald-700',
  GENERATED: 'bg-emerald-100 text-emerald-700',
  CREATED: 'bg-emerald-100 text-emerald-700',
  OPEN: 'bg-emerald-100 text-emerald-700',
  APPROVED: 'bg-sky-100 text-sky-700',
  EXTRACTING: 'bg-sky-100 text-sky-700',
  PROVISIONING: 'bg-sky-100 text-sky-700',
  SENT: 'bg-sky-100 text-sky-700',
  INVOICED: 'bg-slate-100 text-slate-700',
  UPDATED: 'bg-amber-100 text-amber-700',
  PENDING_REVIEW: 'bg-amber-100 text-amber-700',
  SUSPENDED: 'bg-amber-100 text-amber-700',
  CANCELLED: 'bg-red-100 text-red-700',
  EXTRACTION_FAILED: 'bg-red-100 text-red-700',
  FAILED: 'bg-red-100 text-red-700',
  INACTIVE: 'bg-red-100 text-red-700',
  DELETED: 'bg-red-100 text-red-700',
  DRAFT: 'bg-slate-100 text-slate-700',
  QUEUED: 'bg-slate-100 text-slate-700',
  CLOSED: 'bg-slate-100 text-slate-700',
  LOGIN: 'bg-slate-100 text-slate-700',
  VIEWER: 'bg-slate-100 text-slate-700',
  ADMIN: 'bg-red-100 text-red-700',
  BILLING_STAFF: 'bg-sky-100 text-sky-700',
  SUPER_ADMIN: 'bg-violet-100 text-violet-700',
  BILLED: 'bg-violet-100 text-violet-700',
  MONTHLY: 'bg-violet-100 text-violet-700',
  BIWEEKLY: 'bg-sky-100 text-sky-700',
  WEEKLY: 'bg-amber-100 text-amber-700',
  DEFAULT: 'bg-gray-100 text-gray-600',
};

export const getStatusTag = (status: string | null | undefined) => {
  const normalizedStatus = status?.toUpperCase() || 'DEFAULT';

  return {
    className: `${badgeBaseClass} ${statusBadgeClasses[normalizedStatus] ?? statusBadgeClasses.DEFAULT}`,
    label: normalizedStatus.replace(/_/g, ' '),
  };
};

export const formatCurrency = (amount: number | null | undefined): string => {
  if (amount == null) return 'â€”';
  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 0
  }).format(amount);
};
