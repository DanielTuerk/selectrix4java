package net.wbz.selectrix4java.device.serial;

import org.junit.Ignore;
import org.junit.Test;

import net.wbz.selectrix4java.block.FeedbackBlockListener;
import net.wbz.selectrix4java.block.FeedbackBlockModule;

/**
 * Test the feedback block modules for received occupy states and train decoder information.
 * <p/>
 * Testcase:
 * <ol>
 * <li>Train 7</li>
 * <li>Train 13</li>
 * <li>block 58</li>
 * </ol>
 * <p>Scenario:</p>
 * <ol>
 * <li>start with active rail voltage</li>
 * <li>train 7 drive to block 58/4</li>
 * <li>train 13 drive to block 58/4</li>
 * <li>train 7 drive from block58/4 to 58/3</li>
 * <li>train 13 drive from block58/4 to 58/3</li>
 * <li>train 13 drive to block 58/3 to 58/4 to offtrack</li>
 * <li>train 7 drive to block 58/3 to 58/4 to offtrack</li>
 * <li>train 7 drive from offtrack to block 58/4 to 58/3 to 58/2 to offtrack</li>
 * </ol>
 *
 * @author Daniel Tuerk
 */
public class FeedbackBlockMultiTrainTest extends BaseFeedbackDataTest {

    public FeedbackBlockMultiTrainTest() {
        super("records/feedback_module_58-train_7_13", DEFAULT_PLAYBACK_SPEED);
    }

    @Override
    protected void initTest() {
        super.initTest();

        appendToQueue(new FeedbackData(1, false, 0, false));
        appendToQueue(new FeedbackData(4, false, 7, false));
        appendToQueue(new FeedbackData(4, true, 7, false));
    }

    /**
     * TODO renew record data, seems corrupted
     * 
     * @throws Exception
     */
    @Ignore
    public void testBlock() throws Exception {

        final int blockAddress = 58;

        // final LinkedBlockingDeque<Integer> train7EnterBlocks = new LinkedBlockingDeque<>(
        // Lists.newArrayList(4, 3, 4, 4, 3, 2, 3, 4));
        // final LinkedBlockingDeque<Integer> train13EnterBlocks = new LinkedBlockingDeque<>(
        // Lists.newArrayList(4, 3, 4));

        FeedbackBlockModule feedbackBlockModule = getDevice().getFeedbackBlockModule((byte) blockAddress,
                (byte) (blockAddress + 2), (byte) (blockAddress + 1));
        feedbackBlockModule.addFeedbackBlockListener(new FeedbackBlockListener() {
            @Override
            public void trainEnterBlock(int blockNumber, int train, boolean forward) {
                print("train %d enter -> Block: %d (direction: %b)", train, blockNumber, forward);
                check(true, blockNumber, train, forward);
            }

            @Override
            public void trainLeaveBlock(int blockNumber, int train, boolean forward) {
                print("train %d exit <- Block: %d (direction: %b)", train, blockNumber, forward);
                check(false, blockNumber, train, forward);
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