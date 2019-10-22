package org.bitcoinj.core;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by Stouse49 on 9/17/18.
 */
public class CheckpointMessage extends Message {
    int messageSize;
    long version;
    Sha256Hash hashCheckpoint;
    byte [] signature;

    CheckpointMessage(NetworkParameters params, byte [] payload) {
        super(params, payload, 0);
    }

    @Override
    protected void parse() throws ProtocolException {
        messageSize = (int)readVarInt();
        version = readUint32();
        hashCheckpoint = readHash();
        signature = readByteArray();
        length = cursor - offset;
    }

    @Override
    protected void bitcoinSerializeToStream(OutputStream stream) throws IOException {
        stream.write(new VarInt(36 + signature.length).encode());
        Utils.uint32ToByteStreamLE(version, stream);
        stream.write(hashCheckpoint.getReversedBytes());
        stream.write(new VarInt(signature.length).encode());
        stream.write(signature);
    }

    Sha256Hash getSignatureHash() {
        try {
            UnsafeByteArrayOutputStream stream = new UnsafeByteArrayOutputStream(36);
            Utils.uint32ToByteStreamLE(version, stream);
            stream.write(hashCheckpoint.getReversedBytes());
            return Sha256Hash.twiceOf(stream.toByteArray());
        } catch (IOException x) {
            throw new RuntimeException(x);
        }
    }

    boolean checkSignature() {
        ECKey checkpointPubKey = ECKey.fromPublicOnly(Utils.HEX.decode(params.getCheckpointPublicKey()));
        return checkpointPubKey.verify(getSignatureHash().getBytes(), signature);
    }
}
