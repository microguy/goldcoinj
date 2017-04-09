/*
 * Copyright 2013 Google Inc.
 * Copyright 2015 Andreas Schildbach
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.bitcoinj.params;

import org.bitcoinj.core.*;
import org.bitcoinj.net.discovery.*;

import java.net.*;

import static com.google.common.base.Preconditions.*;

/**
 * Parameters for the main production network on which people trade goods and services.
 */
public class MainNetParams extends AbstractBitcoinNetParams {
    public static final int MAINNET_MAJORITY_WINDOW = 1000;
    public static final int MAINNET_MAJORITY_REJECT_BLOCK_OUTDATED = 950;
    public static final int MAINNET_MAJORITY_ENFORCE_BLOCK_UPGRADE = 750;

    public MainNetParams() {
        super();
        interval = INTERVAL;
        targetTimespan = TARGET_TIMESPAN;
        maxTarget = Utils.decodeCompactBits(0x1e0fffffL);
        dumpedPrivateKeyHeader = 128 + 32;
        addressHeader = 32;
        p2shHeader = 5;
        acceptableAddressCodes = new int[] { addressHeader, p2shHeader };
        port = 8121;
        packetMagic = 0xfdc2b4ddL;
        bip32HeaderPub = 0x0488B21E; //The 4 byte header that serializes in base58 to "xpub".
        bip32HeaderPriv = 0x0488ADE4; //The 4 byte header that serializes in base58 to "xprv"

        majorityEnforceBlockUpgrade = MAINNET_MAJORITY_ENFORCE_BLOCK_UPGRADE;
        majorityRejectBlockOutdated = MAINNET_MAJORITY_REJECT_BLOCK_OUTDATED;
        majorityWindow = MAINNET_MAJORITY_WINDOW;

        genesisBlock.setDifficultyTarget(0x1e0ffff0L);
        genesisBlock.setTime(1368560876L);
        genesisBlock.setNonce(3591624);
        id = ID_MAINNET;
        subsidyDecreaseBlockCount = 840000;
        spendableCoinbaseDepth = 100;
        genesisBlock.setMerkleRoot(Sha256Hash.wrap("a215e67ba165202f75b6458d22fedd1a3ec4f03449a4c6b2a4b8130bfebd3b15"));

        String genesisHash = genesisBlock.getHashAsString();
        checkState(genesisHash.equals("dced3542896ed537cb06f9cb064319adb0da615f64dd8c5e5bad974398f44b24"),
                genesisHash);

        // This contains (at a minimum) the blocks which are not BIP30 compliant. BIP30 changed how duplicate
        // transactions are handled. Duplicated transactions could occur in the case where a coinbase had the same
        // extraNonce and the same outputs but appeared at different heights, and greatly complicated re-org handling.
        // Having these here simplifies block connection logic considerably.

        checkpoints.put(0, Sha256Hash.wrap("dced3542896ed537cb06f9cb064319adb0da615f64dd8c5e5bad974398f44b24"));
        checkpoints.put( 1, Sha256Hash.wrap("e39be079a4e57af79f63edb2726bdcb401ae520fa5b5328bbeab185b5b3d636e"));
        checkpoints.put( 50,Sha256Hash.wrap("34b021706ae2b72e41d411a31ead78219087213a29fe338515926f055a4c5655")) ;
        checkpoints.put( 150,Sha256Hash.wrap("c3565831a172cf737b0dd8bd47a81f2dfe99012c97362f397838e617d26ba99b"));
        checkpoints.put( 300,Sha256Hash.wrap("52d9e6919828769b358db66f8793a06e5915948d0a0ee36f59465e91dabb97a6"));
        checkpoints.put( 500,Sha256Hash.wrap("6ba9043e14339790c56181da3ef4d87ff58b8cd3c07b2d33e7d808e189136002"));
        checkpoints.put( 1000,Sha256Hash.wrap("29607502895cf180b7b556f0bc70b2001eacb819be6b4be1e5f0092622e03cb3"));
        checkpoints.put( 6048, Sha256Hash.wrap("e8eb14ac03f25fd52a46b51acba8af543f6b4baf1b85b62d6e6e09ba5f108ed7"));
        checkpoints.put( 12096, Sha256Hash.wrap("d2bd87d677cced55584634d6d03434e041d5dbbaf5639c873a5f6a654d788ad1"));
        checkpoints.put( 16128, Sha256Hash.wrap("290d05c1f5fb2cc75d5bc7657141e4e42d4ee7b4e78a73c96fac878a58d52057"));
        checkpoints.put( 18143, Sha256Hash.wrap("996caca04cce6ffc7f1053abcd350742e2ecc3220663cfd3e3585e3442d7cf74"));
        checkpoints.put( 18144, Sha256Hash.wrap("ed5124e191b92d5405374dc4018203a7e03f4251293b6d5f57a83d1e1ff4df30"));
        checkpoints.put( 20160, Sha256Hash.wrap("e19b119f4a633d89320d502e7c05b88d083acdff3b4bd40efcdca54b25f6cb2c"));
        checkpoints.put( 20500, Sha256Hash.wrap("23ab64ad71d7191c28010c7c1b0b35d32ac97ace893dbb20068a6abb617f80a4"));
        checkpoints.put( 24000, Sha256Hash.wrap("c3cf2892cbaaf8b88565f027460bda831a428bf6ea76fafa870bf3586dd07c5f"));
        checkpoints.put( 26000, Sha256Hash.wrap("906a76b3b36aa7f489ea0ee38c180d0eebaf489e4998e6aefa806fadc687e475"));
        checkpoints.put( 27000, Sha256Hash.wrap("f7391f58e29d057f152b9c124af6153dadb62385d8728118e9cef728d9a4d16d"));
        checkpoints.put( 27000, Sha256Hash.wrap("f7391f58e29d057f152b9c124af6153dadb62385d8728118e9cef728d9a4d16d"));
        checkpoints.put( 28000, Sha256Hash.wrap("28adf712f2a7d9d7ab3836249c9e2beff8d0deb362a1991c61cb61c0fe9af10b"));
        checkpoints.put( 29000, Sha256Hash.wrap("0aca7e1d7cebe224479db62d9887bba7e8dbf5cc295261b6b2e9b9bc76f58ab1"));
        checkpoints.put( 29000, Sha256Hash.wrap("0aca7e1d7cebe224479db62d9887bba7e8dbf5cc295261b6b2e9b9bc76f58ab1"));
        checkpoints.put( 30000, Sha256Hash.wrap("1ff80eac17ba7efc350d65d842cbedd5822b4bef3eae7b1c24424c0d5cc2af51"));
        checkpoints.put( 31000, Sha256Hash.wrap("e9a24595526e9c67357e3a5962e8d489a867573eef1ea104de6be113d26512de"));
        checkpoints.put( 35000, Sha256Hash.wrap("0d8f14bc84ed93490b8c2070de4b744085a4d661f7ef96c856fd32572bbd87fc"));
        checkpoints.put( 45000, Sha256Hash.wrap("612461aaa0ca6a3bc07238ac86e67fa37ae1b8b083d0c1e23f396bbe5bd05896"));
        checkpoints.put( 50000, Sha256Hash.wrap("e251895683ec1363344504b91d9899c29064afc786905c9a052d61ee88a95206"));
        checkpoints.put( 60000, Sha256Hash.wrap("f03feaab75843a39be8cf0fbf8bdae3056aebd4817b89a99e4837db2bdd2659a"));
        checkpoints.put( 65000, Sha256Hash.wrap("b635ce68527e8b777f68a71fe441faab285fa7aafd78259ddc24843539bba369"));
        checkpoints.put( 66000, Sha256Hash.wrap("f619fc8b01c1aedcf4623cea7d85310db85174e27e1b3069dadf76e9bc2f6c99"));
        checkpoints.put(103000, Sha256Hash.wrap("e41285ba8cd299b28f0dbfc60b28a9c3e5b6482079d4608ef0dad14390ce6da7"));
        checkpoints.put(115300, Sha256Hash.wrap("77b3f1913755a4d381f36cd134f280a6519dd54d1c33c499eeded256f36bb394"));
        checkpoints.put(246100, Sha256Hash.wrap("ca7ed43264e0e8965d4115d2f8d3fabb63dcb430fe502d31796c63658ab8274d"));
        checkpoints.put(300000, Sha256Hash.wrap("e81d2c84c9e7332b35788c1166a0b2c9a34be4d17f08f44a9ba2f5edd82dc300"));
        checkpoints.put(372000, Sha256Hash.wrap("e3d2857896d0f52ac502eb056ac23f416cb7eddb0a6eba68785b940cca8257ee"));
        checkpoints.put(564000, Sha256Hash.wrap("9d67ce445d6b513074ef061066bb331871901b953b3bdeaa4dc0a4043cf189f8"));


        dnsSeeds = new String[] {
                "dnsseed.gldcoin.com",
                "seed.gldcoin.com",
                "vps.gldcoin.com",
                /*"104.158.54.162",
                "173.208.211.58",
                "189.229.156.221",
                "192.169.7.18",
                "200.93.92.30",
                "24.54.97.47",
                "77.67.46.103",
                "80.86.85.65",
                "82.17.177.183",
                "89.23.188.53",
                "107.161.23.121",
                "24.54.97.47",
                "167.160.36.162",*/
        };
        httpSeeds = null; /*new HttpDiscovery.Details[] {
                // Andreas Schildbach
                new HttpDiscovery.Details(
                        ECKey.fromPublicOnly(Utils.HEX.decode("0238746c59d46d5408bf8b1d0af5740fe1a6e1703fcb56b2953f0b965c740d256f")),
                        URI.create("http://httpseed.bitcoin.schildbach.de/peers")
                )
        };*/

        addrSeeds = null;/*new int[] {
                0x1ddb1032, 0x6242ce40, 0x52d6a445, 0x2dd7a445, 0x8a53cd47, 0x73263750, 0xda23c257, 0xecd4ed57,
                0x0a40ec59, 0x75dce160, 0x7df76791, 0x89370bad, 0xa4f214ad, 0x767700ae, 0x638b0418, 0x868a1018,
                0xcd9f332e, 0x0129653e, 0xcc92dc3e, 0x96671640, 0x56487e40, 0x5b66f440, 0xb1d01f41, 0xf1dc6041,
                0xc1d12b42, 0x86ba1243, 0x6be4df43, 0x6d4cef43, 0xd18e0644, 0x1ab0b344, 0x6584a345, 0xe7c1a445,
                0x58cea445, 0xc5daa445, 0x21dda445, 0x3d3b5346, 0x13e55347, 0x1080d24a, 0x8e611e4b, 0x81518e4b,
                0x6c839e4b, 0xe2ad0a4c, 0xfbbc0a4c, 0x7f5b6e4c, 0x7244224e, 0x1300554e, 0x20690652, 0x5a48b652,
                0x75c5c752, 0x4335cc54, 0x340fd154, 0x87c07455, 0x087b2b56, 0x8a133a57, 0xac23c257, 0x70374959,
                0xfb63d45b, 0xb9a1685c, 0x180d765c, 0x674f645d, 0x04d3495e, 0x1de44b5e, 0x4ee8a362, 0x0ded1b63,
                0xc1b04b6d, 0x8d921581, 0x97b7ea82, 0x1cf83a8e, 0x91490bad, 0x09dc75ae, 0x9a6d79ae, 0xa26d79ae,
                0x0fd08fae, 0x0f3e3fb2, 0x4f944fb2, 0xcca448b8, 0x3ecd6ab8, 0xa9d5a5bc, 0x8d0119c1, 0x045997d5,
                0xca019dd9, 0x0d526c4d, 0xabf1ba44, 0x66b1ab55, 0x1165f462, 0x3ed7cbad, 0xa38fae6e, 0x3bd2cbad,
                0xd36f0547, 0x20df7840, 0x7a337742, 0x549f8e4b, 0x9062365c, 0xd399f562, 0x2b5274a1, 0x8edfa153,
                0x3bffb347, 0x7074bf58, 0xb74fcbad, 0x5b5a795b, 0x02fa29ce, 0x5a6738d4, 0xe8a1d23e, 0xef98c445,
                0x4b0f494c, 0xa2bc1e56, 0x7694ad63, 0xa4a800c3, 0x05fda6cd, 0x9f22175e, 0x364a795b, 0x536285d5,
                0xac44c9d4, 0x0b06254d, 0x150c2fd4, 0x32a50dcc, 0xfd79ce48, 0xf15cfa53, 0x66c01e60, 0x6bc26661,
                0xc03b47ae, 0x4dda1b81, 0x3285a4c1, 0x883ca96d, 0x35d60a4c, 0xdae09744, 0x2e314d61, 0x84e247cf,
                0x6c814552, 0x3a1cc658, 0x98d8f382, 0xe584cb5b, 0x15e86057, 0x7b01504e, 0xd852dd48, 0x56382f56,
                0x0a5df454, 0xa0d18d18, 0x2e89b148, 0xa79c114c, 0xcbdcd054, 0x5523bc43, 0xa9832640, 0x8a066144,
                0x3894c3bc, 0xab76bf58, 0x6a018ac1, 0xfebf4f43, 0x2f26c658, 0x31102f4e, 0x85e929d5, 0x2a1c175e,
                0xfc6c2cd1, 0x27b04b6d, 0xdf024650, 0x161748b8, 0x28be6580, 0x57be6580, 0x1cee677a, 0xaa6bb742,
                0x9a53964b, 0x0a5a2d4d, 0x2434c658, 0x9a494f57, 0x1ebb0e48, 0xf610b85d, 0x077ecf44, 0x085128bc,
                0x5ba17a18, 0x27ca1b42, 0xf8a00b56, 0xfcd4c257, 0xcf2fc15e, 0xd897e052, 0x4cada04f, 0x2f35f6d5,
                0x382ce8c9, 0xe523984b, 0x3f946846, 0x60c8be43, 0x41da6257, 0xde0be142, 0xae8a544b, 0xeff0c254,
                0x1e0f795b, 0xaeb28890, 0xca16acd9, 0x1e47ddd8, 0x8c8c4829, 0xd27dc747, 0xd53b1663, 0x4096b163,
                0x9c8dd958, 0xcb12f860, 0x9e79305c, 0x40c1a445, 0x4a90c2bc, 0x2c3a464d, 0x2727f23c, 0x30b04b6d,
                0x59024cb8, 0xa091e6ad, 0x31b04b6d, 0xc29d46a6, 0x63934fb2, 0xd9224dbe, 0x9f5910d8, 0x7f530a6b,
                0x752e9c95, 0x65453548, 0xa484be46, 0xce5a1b59, 0x710e0718, 0x46a13d18, 0xdaaf5318, 0xc4a8ff53,
                0x87abaa52, 0xb764cf51, 0xb2025d4a, 0x6d351e41, 0xc035c33e, 0xa432c162, 0x61ef34ae, 0xd16fddbc,
                0x0870e8c1, 0x3070e8c1, 0x9c71e8c1, 0xa4992363, 0x85a1f663, 0x4184e559, 0x18d96ed8, 0x17b8dbd5,
                0x60e7cd18, 0xe5ee104c, 0xab17ac62, 0x1e786e1b, 0x5d23b762, 0xf2388fae, 0x88270360, 0x9e5b3d80,
                0x7da518b2, 0xb5613b45, 0x1ad41f3e, 0xd550854a, 0x8617e9a9, 0x925b229c, 0xf2e92542, 0x47af0544,
                0x73b5a843, 0xb9b7a0ad, 0x03a748d0, 0x0a6ff862, 0x6694df62, 0x3bfac948, 0x8e098f4f, 0x746916c3,
                0x02f38e4f, 0x40bb1243, 0x6a54d162, 0x6008414b, 0xa513794c, 0x514aa343, 0x63781747, 0xdbb6795b,
                0xed065058, 0x42d24b46, 0x1518794c, 0x9b271681, 0x73e4ffad, 0x0654784f, 0x438dc945, 0x641846a6,
                0x2d1b0944, 0x94b59148, 0x8d369558, 0xa5a97662, 0x8b705b42, 0xce9204ae, 0x8d584450, 0x2df61555,
                0xeebff943, 0x2e75fb4d, 0x3ef8fc57, 0x9921135e, 0x8e31042e, 0xb5afad43, 0x89ecedd1, 0x9cfcc047,
                0x8fcd0f4c, 0xbe49f5ad, 0x146a8d45, 0x98669ab8, 0x98d9175e, 0xd1a8e46d, 0x839a3ab8, 0x40a0016c,
                0x6d27c257, 0x977fffad, 0x7baa5d5d, 0x1213be43, 0xb167e5a9, 0x640fe8ca, 0xbc9ea655, 0x0f820a4c,
                0x0f097059, 0x69ac957c, 0x366d8453, 0xb1ba2844, 0x8857f081, 0x70b5be63, 0xc545454b, 0xaf36ded1,
                0xb5a4b052, 0x21f062d1, 0x72ab89b2, 0x74a45318, 0x8312e6bc, 0xb916965f, 0x8aa7c858, 0xfe7effad,
        };*/
    }

    private static MainNetParams instance;
    public static synchronized MainNetParams get() {
        if (instance == null) {
            instance = new MainNetParams();
        }
        return instance;
    }

    @Override
    public String getPaymentProtocolId() {
        return PAYMENT_PROTOCOL_ID_MAINNET;
    }
}
