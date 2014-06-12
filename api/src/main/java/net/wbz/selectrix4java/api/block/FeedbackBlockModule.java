package net.wbz.selectrix4java.api.block;

import com.google.common.collect.Maps;
import net.wbz.selectrix4java.api.bus.BusAddress;
import net.wbz.selectrix4java.api.bus.BusAddressListener;
import net.wbz.selectrix4java.api.train.TrainModule;

import java.util.Map;

/**
 * @author Daniel Tuerk (daniel.tuerk@w-b-z.com)
 */
public class FeedbackBlockModule extends BlockModule {

    private final Map<Integer, Integer> blockSpeedMapping = Maps.newConcurrentMap();

    public FeedbackBlockModule(final Map<BusAddress, TrainModule> trainModules, BusAddress address,
                               BusAddress feedbackAddress, BusAddress additionalAddresses) {
        super(address, additionalAddresses);
        feedbackAddress.addListener(new BusAddressListener() {
            @Override
            public void dataChanged(byte oldValue, byte newValue) {

                //TODO
                byte trainAddress = 0;
                int blockNumber = 0;

                if (blockSpeedMapping.containsKey(blockNumber)) {
                    TrainModule trainModule = trainModules.get(trainAddress);
                    trainModule.setDrivingLevel(blockSpeedMapping.get(blockNumber));
                }

            }
        });
    }

    public FeedbackBlockModule setDriving(int blockNumber, int targetSpeed) {
        return apply(blockNumber, targetSpeed);
    }

    public FeedbackBlockModule setStop(int blockNumber) {
        return apply(blockNumber, 0);
    }

    public FeedbackBlockModule setBraking(int blockNumber, int targetSpeed) {
        return apply(blockNumber, targetSpeed);
    }

    private FeedbackBlockModule apply(int blockNumber, int targetSpeed) {
        blockSpeedMapping.put(blockNumber, targetSpeed);
        getAddress().send();
        return this;
    }


}
