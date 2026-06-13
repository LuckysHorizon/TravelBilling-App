package com.travelbillpro.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelbillpro.dto.AgentDto;
import com.travelbillpro.entity.User;
import com.travelbillpro.enums.InvoiceStatus;
import com.travelbillpro.enums.TicketStatus;
import com.travelbillpro.repository.CompanyRepository;
import com.travelbillpro.repository.InvoiceRepository;
import com.travelbillpro.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Executes tool calls requested by the LLM agent.
 * Each tool maps to an existing service method so the LLM can fetch real data.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AgentToolExecutor {

    private final TicketService ticketService;
    private final CompanyService companyService;
    private final InvoiceService invoiceService;
    private final TicketRepository ticketRepository;
    private final InvoiceRepository invoiceRepository;
    private final CompanyRepository companyRepository;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // Tools that modify data and require explicit user approval
    private static final Set<String> APPROVAL_REQUIRED_TOOLS = Set.of(
            "create_invoice", "approve_ticket", "delete_tickets"
    );

    /**
     * Check if a tool requires user approval before execution.
     */
    public boolean isApprovalRequired(String toolName) {
        return APPROVAL_REQUIRED_TOOLS.contains(toolName);
    }

    /**
     * Returns tool schemas in OpenAI function-calling format for sending to the LLM.
     */
    public List<Map<String, Object>> getToolSchemas() {
        List<Map<String, Object>> tools = new ArrayList<>();

        // get_tickets
        tools.add(buildToolSchema(
                "get_tickets",
                "Search and filter travel tickets. Returns paginated results.",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "search", Map.of("type", "string", "description", "Search by PNR number or passenger name"),
                                "status", Map.of("type", "string", "description", "Filter by status: PENDING_REVIEW, APPROVED, BILLED, PAID"),
                                "company_id", Map.of("type", "integer", "description", "Filter by company ID"),
                                "page", Map.of("type", "integer", "description", "Page number (0-based, default 0)"),
                                "size", Map.of("type", "integer", "description", "Page size (default 20)")
                        ),
                        "required", List.of()
                )
        ));

        // get_companies
        tools.add(buildToolSchema(
                "get_companies",
                "List all companies. Optionally search by name.",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "search", Map.of("type", "string", "description", "Search by company name"),
                                "page", Map.of("type", "integer", "description", "Page number (0-based, default 0)"),
                                "size", Map.of("type", "integer", "description", "Page size (default 20)")
                        ),
                        "required", List.of()
                )
        ));

        // get_invoices
        tools.add(buildToolSchema(
                "get_invoices",
                "Search and filter invoices. Returns paginated results.",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "company_id", Map.of("type", "integer", "description", "Filter by company ID"),
                                "page", Map.of("type", "integer", "description", "Page number (0-based, default 0)"),
                                "size", Map.of("type", "integer", "description", "Page size (default 20)")
                        ),
                        "required", List.of()
                )
        ));

        // get_dashboard_stats
        tools.add(buildToolSchema(
                "get_dashboard_stats",
                "Get summary dashboard statistics including ticket counts, invoice counts, and revenue.",
                Map.of(
                        "type", "object",
                        "properties", Map.of(),
                        "required", List.of()
                )
        ));

        // navigate_to
        tools.add(buildToolSchema(
                "navigate_to",
                "Navigate the user to a specific page in the application. Use this when the user asks to go to a page.",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "route", Map.of("type", "string",
                                        "description", "The route path to navigate to. Valid routes: /dashboard, /tickets, /invoices, /companies, /billing, /reports, /settings")
                        ),
                        "required", List.of("route")
                )
        ));

        // get_current_context
        tools.add(buildToolSchema(
                "get_current_context",
                "Get information about the current user, their role, and organization.",
                Map.of(
                        "type", "object",
                        "properties", Map.of(),
                        "required", List.of()
                )
        ));

        return tools;
    }

    /**
     * Execute a tool call by dispatching to the appropriate service method.
     *
     * @param toolName the name of the tool to execute
     * @param params   parameters from the LLM tool call
     * @param user     the authenticated user making the request
     * @return result DTO with success/failure and data
     */
    @SuppressWarnings("unchecked")
    public AgentDto.ToolResult executeTool(String toolName, Map<String, Object> params, User user) {
        try {
            log.info("Executing agent tool: {} with params: {}", toolName, params);
            Object result = switch (toolName) {
                case "get_tickets" -> executeGetTickets(params);
                case "get_companies" -> executeGetCompanies(params);
                case "get_invoices" -> executeGetInvoices(params);
                case "get_dashboard_stats" -> executeGetDashboardStats();
                case "navigate_to" -> executeNavigateTo(params);
                case "get_current_context" -> executeGetCurrentContext(user);
                default -> throw new IllegalArgumentException("Unknown tool: " + toolName);
            };

            return AgentDto.ToolResult.builder()
                    .success(true)
                    .data(result)
                    .message("Tool executed successfully")
                    .build();

        } catch (Exception e) {
            log.error("Tool execution failed: {} - {}", toolName, e.getMessage(), e);
            return AgentDto.ToolResult.builder()
                    .success(false)
                    .data(null)
                    .message("Tool execution failed: " + e.getMessage())
                    .build();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Tool Implementations
    // ═══════════════════════════════════════════════════════════════════════

    private Object executeGetTickets(Map<String, Object> params) {
        String search = strParam(params, "search");
        String statusStr = strParam(params, "status");
        Long companyId = longParam(params, "company_id");
        int page = intParam(params, "page", 0);
        int size = intParam(params, "size", 20);

        PageRequest pageable = PageRequest.of(page, Math.min(size, 50));

        if (search != null && !search.isBlank()) {
            return ticketService.searchTickets(search, pageable);
        }
        if (statusStr != null && !statusStr.isBlank()) {
            try {
                TicketStatus status = TicketStatus.valueOf(statusStr.toUpperCase());
                return ticketService.getTicketsByStatus(status, pageable);
            } catch (IllegalArgumentException e) {
                return Map.of("error", "Invalid status: " + statusStr,
                        "valid_statuses", TicketStatus.values());
            }
        }
        if (companyId != null) {
            return ticketService.getTicketsByCompany(companyId, pageable);
        }
        return ticketService.getAllTickets(pageable);
    }

    private Object executeGetCompanies(Map<String, Object> params) {
        String search = strParam(params, "search");
        int page = intParam(params, "page", 0);
        int size = intParam(params, "size", 20);

        PageRequest pageable = PageRequest.of(page, Math.min(size, 50));
        return companyService.getAllCompanies(search, pageable);
    }

    private Object executeGetInvoices(Map<String, Object> params) {
        Long companyId = longParam(params, "company_id");
        int page = intParam(params, "page", 0);
        int size = intParam(params, "size", 20);

        PageRequest pageable = PageRequest.of(page, Math.min(size, 50));

        if (companyId != null) {
            return invoiceService.getInvoicesByCompany(companyId, pageable);
        }
        return invoiceService.getAllInvoices(pageable);
    }

    private Object executeGetDashboardStats() {
        long totalTickets = ticketRepository.count();
        long pendingTickets = ticketRepository.countByStatus(TicketStatus.PENDING_REVIEW);
        long approvedTickets = ticketRepository.countByStatus(TicketStatus.APPROVED);
        long billedTickets = ticketRepository.countByStatus(TicketStatus.BILLED);

        long totalInvoices = invoiceRepository.count();
        long draftInvoices = invoiceRepository.countByStatusIn(List.of(InvoiceStatus.DRAFT));
        long sentInvoices = invoiceRepository.countByStatusIn(List.of(InvoiceStatus.SENT));

        long totalCompanies = companyRepository.count();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total_tickets", totalTickets);
        stats.put("pending_tickets", pendingTickets);
        stats.put("approved_tickets", approvedTickets);
        stats.put("billed_tickets", billedTickets);
        stats.put("total_invoices", totalInvoices);
        stats.put("draft_invoices", draftInvoices);
        stats.put("sent_invoices", sentInvoices);
        stats.put("total_companies", totalCompanies);

        return stats;
    }

    private Object executeNavigateTo(Map<String, Object> params) {
        String route = strParam(params, "route");
        if (route == null || route.isBlank()) {
            return Map.of("error", "Route parameter is required");
        }

        Set<String> validRoutes = Set.of(
                "/dashboard", "/tickets", "/invoices", "/companies",
                "/billing", "/reports", "/settings"
        );

        if (!validRoutes.contains(route)) {
            return Map.of("error", "Invalid route: " + route, "valid_routes", validRoutes);
        }

        return Map.of(
                "action", "navigate",
                "route", route,
                "message", "Navigating to " + route
        );
    }

    private Object executeGetCurrentContext(User user) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("user_id", user.getId());
        context.put("username", user.getUsername());
        context.put("email", user.getEmail());
        context.put("role", user.getRole().name());

        if (user.getOrganization() != null) {
            context.put("organization_id", user.getOrganization().getId());
            context.put("organization_name", user.getOrganization().getName());
        }

        context.put("is_active", user.getIsActive());
        return context;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Helpers
    // ═══════════════════════════════════════════════════════════════════════

    private Map<String, Object> buildToolSchema(String name, String description, Map<String, Object> parameters) {
        Map<String, Object> function = new LinkedHashMap<>();
        function.put("name", name);
        function.put("description", description);
        function.put("parameters", parameters);

        Map<String, Object> tool = new LinkedHashMap<>();
        tool.put("type", "function");
        tool.put("function", function);
        return tool;
    }

    private String strParam(Map<String, Object> params, String key) {
        if (params == null) return null;
        Object v = params.get(key);
        return v != null ? v.toString() : null;
    }

    private int intParam(Map<String, Object> params, String key, int defaultValue) {
        if (params == null) return defaultValue;
        Object v = params.get(key);
        if (v == null) return defaultValue;
        try {
            return Integer.parseInt(v.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private Long longParam(Map<String, Object> params, String key) {
        if (params == null) return null;
        Object v = params.get(key);
        if (v == null) return null;
        try {
            return Long.parseLong(v.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
