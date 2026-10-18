package com.newbank.microservices.genai;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.newbank.microservices.genai.dto.AccountDetails;
import com.newbank.microservices.genai.dto.AccountRequest;
import com.newbank.microservices.genai.dto.Advisor;
import com.newbank.microservices.genai.dto.CustomerDetails;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import tools.jackson.core.JacksonException;

@Component
class BankTools {

    private static final Logger LOG = LoggerFactory.getLogger(BankTools.class);

    private final AIDataProvider bankAiProvider;

    BankTools(AIDataProvider bankAiProvider) {
        this.bankAiProvider = bankAiProvider;
    }

    @Tool(description = "List the customers in the bank system")
    public List<CustomerDetails> listCustomers() {
        LOG.info("listCustomers()");
        return bankAiProvider.getAllCustomers();
    }

    @Tool(description = "Add a new customer to the bank. The customer must include a first name and last name, an address, and a 10-digit phone number.")
    public CustomerDetails addCustomerToBank(CustomerRequest customerRequest) {
        LOG.info("addCustomerToBank() customerRequest={}", customerRequest);
        return bankAiProvider.addCustomerToBank(customerRequest);
    }

    @Tool(description = "List the advisors in the bank")
    public List<String> listAdvisors(@ToolParam(required = false) Advisor advisorRequest) {
        LOG.info("listAdvisors() advisorRequest={}", advisorRequest);
        try {
            return bankAiProvider.getAdvisors(advisorRequest);
        }
        catch (JacksonException e) {
            LOG.error("Error processing JSON in the listAdvisors function", e);
            return List.of();
        }
    }

    @Tool(description = "Add an account to a customer. Allowed account type IDs are 1=checking, 2=savings, 3=credit, 4=investment, 5=loan.")
    public AccountDetails addAccountToCustomer(@ToolParam(description = "Customer identifier") int customerId, AccountRequest accountRequest) {
        LOG.info("addAccountToCustomer() customerId={} accountRequest={}", customerId, accountRequest);
        return bankAiProvider.addAccountToCustomer(customerId, accountRequest);
    }
}

record CustomerRequest(@NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank String address,
        @NotBlank String city,
        @NotBlank @Digits(fraction = 0, integer = 12) String telephone) {
}
