package hazelcast.platform.solutions.machineshop.domain;

import com.hazelcast.nio.serialization.compact.CompactReader;
import com.hazelcast.nio.serialization.compact.CompactSerializer;
import com.hazelcast.nio.serialization.compact.CompactWriter;

import java.util.Random;


public class MachineProfile {
    private String serialNum;
    private String location;

    private String block;
    private float faultyOdds;
    private String manufacturer;
    private short warningTemp;
    private short criticalTemp;
    private int maxRPM;

    public String getSerialNum() {
        return serialNum;
    }

    public void setSerialNum(String serialNum) {
        this.serialNum = serialNum;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public short getWarningTemp() {
        return warningTemp;
    }

    public void setWarningTemp(short warningTemp) {
        this.warningTemp = warningTemp;
    }

    public short getCriticalTemp() {
        return criticalTemp;
    }

    public void setCriticalTemp(short criticalTemp) {
        this.criticalTemp = criticalTemp;
    }

    public int getMaxRPM() {
        return maxRPM;
    }

    public void setMaxRPM(int maxRPM) {
        this.maxRPM = maxRPM;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getBlock() {
        return block;
    }

    public void setBlock(String block) {
        this.block = block;
    }

    public float getFaultyOdds() {
        return faultyOdds;
    }

    public void setFaultyOdds(float faultyOdds) {
        this.faultyOdds = faultyOdds;
    }


    /////// for generating fake data

    private static final Random random = new Random();
    private static final String[] companies = new String [] {"Breton","Fabplus", "Laguna Tools", "Snapmaker","Machinecraft", "Multicam", "OZ Machine"};

    private static final String letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String digits = "0123456789";

    private static final int [] rpmLimits = new int[] {8000,10000,12000,20000,30000,40000};

    private static final short[] warningTemps = new short[] {100,150,210, 300};

    private static String randomSN(){
        char [] result = new char[6];
        for(int i=0; i < 3; ++i) result[i] = letters.charAt(random.nextInt(letters.length()));
        for(int j=3; j < result.length; ++j) result[j] = digits.charAt(random.nextInt(digits.length()));
        return new String(result);
    }

    private static String randomCompany(){
        return companies[random.nextInt(companies.length)];
    }
    private static short randomWarningTemp(){
        return warningTemps[random.nextInt(warningTemps.length)];
    }

    private static int randomMaxRPM(){
        return rpmLimits[random.nextInt(rpmLimits.length)];
    }

    // not thread safe
    public static MachineProfile fake(String location, String block, float pFaulty){
        MachineProfile result = new MachineProfile();

        result.setManufacturer(randomCompany());
        result.setSerialNum(randomSN());
        result.setLocation(location);
        result.setBlock(block);
        result.setFaultyOdds(pFaulty);
        result.setWarningTemp(randomWarningTemp());
        result.setCriticalTemp((short) (result.warningTemp + 30));
        result.setMaxRPM(randomMaxRPM());

        return result;
    }

    /*
     * Although not required, explicit compact serializers provide the best performance.
     */
    public static class Serializer implements CompactSerializer<MachineProfile> {

        @Override
        public MachineProfile read(CompactReader reader) {
            MachineProfile result = new MachineProfile();
            result.setSerialNum(reader.readString("serialNum"));
            result.setLocation(reader.readString("location"));
            result.setBlock(reader.readString("block"));
            result.setFaultyOdds(reader.readFloat32("faultyOdds"));
            result.setManufacturer(reader.readString("manufacturer"));
            result.setWarningTemp(reader.readInt16("warningTemp"));
            result.setCriticalTemp(reader.readInt16("criticalTemp"));
            result.setMaxRPM(reader.readInt32("maxRPM"));
            return result;
        }

        @Override
        public void write(CompactWriter writer, MachineProfile object) {
            writer.writeString("serialNum", object.getSerialNum());
            writer.writeString("location", object.getLocation());
            writer.writeString("block", object.getBlock());
            writer.writeFloat32("faultyOdds", object.getFaultyOdds());
            writer.writeString("manufacturer", object.getManufacturer());
            writer.writeInt16("warningTemp", object.getWarningTemp());
            writer.writeInt16("criticalTemp", object.getCriticalTemp());
            writer.writeInt32("maxRPM", object.getMaxRPM());
        }

        @Override
        public String getTypeName() {
            return "machine_profile";
        }

        @Override
        public Class<MachineProfile> getCompactClass() {
            return MachineProfile.class;
        }
    }
}
