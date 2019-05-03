package io.mywish.eos.blockchain;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.lastwill.eventscan.events.EventModule;
import io.lastwill.eventscan.model.NetworkType;
import io.lastwill.eventscan.repositories.LastBlockRepository;
import io.mywish.eos.blockchain.services.EosNetwork;
import io.mywish.eos.blockchain.services.EosScanner;
import io.mywish.eos.blockchain.services.EosScannerPolling;
import io.mywish.eoscli4j.service.EosClientImpl;
import io.mywish.scanner.services.LastBlockDbPersister;
import io.mywish.scanner.services.LastBlockMemoryPersister;
import io.mywish.scanner.services.LastBlockPersister;
import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.net.URI;

@Configuration
@ComponentScan
@Import(EventModule.class)
public class EosBCModule {
    @ConditionalOnProperty(name = {"etherscanner.eos.rpc-url.testnet", "etherscanner.eos.tcp-url.testnet"})
    @Bean(name = NetworkType.EOS_TESTNET_VALUE)
    public EosNetwork eosNetTest(
            final CloseableHttpClient closeableHttpClient,
            final ObjectMapper objectMapper,
            final @Value("${etherscanner.eos.tcp-url.testnet}") URI rcpUrl,
            final @Value("${etherscanner.eos.rpc-url.testnet}") URI rpc
    ) throws Exception {
        return new EosNetwork(
                NetworkType.EOS_TESTNET,
                new EosClientImpl(
                        rcpUrl,
                        closeableHttpClient,
                        rpc,
                        objectMapper
                )
        );
    }

    @ConditionalOnProperty(name = {"etherscanner.eos.rpc-url.mainnet", "etherscanner.eos.tcp-url.mainnet"})
    @Bean(name = NetworkType.EOS_MAINNET_VALUE)
    public EosNetwork eosNetMain(
            final CloseableHttpClient closeableHttpClient,
            final ObjectMapper objectMapper,
            final @Value("${etherscanner.eos.tcp-url.mainnet}") URI tcpUrl,
            final @Value("${etherscanner.eos.rpc-url.mainnet}") URI rpc
    ) throws Exception {
        return new EosNetwork(
                NetworkType.EOS_MAINNET,
                new EosClientImpl(
                        tcpUrl,
                        closeableHttpClient,
                        rpc,
                        objectMapper
                )
        );
    }

    @Bean
    public LastBlockPersister eosMainnetLastBlockPersister(
            LastBlockRepository lastBlockRepository,
            final @Value("${etherscanner.eos.last-block.mainnet:#{null}}") Long lastBlock
    ) {
        return new LastBlockDbPersister(NetworkType.EOS_MAINNET, lastBlockRepository, lastBlock);
    }

    @Bean
    public LastBlockPersister eosTestnetLastBlockPersister(
            LastBlockRepository lastBlockRepository,
            final @Value("${etherscanner.eos.last-block.testnet:#{null}}") Long lastBlock
    ) {
        return new LastBlockDbPersister(NetworkType.EOS_TESTNET, lastBlockRepository, lastBlock);
    }

    @ConditionalOnBean(name = NetworkType.EOS_MAINNET_VALUE)
    @ConditionalOnProperty(name = {"etherscanner.eos.pending", "etherscanner.eos.subscription.mainnet"})
    @Bean
    public EosScanner eosScannerMain(
            final @Qualifier(NetworkType.EOS_MAINNET_VALUE) EosNetwork network,
            final @Qualifier("eosMainnetLastBlockPersister") LastBlockPersister lastBlockPersister
    ) {
        return new EosScanner(
                network,
                lastBlockPersister,
                false
        );
    }

    @ConditionalOnBean(name = NetworkType.EOS_MAINNET_VALUE)
    @ConditionalOnProperty(name = "etherscanner.eos.subscription.mainnet")
    @Bean
    public EosScanner eosPendingScannerMain(
            final @Qualifier(NetworkType.EOS_MAINNET_VALUE) EosNetwork network,
            final @Value("${etherscanner.eos.last-block.mainnet:#{null}}") Long lastBlock
    ) {
        return new EosScanner(
                network,
                new LastBlockMemoryPersister(lastBlock),
                true
        );
    }

    @ConditionalOnBean(name = NetworkType.EOS_MAINNET_VALUE)
    @ConditionalOnMissingBean(name = {"eosScannerMain", "eosPendingScannerMain"})
    @ConditionalOnProperty(name = "etherscanner.eos.pending")
    @Bean
    public EosScannerPolling eosScannerPollingMain(
            final @Qualifier(NetworkType.EOS_MAINNET_VALUE) EosNetwork network,
            final @Qualifier("eosMainnetLastBlockPersister") LastBlockPersister lastBlockPersister,
            final @Value("${etherscanner.eos.polling-interval-ms:500}") Long pollingInterval,
            final @Value("${etherscanner.eos.commit-chain-length:5}") Integer commitmentChainLength
    ) {
        return new EosScannerPolling(
                network,
                lastBlockPersister,
                pollingInterval,
                commitmentChainLength,
                false
        );
    }

    @ConditionalOnBean(name = NetworkType.EOS_MAINNET_VALUE)
    @ConditionalOnMissingBean(name = {"eosScannerMain", "eosPendingScannerMain"})
    @Bean
    public EosScannerPolling eosPendingScannerPollingMain(
            final @Qualifier(NetworkType.EOS_MAINNET_VALUE) EosNetwork network,
            final @Value("${etherscanner.eos.last-block.mainnet:#{null}}") Long lastBlock,
            final @Value("${etherscanner.eos.polling-interval-ms:500}") Long pollingInterval,
            final @Value("${etherscanner.eos.commit-chain-length:5}") Integer commitmentChainLength
    ) {
        return new EosScannerPolling(
                network,
                new LastBlockMemoryPersister(lastBlock),
                pollingInterval,
                commitmentChainLength,
                true
        );
    }

    @ConditionalOnBean(name = NetworkType.EOS_TESTNET_VALUE)
    @ConditionalOnProperty(name = {"etherscanner.eos.pending", "etherscanner.eos.subscription.testnet"})
    @Bean
    public EosScanner eosScannerTest(
            final @Qualifier(NetworkType.EOS_TESTNET_VALUE) EosNetwork network,
            final @Qualifier("eosTestnetLastBlockPersister") LastBlockPersister lastBlockPersister
    ) {
        return new EosScanner(
                network,
                lastBlockPersister,
                false
        );
    }

    @ConditionalOnBean(name = NetworkType.EOS_TESTNET_VALUE)
    @ConditionalOnProperty(name = "etherscanner.eos.subscription.testnet")
    @Bean
    public EosScanner eosPendingScannerTest(
            final @Qualifier(NetworkType.EOS_TESTNET_VALUE) EosNetwork network,
            final @Value("${etherscanner.eos.last-block.testnet:#{null}}") Long lastBlock
    ) {
        return new EosScanner(
                network,
                new LastBlockMemoryPersister(lastBlock),
                true
        );
    }

    @ConditionalOnBean(name = NetworkType.EOS_TESTNET_VALUE)
    @ConditionalOnMissingBean(name = {"eosScannerTest", "eosPendingScannerTest"})
    @ConditionalOnProperty(name = "etherscanner.eos.pending")
    @Bean
    public EosScannerPolling eosScannerPollingTest(
            final @Qualifier(NetworkType.EOS_TESTNET_VALUE) EosNetwork network,
            final @Qualifier("eosTestnetLastBlockPersister") LastBlockPersister lastBlockPersister,
            final @Value("${etherscanner.eos.polling-interval-ms:500}") Long pollingInterval,
            final @Value("${etherscanner.eos.commit-chain-length:5}") Integer commitmentChainLength
    ) {
        return new EosScannerPolling(
                network,
                lastBlockPersister,
                pollingInterval,
                commitmentChainLength,
                false
        );
    }

    @ConditionalOnBean(name = NetworkType.EOS_TESTNET_VALUE)
    @ConditionalOnMissingBean(name = {"eosScannerTest", "eosPendingScannerTest"})
    @Bean
    public EosScannerPolling eosPendingScannerPollingTest(
            final @Qualifier(NetworkType.EOS_TESTNET_VALUE) EosNetwork network,
            final @Value("${etherscanner.eos.last-block.testnet:#{null}}") Long lastBlock,
            final @Value("${etherscanner.eos.polling-interval-ms:500}") Long pollingInterval,
            final @Value("${etherscanner.eos.commit-chain-length:5}") Integer commitmentChainLength
    ) {
        return new EosScannerPolling(
                network,
                new LastBlockMemoryPersister(lastBlock),
                pollingInterval,
                commitmentChainLength,
                true
        );
    }
}
