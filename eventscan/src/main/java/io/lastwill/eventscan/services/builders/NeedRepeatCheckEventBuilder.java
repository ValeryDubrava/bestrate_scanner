package io.lastwill.eventscan.services.builders;

import io.lastwill.eventscan.events.contract.CheckedEvent;
import io.lastwill.eventscan.events.contract.ContractEventDefinition;
import lombok.Getter;
import org.springframework.stereotype.Component;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.Type;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.util.Collections;
import java.util.List;

@Getter
@Component
public class NeedRepeatCheckEventBuilder extends ContractEventBuilder<CheckedEvent> {
    private final ContractEventDefinition definition = new ContractEventDefinition(
            "NeedRepeatCheck",
            Collections.emptyList(),
            Collections.singletonList(TypeReference.create(Bool.class))
    );

    @Override
    public CheckedEvent build(TransactionReceipt transactionReceipt, String address, List<Type> indexedValues, List<Type> nonIndexedValues) {
        return new CheckedEvent(definition, transactionReceipt, (Boolean) nonIndexedValues.get(0).getValue(), address);
    }
}
