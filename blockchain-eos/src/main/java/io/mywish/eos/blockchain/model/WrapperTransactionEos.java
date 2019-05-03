package io.mywish.eos.blockchain.model;

import io.mywish.blockchain.WrapperInput;
import io.mywish.eoscli4j.model.TransactionStatus;
import io.mywish.blockchain.WrapperOutput;
import io.mywish.blockchain.WrapperTransaction;
import lombok.Getter;

import java.util.List;

@Getter
public class WrapperTransactionEos extends WrapperTransaction {
    private final TransactionStatus status;

    public WrapperTransactionEos(String hash, List<WrapperInput> inputs, List<WrapperOutput> outputs, boolean contractCreation, TransactionStatus status) {
        super(hash, inputs, outputs, contractCreation);
        this.status = status;
    }
}
