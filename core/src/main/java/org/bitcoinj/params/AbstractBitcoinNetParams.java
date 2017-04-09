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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import org.bitcoinj.core.Block;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.StoredBlock;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.Utils;
import org.bitcoinj.utils.MonetaryFormat;
import org.bitcoinj.core.VerificationException;
import org.bitcoinj.store.BlockStore;
import org.bitcoinj.store.BlockStoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Stopwatch;

import org.bitcoinj.core.BitcoinSerializer;

import static com.google.common.base.Preconditions.checkState;

/**
 * Parameters for Bitcoin-like networks.
 */
public abstract class AbstractBitcoinNetParams extends NetworkParameters {
    /**
     * Scheme part for Bitcoin URIs.
     */
    public static final String BITCOIN_SCHEME = "goldcoin";

    private static final Logger log = LoggerFactory.getLogger(AbstractBitcoinNetParams.class);

    public AbstractBitcoinNetParams() {
        super();
    }

    /** 
     * Checks if we are at a difficulty transition point. 
     * @param storedPrev The previous stored block 
     * @return If this is a difficulty transition point 
     */
    protected boolean isDifficultyTransitionPoint(StoredBlock storedPrev) {
        return ((storedPrev.getHeight() + 1) % this.getInterval()) == 0;
    }


    public void checkDifficultyTransitions_btc(final StoredBlock storedPrev, final Block nextBlock,
    	final BlockStore blockStore) throws VerificationException, BlockStoreException {
        Block prev = storedPrev.getHeader();

        // Is this supposed to be a difficulty transition point?
        if (!isDifficultyTransitionPoint(storedPrev)) {

            // No ... so check the difficulty didn't actually change.
            if (nextBlock.getDifficultyTarget() != prev.getDifficultyTarget())
                throw new VerificationException("Unexpected change in difficulty at height " + storedPrev.getHeight() +
                        ": " + Long.toHexString(nextBlock.getDifficultyTarget()) + " vs " +
                        Long.toHexString(prev.getDifficultyTarget()));
            return;
        }

        // We need to find a block far back in the chain. It's OK that this is expensive because it only occurs every
        // two weeks after the initial block chain download.
        final Stopwatch watch = Stopwatch.createStarted();
        StoredBlock cursor = blockStore.get(prev.getHash());
        for (int i = 0; i < this.getInterval() - 1; i++) {
            if (cursor == null) {
                // This should never happen. If it does, it means we are following an incorrect or busted chain.
                throw new VerificationException(
                        "Difficulty transition point but we did not find a way back to the genesis block.");
            }
            cursor = blockStore.get(cursor.getHeader().getPrevBlockHash());
        }
        watch.stop();
        if (watch.elapsed(TimeUnit.MILLISECONDS) > 50)
            log.info("Difficulty transition traversal took {}", watch);

        Block blockIntervalAgo = cursor.getHeader();
        int timespan = (int) (prev.getTimeSeconds() - blockIntervalAgo.getTimeSeconds());
        // Limit the adjustment step.
        final int targetTimespan = this.getTargetTimespan();
        if (timespan < targetTimespan / 4)
            timespan = targetTimespan / 4;
        if (timespan > targetTimespan * 4)
            timespan = targetTimespan * 4;

        BigInteger newTarget = Utils.decodeCompactBits(prev.getDifficultyTarget());
        newTarget = newTarget.multiply(BigInteger.valueOf(timespan));
        newTarget = newTarget.divide(BigInteger.valueOf(targetTimespan));

        if (newTarget.compareTo(this.getMaxTarget()) > 0) {
            log.info("Difficulty hit proof of work limit: {}", newTarget.toString(16));
            newTarget = this.getMaxTarget();
        }

        int accuracyBytes = (int) (nextBlock.getDifficultyTarget() >>> 24) - 3;
        long receivedTargetCompact = nextBlock.getDifficultyTarget();

        // The calculated difficulty is to a higher precision than received, so reduce here.
        BigInteger mask = BigInteger.valueOf(0xFFFFFFL).shiftLeft(accuracyBytes * 8);
        newTarget = newTarget.and(mask);
        long newTargetCompact = Utils.encodeCompactBits(newTarget);

        if (newTargetCompact != receivedTargetCompact)
            throw new VerificationException("Network provided difficulty bits do not match what was calculated: " +
                    Long.toHexString(newTargetCompact) + " vs " + Long.toHexString(receivedTargetCompact));
    }

    static final long julyFork = 45000;
    static final long novemberFork = 103000;
    static final long novemberFork2 = 118800;
    static final long mayFork = 248000;
    static final long octoberFork = 100000;

    static final long julyFork2 = 251230;

    static boolean hardForkedJuly;
    static boolean hardForkedNovember;

    static final long nTargetTimespan = (2 * 60 * 60);// Difficulty changes every 60 blocks
    static final long nTargetSpacing = 2 * 60;

    @Override
    public void checkDifficultyTransitions(StoredBlock pindexLast, Block pblock,
                                            final BlockStore blockStore) throws BlockStoreException, VerificationException {
        boolean fTestNet = getId().equals(NetworkParameters.ID_TESTNET);
        Block prev = pindexLast.getHeader();

        //Todo:: Clean this mess up.. -akumaburn
        BigInteger bnProofOfWorkLimit = maxTarget;
        BigInteger bnNew;



        // Genesis block
        if (pindexLast == null) {
            verifyDifficulty(maxTarget, pblock);
            return;
        }

        // FeatherCoin difficulty adjustment protocol switch
        final int nDifficultySwitchHeight = 21000;
        int nHeight = pindexLast.getHeight() + 1;
        boolean fNewDifficultyProtocol = (nHeight >= nDifficultySwitchHeight || fTestNet);

        //julyFork2 whether or not we had a massive difficulty fall authorized
        boolean didHalfAdjust = false;

        //moved to solve scope issues
        long averageTime = 120;

        if(nHeight < julyFork) {
            //if(!hardForkedJuly) {
            long nTargetTimespan2 = (7 * 24 * 60 * 60) / 8;
            long nTargetSpacing2 = (long)(2.5 * 60);

            long nTargetTimespan2Current = fNewDifficultyProtocol? nTargetTimespan2 : (nTargetTimespan2*4);
            long nInterval = nTargetTimespan2Current / nTargetSpacing2;

            // Only change once per interval, or at protocol switch height
            if ((nHeight % nInterval != 0) &&
                    (nHeight != nDifficultySwitchHeight || fTestNet))
            {
                // Special difficulty rule for testnet:
                if (fTestNet)
                {
                    // If the new block's timestamp is more than 2* 10 minutes
                    // then allow mining of a min-difficulty block.
                    if (pblock.getTimeSeconds() > pindexLast.getHeader().getTimeSeconds() + nTargetSpacing2*2) {
                        verifyDifficulty(bnProofOfWorkLimit, pblock);
                        return;
                    }
                    else
                    {
                        // Return the last non-special-min-difficulty-rules-block
                        StoredBlock cursor = pindexLast;

                        while((cursor = cursor.getPrev(blockStore))!= null && cursor.getHeight() % nInterval != 0 && !cursor.getHeader().getDifficultyTargetAsInteger().equals(bnProofOfWorkLimit))
                        {
                            cursor = cursor.getPrev(blockStore);
                            if(cursor == null)
                                return;
                        }
                        verifyDifficulty(pindexLast.getHeader().getDifficultyTargetAsInteger(), pblock);
                        return;
                    }
                }

                verifyDifficulty(pindexLast.getHeader().getDifficultyTargetAsInteger(), pblock);
                return;
            }

            // GoldCoin (GLD): This fixes an issue where a 51% attack can change difficulty at will.
            // Go back the full period unless it's the first retarget after genesis. Code courtesy of Art Forz
            long blockstogoback = nInterval-1;
            if ((pindexLast.getHeight()+1) != nInterval)
                blockstogoback = nInterval;

            StoredBlock pindexFirst = pindexLast;
            for (int i = 0; pindexFirst != null && i < blockstogoback; i++) {
                //pindexFirst = pindexFirst -> pprev;
                pindexFirst = pindexFirst.getPrev(blockStore);

                if(pindexFirst == null)
                    return;
            }
            //assert(pindexFirst);

            // Limit adjustment step
            long nActualTimespan = pindexLast.getHeader().getTimeSeconds() - pindexFirst.getHeader().getTimeSeconds();
            log.info("  nActualTimespan = {}  before bounds", nActualTimespan);
            long nActualTimespanMax = fNewDifficultyProtocol? ((nTargetTimespan2Current*99)/70) : (nTargetTimespan2Current*4);
            long nActualTimespanMin = fNewDifficultyProtocol? ((nTargetTimespan2Current*70)/99) : (nTargetTimespan2Current/4);
            if (nActualTimespan < nActualTimespanMin)
                nActualTimespan = nActualTimespanMin;
            if (nActualTimespan > nActualTimespanMax)
                nActualTimespan = nActualTimespanMax;
            // Retarget
            bnNew = pindexLast.getHeader().getDifficultyTargetAsInteger();
            //bnNew *= nActualTimespan;
            bnNew = bnNew.multiply(BigInteger.valueOf(nActualTimespan));
            //bnNew /= nTargetTimespan2Current;
            bnNew = bnNew.divide(BigInteger.valueOf(nTargetTimespan2Current));


            //if (bnNew.compareTo(bnProofOfWorkLimit) < 0)
            //    bnNew = bnProofOfWorkLimit;

            /// debug print
            //log.info("GetNextWorkRequired RETARGET\n");
            //log.info("nTargetTimespan2 = %d    nActualTimespan = %\n", nTargetTimespan2Current, nActualTimespan);
            //log.info("Before: %08x  %s\n", pindexLast->nBits, CBigNum().SetCompact(pindexLast->nBits).getuint256().ToString().c_str());
            //log.info("After:  %08x  %s\n", bnNew.GetCompact(), bnNew.getuint256().ToString().c_str());
        } else if(nHeight > novemberFork) {
            hardForkedNovember = true;

            long nTargetTimespanCurrent = fNewDifficultyProtocol? nTargetTimespan : (nTargetTimespan*4);
            long nInterval = nTargetTimespanCurrent / nTargetSpacing;

            // Only change once per interval, or at protocol switch height
            // After julyFork2 we change difficulty at every block.. so we want this only to happen before that..
            if ((nHeight % nInterval != 0) &&
                    (nHeight != nDifficultySwitchHeight || fTestNet) && (nHeight <= julyFork2))
            {
                // Special difficulty rule for testnet:
                if (fTestNet)
                {
                    // If the new block's timestamp is more than 2* 10 minutes
                    // then allow mining of a min-difficulty block.
                    if (pblock.getTimeSeconds() > pindexLast.getHeader().getTimeSeconds() + nTargetSpacing*2) {
                        verifyDifficulty(bnProofOfWorkLimit, pblock);
                        return;
                    }
                    else
                    {
                        // Return the last non-special-min-difficulty-rules-block
                        StoredBlock cursor = pindexLast;

                        while((cursor = cursor.getPrev(blockStore))!= null && cursor.getHeight() % nInterval != 0 && !cursor.getHeader().getDifficultyTargetAsInteger().equals(bnProofOfWorkLimit))
                        {
                            cursor = cursor.getPrev(blockStore);
                            if(cursor == null)
                                return;
                        }
                        verifyDifficulty(pindexLast.getHeader().getDifficultyTargetAsInteger(), pblock);
                        return;
                    }
                }

                verifyDifficulty(pindexLast.getHeader().getDifficultyTargetAsInteger(), pblock);
                return;
            }

            // GoldCoin (GLD): This fixes an issue where a 51% attack can change difficulty at will.
            // Go back the full period unless it's the first retarget after genesis. Code courtesy of Art Forz
            long blockstogoback = nInterval-1;
            if ((pindexLast.getHeight()+1) != nInterval)
                blockstogoback = nInterval;

            StoredBlock pindexFirst = pindexLast;
            for (int i = 0; pindexFirst != null && i < blockstogoback; i++) {
                //pindexFirst = pindexFirst -> pprev;
                pindexFirst = pindexFirst.getPrev(blockStore);

                if(pindexFirst == null)
                    return;
            }

            StoredBlock tblock1 = pindexLast;//We want to copy pindexLast to avoid changing it accidentally
            StoredBlock tblock2 = tblock1;

            //std::vector<int64> last60BlockTimes;
            ArrayList<Long> last60BlockTimes = new ArrayList<Long>(60);
            // Limit adjustment step
            //We need to set this in a way that reflects how fast blocks are actually being solved..
            //First we find the last 60 blocks and take the time between blocks
            //That gives us a list of 59 time differences
            //Then we take the median of those times and multiply it by 60 to get our actualtimespan

            while(last60BlockTimes.size() < 60) {
                last60BlockTimes.add(tblock2.getHeader().getTimeSeconds());
                //if(tblock2->pprev)//should always be so
                //    tblock2 = tblock2->pprev;
                tblock2 = tblock2.getPrev(blockStore);
                if(tblock2 == null)
                    return;
            }
            //std::vector<int64> last59TimeDifferences;
            ArrayList<Long> last59TimeDifferences = new ArrayList<Long>(59);

            int xy = 0;
            while(last59TimeDifferences.size() != 59) {
                if(xy == 59) {
                    //printf(" GetNextWorkRequired(): This shouldn't have happened \n");
                    break;
                }
                last59TimeDifferences.add(java.lang.Math.abs(last60BlockTimes.get(xy) - last60BlockTimes.get(xy+1)));
                xy++;
            }
            Collections.sort(last59TimeDifferences);
            //sort(last59TimeDifferences.begin(), last59TimeDifferences.end(), comp64);

            //log.info("  Median Time between blocks is: {} ",last59TimeDifferences.get(29));
            long nActualTimespan = java.lang.Math.abs((last59TimeDifferences.get(29)));
            long medTime = nActualTimespan;

            if(nHeight > mayFork) {


                //Difficulty Fix here for case where average time between blocks becomes far longer than 2 minutes, even though median time is close to 2 minutes.
                //Uses the last 120 blocks(Should be 4 hours) for calculating

                //log.info(" GetNextWorkRequired(): May Fork mode \n");

                tblock1 = pindexLast;//We want to copy pindexLast to avoid changing it accidentally
                tblock2 = tblock1;

                ArrayList<Long> last120BlockTimes = new ArrayList<Long>();
                // Limit adjustment step
                //We need to set this in a way that reflects how fast blocks are actually being solved..
                //First we find the last 120 blocks and take the time between blocks
                //That gives us a list of 119 time differences
                //Then we take the average of those times and multiply it by 60 to get our actualtimespan
                while(last120BlockTimes.size() < 120) {
                    last120BlockTimes.add(tblock2.getHeader().getTimeSeconds());
                    tblock2 = tblock2.getPrev(blockStore);
                    if(tblock2 == null)
                        return;
                }
                ArrayList<Long> last119TimeDifferences = new ArrayList<Long>();

                xy = 0;
                while(last119TimeDifferences.size() != 119) {
                    if(xy == 119) {
                        //       printf(" GetNextWorkRequired(): This shouldn't have happened 2 \n");
                        break;
                    }
                    last119TimeDifferences.add(java.lang.Math.abs(last120BlockTimes.get(xy) - last120BlockTimes.get(xy + 1)));
                    xy++;
                }
                long total = 0;

                for(int x = 0; x < 119; x++) {
                    long timeN = last119TimeDifferences.get(x);
                    //printf(" GetNextWorkRequired(): Current Time difference is: %"PRI64d" \n",timeN);
                    total += timeN;
                }

                averageTime = total/119;


                //log.info(" GetNextWorkRequired(): Average time between blocks over the last 120 blocks is: "+ averageTime);
            /*printf(" GetNextWorkRequired(): Total Time (over 119 time differences) is: %"PRI64d" \n",total);
            printf(" GetNextWorkRequired(): First Time (over 119 time differences) is: %"PRI64d" \n",last119TimeDifferences[0]);
            printf(" GetNextWorkRequired(): Last Time (over 119 time differences) is: %"PRI64d" \n",last119TimeDifferences[118]);
            printf(" GetNextWorkRequired(): Last Time is: %"PRI64d" \n",last120BlockTimes[119]);
            printf(" GetNextWorkRequired(): 2nd Last Time is: %"PRI64d" \n",last120BlockTimes[118]);

            printf(" GetNextWorkRequired(): First Time is: %"PRI64d" \n",last120BlockTimes[0]);
            printf(" GetNextWorkRequired(): 2nd Time is: %"PRI64d" \n",last120BlockTimes[1]);*/

                if(nHeight <= julyFork2) {
                    //If the average time between blocks exceeds or is equal to 3 minutes then increase the med time accordingly
                    if(averageTime >= 180) {
                        //log.info(" \n Average Time between blocks is too high.. Attempting to Adjust.. \n ");
                        medTime = 130;
                    } else if(averageTime >= 108 && medTime < 120) {
                        //If the average time between blocks is more than 1.8 minutes and medTime is less than 120 seconds (which would ordinarily prompt an increase in difficulty)
                        //limit the stepping to something reasonable(so we don't see massive difficulty spike followed by miners leaving in these situations).
                        medTime = 110;
                        //log.info(" \n Medium Time between blocks is too low compared to average time.. Attempting to Adjust.. \n ");
                    }
                } else {//julyFork2 changes here

                    //Calculate difficulty of previous block as a double
                /*int nShift = (pindexLast->nBits >> 24) & 0xff;

				double dDiff =
					(double)0x0000ffff / (double)(pindexLast->nBits & 0x00ffffff);

				while (nShift < 29)
				{
					dDiff *= 256.0;
					nShift++;
				}
				while (nShift > 29)
				{
					dDiff /= 256.0;
					nShift--;
                } */

                    //int64 hashrate = (int64)(dDiff * pow(2.0,32.0))/((medTime > averageTime)?averageTime:medTime);

                    medTime = (medTime > averageTime)?averageTime:medTime;

                    if(averageTime >= 180 && last119TimeDifferences.get(0) >= 1200 && last119TimeDifferences.get(1) >= 1200) {
                        didHalfAdjust = true;
                        medTime = 240;
                    }

                }
            }

            //Fixes an issue where median time between blocks is greater than 120 seconds and is not permitted to be lower by the defence system
            //Causing difficulty to drop without end

            if(nHeight > novemberFork2) {
                if(medTime >= 120) {
                    //Check to see whether we are in a deadlock situation with the 51% defense system
                    //printf(" \n Checking for DeadLocks \n");
                    int numTooClose = 0;
                    int index = 1;
                    while(index != 55) {
                        if(java.lang.Math.abs(last60BlockTimes.get(last60BlockTimes.size()-index) - last60BlockTimes.get(last60BlockTimes.size() - (index + 5))) == 600) {
                            numTooClose++;
                        }
                        index++;
                    }

                    if(numTooClose > 0) {
                        //We found 6 blocks that were solved in exactly 10 minutes
                        //Averaging 1.66 minutes per block
                        //printf(" \n DeadLock detected and fixed - Difficulty Increased to avoid bleeding edge of defence system \n");

                        if(nHeight > julyFork2) {
                            medTime = 119;
                        } else {
                            medTime = 110;
                        }
                    } else {
                        //printf(" \n DeadLock not detected. \n");
                    }


                }
            }


            if(nHeight > julyFork2) {
                //216 == (int64) 180.0/100.0 * 120
                //122 == (int64) 102.0/100.0 * 120 == 122.4
                if(averageTime > 216 || medTime > 122) {
                    if(didHalfAdjust) {
                        // If the average time between blocks was
                        // too high.. allow a dramatic difficulty
                        // fall..
                        medTime = (long)(120 * 142.0/100.0);
                    } else {
                        // Otherwise only allow a 120/119 fall per block
                        // maximum.. As we now adjust per block..
                        // 121 == (int64) 120 * 120.0/119.0
                        medTime = 121;
                    }
                }
                // 117 -- (int64) 120.0 * 98.0/100.0
                else if(averageTime < 117 || medTime < 117)  {
                    // If the average time between blocks is within 2% of target
                    // value
                    // Or if the median time stamp between blocks is within 2% of
                    // the target value
                    // Limit diff increase to 2%
                    medTime = 117;
                }
                nActualTimespan = medTime * 60;
            } else {

                nActualTimespan = medTime * 60;

                //printf("  nActualTimespan = %"PRI64d"  before bounds\n", nActualTimespan);
                long nActualTimespanMax = fNewDifficultyProtocol? ((nTargetTimespanCurrent*99)/70) : (nTargetTimespanCurrent*4);
                long nActualTimespanMin = fNewDifficultyProtocol? ((nTargetTimespanCurrent*70)/99) : (nTargetTimespanCurrent/4);
                if (nActualTimespan < nActualTimespanMin)
                    nActualTimespan = nActualTimespanMin;
                if (nActualTimespan > nActualTimespanMax)
                    nActualTimespan = nActualTimespanMax;

            }


            if(nHeight > julyFork2) {
                StoredBlock tblock11 = pindexLast;//We want to copy pindexLast to avoid changing it accidentally
                StoredBlock tblock22 = tblock11;

                // We want to limit the possible difficulty raise/fall over 60 and 240 blocks here
                // So we get the difficulty at 60 and 240 blocks ago

                long nbits60ago = 0;
                long nbits240ago = 0;
                int counter = 0;
                //Note: 0 is the current block, we want 60 past current

                while(counter <= 240) {
                    if(counter == 60) {
                        nbits60ago = tblock22.getHeader().getDifficultyTarget();
                    } else if(counter == 240) {
                        nbits240ago = tblock22.getHeader().getDifficultyTarget();
                    }
                    tblock22 = tblock22.getPrev(blockStore);

                    if(tblock22 == null)
                        return; //break out since not 240 block in the chain
                    counter++;
                }

                //Now we get the old targets
                BigInteger bn60ago;
                BigInteger bn240ago;
                BigInteger bnLast;

                bn60ago = Utils.decodeCompactBits(nbits60ago);
                bn240ago = Utils.decodeCompactBits(nbits240ago);
                bnLast = pindexLast.getHeader().getDifficultyTargetAsInteger();

                //Set the new target
                bnNew = pindexLast.getHeader().getDifficultyTargetAsInteger();
                //bnNew *= nActualTimespan;
                bnNew = bnNew.multiply(BigInteger.valueOf(nActualTimespan));
                //bnNew /= nTargetTimespanCurrent;
                bnNew = bnNew.divide(BigInteger.valueOf(nTargetTimespanCurrent));


                //Now we have the difficulty at those blocks..

                // Set a floor on difficulty decreases per block(20% lower maximum
                // than the previous block difficulty).. when there was no halfing
                // necessary.. 10/8 == 1.0/0.8
                //bnLast *= 10;
                bnLast = bnLast.multiply(BigInteger.valueOf(10));
                //bnLast /= 8;
                bnLast = bnLast.divide(BigInteger.valueOf(8));

                if(!didHalfAdjust && bnNew.compareTo(bnLast) > 0) {
                    bnNew = bnLast;
                    //log.info("New target > Last (10/8)[{}]", pindexLast.getHeight());
                }

                //bnLast *= 8;
                bnLast = bnLast.multiply(BigInteger.valueOf(8));
                //bnLast /= 10;
                bnLast = bnLast.divide(BigInteger.valueOf(10));

                // Set ceilings on difficulty increases per block

                //1.0/1.02 == 100/102
                //bn60ago *= 100;
                bn60ago = bn60ago.multiply(BigInteger.valueOf(100));
                //bn60ago /= 102;
                bn60ago = bn60ago.divide(BigInteger.valueOf(102));

                if(bnNew.compareTo(bn60ago) < 0) {
                    bnNew = bn60ago;
                    //log.info("New target < 60-blocks-ago*1.02[{}]", pindexLast.getHeight());
                }

//                bn60ago *= 102;
                //              bn60ago /= 100;
                bn60ago = bn60ago.multiply(BigInteger.valueOf(102));
                bn60ago = bn60ago.divide(BigInteger.valueOf(100));

                //1.0/(1.02*4) ==  100 / 408

                //bn240ago *= 100;
                //bn240ago /= 408;
                bn240ago = bn240ago.multiply(BigInteger.valueOf(100));
                bn240ago = bn240ago.divide(BigInteger.valueOf(408));

                if(bnNew.compareTo(bn240ago) < 0) {
                    bnNew = bn240ago;
                   // log.info("New target < 240-blocks-ago*1.02 [{}]", pindexLast.getHeight());
                }

                //bn240ago *= 408;
                //bn240ago /= 100;
                bn240ago = bn240ago.multiply(BigInteger.valueOf(408));
                bn240ago = bn240ago.divide(BigInteger.valueOf(100));


            } else {
                // Retarget
                bnNew = pindexLast.getHeader().getDifficultyTargetAsInteger();
                //bnNew *= nActualTimespan;
                //bnNew /= nTargetTimespanCurrent;
                bnNew = bnNew.multiply(BigInteger.valueOf(nActualTimespan));
                bnNew = bnNew.divide(BigInteger.valueOf(nTargetTimespanCurrent));
            }

            //Sets a ceiling on highest target value (lowest possible difficulty)
            if (bnNew.compareTo(bnProofOfWorkLimit) > 0)
                bnNew = bnProofOfWorkLimit;

            /// debug print
            //printf("GetNextWorkRequired RETARGET\n");
            //printf("nTargetTimespan = %"PRI64d"    nActualTimespan = %"PRI64d"\n", nTargetTimespanCurrent, nActualTimespan);
            //printf("Before: %08x  %s\n", pindexLast->nBits, CBigNum().SetCompact(pindexLast->nBits).getuint256().ToString().c_str());
            //printf("After:  %08x  %s\n", bnNew.GetCompact(), bnNew.getuint256().ToString().c_str());
        } else {
            hardForkedJuly = true;
            long nTargetTimespanCurrent = fNewDifficultyProtocol? nTargetTimespan : (nTargetTimespan*4);
            long nInterval = nTargetTimespanCurrent / nTargetSpacing;

            // Only change once per interval, or at protocol switch height
            if ((nHeight % nInterval != 0) &&
                    (nHeight != nDifficultySwitchHeight || fTestNet))
            {
                // Special difficulty rule for testnet:
                if (fTestNet)
                {
                    // If the new block's timestamp is more than 2* 10 minutes
                    // then allow mining of a min-difficulty block.
                    if (pblock.getTimeSeconds() > pindexLast.getHeader().getTimeSeconds() + nTargetSpacing*2) {
                        verifyDifficulty(bnProofOfWorkLimit, pblock);
                        return;
                    }
                    else
                    {
                        // Return the last non-special-min-difficulty-rules-block
                        // Return the last non-special-min-difficulty-rules-block
                        StoredBlock cursor = pindexLast;

                        while((cursor = cursor.getPrev(blockStore))!= null && cursor.getHeight() % nInterval != 0 && !cursor.getHeader().getDifficultyTargetAsInteger().equals(bnProofOfWorkLimit))
                        {
                            cursor = cursor.getPrev(blockStore);
                            if(cursor == null)
                                return;
                        }
                        verifyDifficulty(pindexLast.getHeader().getDifficultyTargetAsInteger(), pblock);
                        return;
                    }
                }

                verifyDifficulty(pindexLast.getHeader().getDifficultyTargetAsInteger(), pblock);
                return;
            }

            // GoldCoin (GLD): This fixes an issue where a 51% attack can change difficulty at will.
            // Go back the full period unless it's the first retarget after genesis. Code courtesy of Art Forz
            long blockstogoback = nInterval-1;
            if ((pindexLast.getHeight()+1) != nInterval)
                blockstogoback = nInterval;
            StoredBlock pindexFirst = pindexLast;
            for (int i = 0; pindexFirst != null && i < blockstogoback; i++) {
                pindexFirst = pindexFirst.getPrev(blockStore);
                if(pindexFirst == null)
                    return;
            }
            //assert(pindexFirst);

            // Limit adjustment step
            long nActualTimespan = pindexLast.getHeader().getTimeSeconds() - pindexFirst.getHeader().getTimeSeconds();
            //printf("  nActualTimespan = %"PRI64d"  before bounds\n", nActualTimespan);
            long nActualTimespanMax = fNewDifficultyProtocol? ((nTargetTimespanCurrent*99)/70) : (nTargetTimespanCurrent*4);
            long nActualTimespanMin = fNewDifficultyProtocol? ((nTargetTimespanCurrent*70)/99) : (nTargetTimespanCurrent/4);
            if (nActualTimespan < nActualTimespanMin)
                nActualTimespan = nActualTimespanMin;
            if (nActualTimespan > nActualTimespanMax)
                nActualTimespan = nActualTimespanMax;
            // Retarget
            bnNew = pindexLast.getHeader().getDifficultyTargetAsInteger();
            //bnNew *= nActualTimespan;
            //bnNew /= nTargetTimespanCurrent;
            bnNew = bnNew.multiply(BigInteger.valueOf(nActualTimespan));
            bnNew = bnNew.divide(BigInteger.valueOf(nTargetTimespanCurrent));

            //if (bnNew > bnProofOfWorkLimit)
            //  bnNew = bnProofOfWorkLimit;

            /// debug print
            //  printf("GetNextWorkRequired RETARGET\n");
            // printf("nTargetTimespan = %"PRI64d"    nActualTimespan = %"PRI64d"\n", nTargetTimespanCurrent, nActualTimespan);
            // printf("Before: %08x  %s\n", pindexLast->nBits, CBigNum().SetCompact(pindexLast->nBits).getuint256().ToString().c_str());
            //printf("After:  %08x  %s\n", bnNew.GetCompact(), bnNew.getuint256().ToString().c_str());
        }
        //return bnNew.GetCompact();
        verifyDifficulty(bnNew, pblock);
        return;
    }
    void verifyDifficulty(BigInteger newDifficulty, Block nextBlock) throws VerificationException
    {
        if (newDifficulty.compareTo(maxTarget) > 0) {
            log.info("Difficulty hit proof of work limit: {}", newDifficulty.toString(16));
            newDifficulty = maxTarget;
        }

        int accuracyBytes = (int) (nextBlock.getDifficultyTarget() >>> 24) - 3;
        BigInteger receivedDifficulty = nextBlock.getDifficultyTargetAsInteger();

        // The calculated difficulty is to a higher precision than received, so reduce here.
        BigInteger mask = BigInteger.valueOf(0xFFFFFFL).shiftLeft(accuracyBytes * 8);
        newDifficulty = newDifficulty.and(mask);

        if (newDifficulty.compareTo(receivedDifficulty) != 0)
            throw new VerificationException("Network provided difficulty bits do not match what was calculated: received" +
                    receivedDifficulty.toString(16) + " vs calculated " + newDifficulty.toString(16));
    }

    @Override
    public Coin getMaxMoney() {
        return MAX_MONEY;
    }

    @Override
    public Coin getMinNonDustOutput() {
        return Transaction.MIN_NONDUST_OUTPUT;
    }

    @Override
    public MonetaryFormat getMonetaryFormat() {
        return new MonetaryFormat();
    }

    @Override
    public int getProtocolVersionNum(final ProtocolVersion version) {
        return version.getBitcoinProtocolVersion();
    }

    @Override
    public BitcoinSerializer getSerializer(boolean parseRetain) {
        return new BitcoinSerializer(this, parseRetain);
    }

    @Override
    public String getUriScheme() {
        return BITCOIN_SCHEME;
    }

    @Override
    public boolean hasMaxMoney() {
        return true;
    }
}
