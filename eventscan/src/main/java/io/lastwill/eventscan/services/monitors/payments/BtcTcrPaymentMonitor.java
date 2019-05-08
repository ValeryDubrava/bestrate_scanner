package io.lastwill.eventscan.services.monitors.payments;

import io.lastwill.eventscan.events.model.PaymentEvent;
import io.lastwill.eventscan.model.NetworkProviderType;
import io.lastwill.eventscan.model.NetworkType;
import io.lastwill.eventscan.repositories.SubscriptionRepository;
import io.mywish.blockchain.WrapperInput;
import io.mywish.blockchain.WrapperOutput;
import io.mywish.blockchain.WrapperTransaction;
import io.mywish.scanner.model.NewBlockEvent;
import io.mywish.scanner.services.EventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
public class BtcTcrPaymentMonitor {
    @Autowired
    private EventPublisher eventPublisher;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    private Map<NetworkType, String> currencyByNetwork = new HashMap<>();

    @PostConstruct
    protected void init() {
        currencyByNetwork.put(NetworkType.BTC_MAINNET, "BTC");
        currencyByNetwork.put(NetworkType.BTC_TESTNET_3, "BTC");
        currencyByNetwork.put(NetworkType.TECRA_MAINNET, "TCR");
    }

    @EventListener
    private void handleTecraBlock(NewBlockEvent event) {
        if (event.getNetworkType().getNetworkProviderType() != NetworkProviderType.BTC) {
            return;
        }
        Set<String> addresses = event.getTransactionsByAddress().keySet();
        if (addresses.isEmpty()) {
            return;
        }
        subscriptionRepository.findSubscribedByAddressesListAndNetwork(addresses, event.getNetworkType())
                .forEach(subscription -> {
                    List<WrapperTransaction> transactions = event
                            .getTransactionsByAddress()
                            .get(subscription.getAddress());
                    if (transactions == null) {
                        log.warn("There is no Subscription entity found for TCR address {}.",
                                subscription.getAddress());
                        return;
                    }

                    for (WrapperTransaction tx : transactions) {
                        BigInteger inputValue = BigInteger.ZERO;
                        for (WrapperInput input : tx.getInputs()) {
                            if (input.getParentTransaction() == null) {
                                log.warn("Skip it. Input {} has not parent transaction.", input);
                                continue;
                            }
                            if (!input.getAddress().equalsIgnoreCase(subscription.getAddress())) {
                                continue;
                            }

                            inputValue = inputValue.add(input.getValue());
                        }

                        BigInteger outputValue = BigInteger.ZERO;
                        for (WrapperOutput output : tx.getOutputs()) {
                            if (output.getParentTransaction() == null) {
                                log.warn("Skip it. Output {} has not parent transaction.", output);
                                continue;
                            }
                            if (!output.getAddress().equalsIgnoreCase(subscription.getAddress())) {
                                continue;
                            }

                            outputValue = outputValue.add(output.getValue());
                        }

                        // write-off
                        if (!inputValue.equals(BigInteger.ZERO)) {
                            BigInteger amount = inputValue.subtract(outputValue).subtract(tx.getFee());

                            eventPublisher.publish(new PaymentEvent(
                                    subscription,
                                    event.getNetworkType(),
                                    event.getBlock(),
                                    tx,
                                    subscription.getAddress(),
                                    null,
                                    amount,
                                    tx.getFee(),
                                    null,
                                    currencyByNetwork.get(event.getNetworkType()),
                                    null,
                                    true
                            ));
                        }

                        // replenishment
                        if (!outputValue.equals(BigInteger.ZERO) && inputValue.equals(BigInteger.ZERO)) {
                            eventPublisher.publish(new PaymentEvent(
                                    subscription,
                                    event.getNetworkType(),
                                    event.getBlock(),
                                    tx,
                                    null,
                                    subscription.getAddress(),
                                    outputValue,
                                    tx.getFee(),
                                    null,
                                    currencyByNetwork.get(event.getNetworkType()),
                                    null,
                                    true
                            ));
                        }
                    }
                });
    }
}
