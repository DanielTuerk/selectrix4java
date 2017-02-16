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

            Assert.assertEquals(data.blockNumber, blockNumber);
            Assert.assertEquals(data.enter, enter);
            Assert.assertEquals(data.train, train);
            Assert.assertEquals(data.forward, forward);
        } catch (AssertionError e) {
            add(e);
        }
    }
}
