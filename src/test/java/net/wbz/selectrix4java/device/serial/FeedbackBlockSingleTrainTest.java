package net.wbz.selectrix4java.device.serial;

import org.junit.Assert;
import org.junit.Test;

import net.wbz.selectrix4java.block.FeedbackBlockListener;
import net.wbz.selectrix4java.block.FeedbackBlockModule;

/**
 * Test the feedback block modules for received occupy states and train decoder information.
 * <p/>
 * Testcase:
 * <ol>
 * <li>Train 7</li>
 * <li>block 58</li>
 * </ol>
 * <p>Scenario:</p>
 * <ol>
 * <li>start with active rail voltage</li>
 * <li>train 7 drive to block 58/4</li>
 * <li>train 7 drive from block58/4 to 58/3</li>
 * <li>train 7 drive from block58/3 to 58/2</li>
 * <li>train 7 drive from block58/2 to 58/1</li>
 * <li>short pause</li>
 * <li>train 7 drive from block58/1 to 58/2</li>
 * <li>train 7 drive from block58/2 to 58/3</li>
 * <li>train 7 drive from block58/3 to 58/4</li>
 * <li>train 7 drive from 58/4 to offtrack</li>
 * </ol>
 *
 * @author Daniel Tuerk
 */
public class FeedbackBlockSingleTrainTest extends BaseFeedbackTest<Integer> {

    public FeedbackBlockSingleTrainTest() {
        super("records/feedback_module_58-train_7", DEFAULT_PLAYBACK_SPEED);
    }

    @Override
    protected void initTest() {
        // TODO create new testdata, also there are two trains in test data (5+7)
        appendToQueue(4);
        appendToQueue(3);
        appendToQueue(2);
        appendToQueue(1);
        appendToQueue(2);
        appendToQueue(3);
        appendToQueue(4);
    }

    @Test
    public void testBlock() throws Exception {

        final int expectedTrain = 7;

        final int blockAddress = 58;

        FeedbackBlockModule feedbackBlockModule = getDevice().getFeedbackBlockModule((byte) blockAddress,
                (byte) (blockAddress + 2), (byte) (blockAddress + 1));
        feedbackBlockModule.addFeedbackBlockListener(new FeedbackBlockListener() {
            @Override
            public void trainEnterBlock(int blockNumber, int train, boolean forward) {
                print("train %d enter -> Block: %d (direction: %b)", train, blockNumber, forward);
                try {
                    Assert.assertEquals(nextFromQueue().intValue(), blockNumber);
                    Assert.assertEquals(expectedTrain, train);
                } catch (AssertionError e) {
                    add(e);
                }
            }

            @Override
            public void trainLeaveBlock(int blockNumber, int train, boolean forward) {
                print("train %d exit <- Block: %d (direction: %b)", train, blockNumber, forward);
            }

            @Override
            public void blockOccupied(int blockNr) {
                print("FeedbackBlockModule :: blockOccupied - nr: %d", blockNr);
            }

            @Override
            public void blockFreed(int blockNr) {
                print("FeedbackBlockModule :: blockFreed - nr: %d", blockNr);
            }
        });

        startPlayback();

        waitToFinishRecord();

    }

}
