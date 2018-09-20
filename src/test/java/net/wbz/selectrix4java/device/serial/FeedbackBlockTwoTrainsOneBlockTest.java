package net.wbz.selectrix4java.device.serial;

import net.wbz.selectrix4java.block.FeedbackBlockListener;
import net.wbz.selectrix4java.block.FeedbackBlockModule;
import org.junit.Test;

/**
 * 2017-02-10 19:30:01 [bus-data-dispatcher-0] DEBUG n.w.s.block.FeedbackBlockModule - feedback module (53) - block: 3;
 * sequence: 32 (last: 0); train: 13; enter: true; forward: false 2017-02-10 19:30:03 [bus-data-dispatcher-0] DEBUG
 * n.w.s.block.FeedbackBlockModule - feedback module (53) - block: 5; sequence: 64 (last: 32); train: 13; enter: false;
 * forward: false 2017-02-10 19:30:16 [bus-data-dispatcher-1] DEBUG n.w.s.block.FeedbackBlockModule - feedback module
 * (53) - block: 4; sequence: 96 (last: 64); train: 13; enter: true; forward: true 2017-02-10 19:30:32
 * [bus-data-dispatcher-1] DEBUG n.w.s.block.FeedbackBlockModule - feedback module (53) - block: 6; sequence: 0 (last:
 * 96); train: 7; enter: true; forward: true 2017-02-10 19:30:34 [bus-data-dispatcher-1] DEBUG
 * n.w.s.block.FeedbackBlockModule - feedback module (53) - block: 4; sequence: 32 (last: 0); train: 7; enter: false;
 * forward: true 2017-02-10 19:30:47 [bus-data-dispatcher-0] DEBUG n.w.s.block.FeedbackBlockModule - feedback module
 * (53) - block: 2; sequence: 64 (last: 32); train: 13; enter: true; forward: false 2017-02-10 19:30:48
 * [bus-data-dispatcher-0] DEBUG n.w.s.block.FeedbackBlockModule - feedback module (53) - block: 3; sequence: 96 (last:
 * 64); train: 13; enter: false; forward: false Test the feedback block modules for received occupy states and train
 * decoder information.
 * Testcase:
 * <ol>
 * <li>Train 7</li>
 * <li>Train 13</li>
 * <li>block 53</li>
 * </ol>
 * <p>
 * Scenario:
 * </p>
 * <ol>
 * <li>start with active rail voltage</li>
 * <li>train 13 enter 53/3</li>
 * <li>train 13 leave 53/5</li>
 * <li>train 7 enter 53/4</li>
 * <li>train 7 enter 53/6</li>
 * <li>train 7 leave 53/4</li>
 * <li>train 13 enter 53/2</li>
 * <li>train 13 leave 53/3</li>
 * </ol>
 *
 * @author Daniel Tuerk
 */
public class FeedbackBlockTwoTrainsOneBlockTest extends BaseFeedbackDataTest {

    public FeedbackBlockTwoTrainsOneBlockTest() {
        super("records/record_2-trains_1_on_Block-53", DEFAULT_PLAYBACK_SPEED);
    }

    @Override
    protected void initTest() {
        // expected data
        appendToQueue(new FeedbackData(3, true, 13, false));
        appendToQueue(new FeedbackData(5, false, 13, false));
        appendToQueue(new FeedbackData(4, true, 7, true));
        appendToQueue(new FeedbackData(6, true, 7, true));
        appendToQueue(new FeedbackData(4, false, 7, true));
        appendToQueue(new FeedbackData(2, true, 13, false));
        appendToQueue(new FeedbackData(3, false, 13, false));
    }

    @Test
    public void testBlock() throws Exception {
        final int blockAddress = 53;
        FeedbackBlockModule feedbackBlockModule = getDevice()
                .getFeedbackBlockModule((byte) blockAddress, (byte) (blockAddress + 2), (byte) (blockAddress + 1));
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
            }

            @Override
            public void blockFreed(int blockNr) {
            }

        });

        startPlayback();

        waitToFinishRecord();

    }

}
