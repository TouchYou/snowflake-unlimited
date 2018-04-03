package cn.Jolyne;

/**
 * Represents a unique id generator.
 *
 */
public interface UidGenerator {

    /**
     * Get a unique ID
     *
     * @return UID
     * @throws UidGenerateException
     */
    String getUID(String preFix) throws UidGenerateException;

    /**
     * Parse the UID into elements which are used to generate the UID. <br>
     * Such as timestamp & workerId & sequence...
     *
     * @param uid
     * @return Parsed info
     */
    String parseUID(String uid);

}
