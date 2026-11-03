package com.newbank.microservices.genai;

import java.util.Map;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.context.ToolContext;

import com.newbank.microservices.genai.dto.AccountRequest;
import com.newbank.microservices.genai.dto.CustomerRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * #3 + #13: constraints on the DTOs are actually enforced, and writes without
 * explicit confirmation are refused before touching any downstream service.
 */
class BankToolsConfirmationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void invalidCustomerRequest_rejected() {
        var bad = new CustomerRequest("", "Lovelace", "addr", "city", "not-a-phone");
        assertThat(validator.validate(bad)).isNotEmpty();
    }

    @Test
    void validCustomerRequest_passes() {
        var ok = new CustomerRequest("Ada", "Lovelace", "123 Main", "London", "0712345678");
        assertThat(validator.validate(ok)).isEmpty();
    }

    @Test
    void invalidAccountRequest_rejected() {
        var bad = new AccountRequest(0, null, "", 0);
        assertThat(validator.validate(bad)).isNotEmpty();
    }

    @Test
    void unconfirmedCustomerWrite_refusedWithoutDownstreamCall() {
        var data = mock(AIDataProvider.class);
        var tools = new BankTools(data, new ToolConfirmationService());
        var ctx = mock(ToolContext.class);
        when(ctx.getContext()).thenReturn(Map.of("conversationId", "conv-1", "confirmed", false));

        assertThatThrownBy(() -> tools.addCustomerToBank(
                new CustomerRequest("A", "B", "addr", "city", "0712345678"), ctx))
            .isInstanceOf(BankTools.ConfirmationRequiredException.class);
        verifyNoInteractions(data);
    }

    @Test
    void unconfirmedAccountWrite_refusedWithoutDownstreamCall() {
        var data = mock(AIDataProvider.class);
        var tools = new BankTools(data, new ToolConfirmationService());
        var ctx = mock(ToolContext.class);
        when(ctx.getContext()).thenReturn(Map.of("conversationId", "conv-1", "confirmed", false));

        assertThatThrownBy(() -> tools.addAccountToCustomer(1,
                new AccountRequest(1, new java.util.Date(), "Main", 1), ctx))
            .isInstanceOf(BankTools.ConfirmationRequiredException.class);
        verifyNoInteractions(data);
    }
}
