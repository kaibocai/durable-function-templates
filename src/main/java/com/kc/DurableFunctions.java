/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.kc;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;

import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import com.microsoft.durabletask.*;
import com.microsoft.durabletask.azurefunctions.DurableActivityTrigger;
import com.microsoft.durabletask.azurefunctions.DurableClientContext;
import com.microsoft.durabletask.azurefunctions.DurableClientInput;
import com.microsoft.durabletask.azurefunctions.DurableOrchestrationTrigger;

import java.util.Optional;

/**
 * Azure Functions with HTTP Trigger.
 */
public class DurableFunctions {
    /**
     * This HTTP-triggered function starts the orchestration.
     */
    @FunctionName("StartHelloCities")
    public HttpResponseMessage startHelloCities(
            @HttpTrigger(name = "req", methods = {HttpMethod.GET, HttpMethod.POST}, authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<String>> request,
            @DurableClientInput(name = "durableContext") DurableClientContext durableContext,
            final ExecutionContext context) {

        DurableTaskClient client = durableContext.getClient();
        String instanceId = client.scheduleNewOrchestrationInstance("HelloCities");
        context.getLogger().info("Created new Java orchestration with instance ID = " + instanceId);
        return durableContext.createCheckStatusResponse(request, instanceId);
    }

    /**
     * This is the orchestrator function. The OrchestrationRunner.loadAndRun() static
     * method is used to take the function input and execute the orchestrator logic.
     */
    @FunctionName("HelloCities")
    public String helloCitiesOrchestrator(
            @DurableOrchestrationTrigger(name = "orchestratorRequestProtoBytes") String orchestratorRequestProtoBytes) {
        return OrchestrationRunner.loadAndRun(orchestratorRequestProtoBytes, ctx -> {
            String result = "";
            result += ctx.callActivity("SayHello", "Tokyo", String.class).await() + ", ";
            result += ctx.callActivity("SayHello", "Seattle", String.class).await();
            result += ctx.callActivity("SayHello", "London", String.class).await();
            return result;
        });
    }

    /**
     * This is the activity function that gets invoked by the orchestration.
     */
    @FunctionName("SayHello")
    public String sayHello(
            @DurableActivityTrigger(name = "name") String name,
            final ExecutionContext context) {
        context.getLogger().info("Saying hello to: " + name);
        return String.format("Hello %s!", name);
    }
}
