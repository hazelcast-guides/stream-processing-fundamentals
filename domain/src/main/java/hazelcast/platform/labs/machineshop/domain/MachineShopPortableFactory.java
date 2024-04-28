package hazelcast.platform.labs.machineshop.domain;

import com.hazelcast.nio.serialization.Portable;
import com.hazelcast.nio.serialization.PortableFactory;

public class MachineShopPortableFactory implements PortableFactory {
    @Override
    public Portable create(int classId) {
        if (classId == PortableHelper.MACHINE_PROFILE_ID)
            return new MachineProfile();
        else if (classId == PortableHelper.MACHINE_STATUS_ID)
            return new MachineStatus();
        else
            return null;
    }
}
