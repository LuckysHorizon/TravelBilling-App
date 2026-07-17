export const getStatusTag = (status: string) => {
  switch (status) {
    // Success states
    case 'PAID':
    case 'ACTIVE':
    case 'GENERATED':
    case 'CREATED':
    case 'OPEN':
      return { color: 'success', label: status.replace('_', ' ') };
      
    // Processing / In-progress states
    case 'EXTRACTING':
    case 'PROVISIONING':
    case 'INVOICED':
    case 'SENT':
    case 'UPDATED':
      return { color: 'processing', label: status.replace('_', ' ') };
      
    // Warning / Attention states
    case 'PENDING_REVIEW':
    case 'SUSPENDED':
      return { color: 'warning', label: status.replace('_', ' ') };
      
    // Error states
    case 'CANCELLED':
    case 'EXTRACTION_FAILED':
    case 'FAILED':
    case 'INACTIVE':
    case 'DELETED':
      return { color: 'error', label: status.replace('_', ' ') };
      
    // Neutral / Other states
    case 'DRAFT':
    case 'QUEUED':
    case 'CLOSED':
    case 'LOGIN':
    case 'VIEWER':
      return { color: 'default', label: status.replace('_', ' ') };
      
    // Roles with specific colors
    case 'ADMIN':
      return { color: 'error', label: status.replace('_', ' ') };
    case 'BILLING_STAFF':
      return { color: 'processing', label: status.replace('_', ' ') };
    case 'SUPER_ADMIN':
      return { color: 'purple', label: status.replace('_', ' ') };
      
    // Special states
    case 'APPROVED':
      return { color: 'blue', label: status.replace('_', ' ') };
    case 'BILLED':
      return { color: 'purple', label: status.replace('_', ' ') };
      
    default:
      return { color: 'default', label: status };
  }
};

export const formatCurrency = (amount: number | null | undefined): string => {
  if (amount == null) return '—';
  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 0
  }).format(amount);
};
