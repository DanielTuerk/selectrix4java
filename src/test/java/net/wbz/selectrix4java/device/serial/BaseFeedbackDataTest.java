package net.wbz.selectrix4java.device.serial;

import org.junit.Assert;

/**
 * @author Daniel Tuerk
 */
public class BaseFeedbackDataTest extends BaseFeedbackTest<FeedbackData> {

    public BaseFeedbackDataTest(String recordFilePath, int playbackSpeed) {
        super(recordFilePath, playbackSpeed);
    }

    protected synchronized void check(boolean enter, int blockNumber, int train, boolean forward) {
        try {
            FeedbackData data = nextFromQueue();

            Assert.assertNotNull("no more data in queue", data);

            Assert.assertEquals(msg("blockNumber", enter, blockNumber, train, forward, data), data.blockNumber, blockNumber);
            Assert.assertEquals(msg("enter", enter, blockNumber, train, forward, data), data.enter, enter);
            Assert.assertEquals(msg("train", enter, blockNumber, train, forward, data), data.train, train);
            Assert.assertEquals(msg("forward", enter, blockNumber, train, forward, data), data.forward, forward);
        } catch (AssertionError e) {
            add(e);
        }
    }

    private static String msg(String msg, boolean enter, int blockNumber, int train, boolean forward, FeedbackData data) {
        return ("%s failed, expected:%%s - actual:%%s".formatted(msg))
                .formatted(data, "(train: %d, forward: %s, block: %d, enter: %s)"
                        .formatted(train, forward, blockNumber, enter));
    }
}
