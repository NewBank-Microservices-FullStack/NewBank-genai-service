package com.newbank.microservices.genai;

import java.util.List;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import tools.jackson.core.JacksonException;

import com.newbank.microservices.genai.dto.AccountDetails;
import com.newbank.microservices.genai.dto.AccountRequest;
import com.newbank.microservices.genai.dto.Advisor;
import com.newbank.microservices.genai.dto.CustomerDetails;
import com.newbank.microservices.genai.dto.CustomerRequest;


/**
 * #3: writes are role-restricted, validated with {@code @Valid} (the constraints on
 * {@link CustomerRequest}/{@link AccountRequest} were previously never enforced) and
 * confirmation-gated — the model may only PROPOSE a mutation; it executes after the
 * user re-posts with {@code confirmWrites=true}. #15: logs carry ids only, never
 * whole records (no customer PII in logs).
 */
@Component
@Validated
public class BankTools {

    private static final Logger LOG = LoggerFactory.getLogger(BankTools.class);

    private final AIDataProvider bankAiProvider;
    private final ToolConfirmationService confirmations;

    BankTools(AIDataProvider bankAiProvider, ToolConfirmationService confirmations) {
        this.bankAiProvider = bankAiProvider;
        this.confirmations = confirmations;
    }

    private static String conversationOf(ToolContext ctx) {
        if (ctx == null || ctx.getContext() == null) {
            return "unknown";
        }
        Object conv = ctx.getContext().get("conversationId");
        return conv == null ? "unknown" : String.valueOf(conv);
    }

    private static boolean confirmedOf(ToolContext ctx) {
        if (ctx == null || ctx.getContext() == null) {
            return false;
        }
        return Boolean.TRUE.equals(ctx.getContext().get("confirmed"));
    }

    @Tool(description = "List the customers in the bank system")
    @PreAuthorize("hasAnyRole('USER','ADMIN','ADVISOR')")
    public List<CustomerDetails> listCustomers() {
        LOG.info("listCustomers()");
        return bankAiProvider.getAllCustomers();
    }

    @Tool(description = "Propose adding a new customer to the bank. Does NOT create anything; ask the user to confirm first.")
    @PreAuthorize("hasAnyRole('USER','ADMIN','ADVISOR')")
    public String proposeCustomerToBank(@Valid CustomerRequest customerRequest, ToolContext ctx) {
        String conv = conversationOf(ctx);
        LOG.info("proposeCustomerToBank() conversation={}", conv);
        var pending = confirmations.propose(conv, "Create customer");
        return "Proposed customer creation. Ask the user to confirm before proceeding. token=" + pending.token();
    }

    @Tool(description = "Add a new customer to the bank. Only runs after proposeCustomerToBank plus explicit user confirmation.")
    @PreAuthorize("hasAnyRole('ADMIN','ADVISOR')")
    public CustomerDetails addCustomerToBank(@Valid CustomerRequest customerRequest, ToolContext ctx) {
        String conv = conversationOf(ctx);
        if (!confirmedOf(ctx) || !confirmations.hasPending(conv)) {
            LOG.warn("addCustomerToBank() refused without confirmation conversation={}", conv);
            throw new ConfirmationRequiredException(
                "Customer creation requires explicit confirmation. Call proposeCustomerToBank first.");
        }
        confirmations.confirm(conv);
        LOG.info("addCustomerToBank() conversation={}", conv);
        return bankAiProvider.addCustomerToBank(customerRequest);
    }

    @Tool(description = "List the advisors in the bank")
    @PreAuthorize("hasAnyRole('USER','ADMIN','ADVISOR')")
    public List<String> listAdvisors(@ToolParam(required = false) Advisor advisorRequest) {
        // #10: null is valid — provider returns a paginated list. Log a flag, not content.
        LOG.info("listAdvisors() hasCriteria={}", advisorRequest != null);
        try {
            return bankAiProvider.getAdvisors(advisorRequest);
        }
        catch (JacksonException e) {
            LOG.error("Error processing JSON in the listAdvisors function", e);
            return List.of();
        }
    }

    @Tool(description = "Add an account to a customer. Allowed account type IDs are 1=checking, 2=savings, 3=credit, 4=investment, 5=loan. Requires explicit user confirmation.")
    @PreAuthorize("hasAnyRole('ADMIN','ADVISOR')")
    public AccountDetails addAccountToCustomer(@ToolParam(description = "Customer identifier") int customerId,
            @Valid AccountRequest accountRequest, ToolContext ctx) {
        String conv = conversationOf(ctx);
        if (!confirmedOf(ctx) || !confirmations.hasPending(conv)) {
            LOG.warn("addAccountToCustomer() refused without confirmation conversation={}", conv);
            throw new ConfirmationRequiredException(
                "Account creation requires explicit confirmation from the user.");
        }
        confirmations.confirm(conv);
        LOG.info("addAccountToCustomer() customerId={} conversation={}", customerId, conv);
        return bankAiProvider.addAccountToCustomer(customerId, accountRequest);
    }

    public static class ConfirmationRequiredException extends RuntimeException {
        public ConfirmationRequiredException(String message) {
            super(message);
        }
    }
}
