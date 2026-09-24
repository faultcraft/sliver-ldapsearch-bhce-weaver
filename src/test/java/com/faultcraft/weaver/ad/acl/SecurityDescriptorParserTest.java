package com.faultcraft.weaver.ad.acl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Base64;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityDescriptorParserTest {

    @Test
    void parse_nullInput_returnsEmpty() {
        var result = SecurityDescriptorParser.parse(null);
        assertTrue(result.aces().isEmpty());
        assertFalse(result.isDaclProtected());
    }

    @Test
    void parse_emptyInput_returnsEmpty() {
        var result = SecurityDescriptorParser.parse("");
        assertTrue(result.aces().isEmpty());
    }

    @Test
    void parse_invalidBase64_returnsEmpty() {
        var result = SecurityDescriptorParser.parse("not-valid-base64!!!");
        assertTrue(result.aces().isEmpty());
    }

    @Test
    void parseBytes_tooShort_returnsEmpty() {
        var result = SecurityDescriptorParser.parseBytes(new byte[10]);
        assertTrue(result.aces().isEmpty());
    }

    @Test
    void parseBytes_noDacl_returnsEmpty() {
        byte[] sd = buildSecurityDescriptor(0, 0);
        var result = SecurityDescriptorParser.parseBytes(sd);
        assertTrue(result.aces().isEmpty());
        assertFalse(result.isDaclProtected());
    }

    @Test
    void parseBytes_daclProtected_flagSet() {
        byte[] sd = buildSecurityDescriptor(0x1000, 0);
        var result = SecurityDescriptorParser.parseBytes(sd);
        assertTrue(result.isDaclProtected());
    }

    @Test
    void parseBytes_basicAllowAce_parsed() {
        byte[] ace = buildBasicAce(AceRecord.ACCESS_ALLOWED, 0x00, 0x000F01FF,
                new byte[]{0x01, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x05,
                        0x12, 0x00, 0x00, 0x00});
        byte[] dacl = buildDacl(ace);
        byte[] sd = buildSecurityDescriptorWithDacl(0, dacl);

        var result = SecurityDescriptorParser.parseBytes(sd);

        assertEquals(1, result.aces().size());
        AceRecord parsed = result.aces().get(0);
        assertEquals(AceRecord.ACCESS_ALLOWED, parsed.aceType());
        assertEquals(0x000F01FF, parsed.accessMask());
        assertEquals("S-1-5-18", parsed.principalSid());
        assertTrue(parsed.isAllow());
        assertFalse(parsed.isInherited());
    }

    @Test
    void parseBytes_inheritedAce_flagSet() {
        byte[] ace = buildBasicAce(AceRecord.ACCESS_ALLOWED, 0x10, 0x000F01FF,
                new byte[]{0x01, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x05,
                        0x12, 0x00, 0x00, 0x00});
        byte[] dacl = buildDacl(ace);
        byte[] sd = buildSecurityDescriptorWithDacl(0, dacl);

        var result = SecurityDescriptorParser.parseBytes(sd);

        assertEquals(1, result.aces().size());
        assertTrue(result.aces().get(0).isInherited());
    }

    @Test
    void parseBytes_deniedAce_notAllow() {
        byte[] ace = buildBasicAce(AceRecord.ACCESS_DENIED, 0x00, 0x000F01FF,
                new byte[]{0x01, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x05,
                        0x12, 0x00, 0x00, 0x00});
        byte[] dacl = buildDacl(ace);
        byte[] sd = buildSecurityDescriptorWithDacl(0, dacl);

        var result = SecurityDescriptorParser.parseBytes(sd);

        assertEquals(1, result.aces().size());
        assertFalse(result.aces().get(0).isAllow());
    }

    @Test
    void parseBytes_multipleAces_allParsed() {
        byte[] ace1 = buildBasicAce(AceRecord.ACCESS_ALLOWED, 0x00, 0x000F01FF,
                new byte[]{0x01, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x05,
                        0x12, 0x00, 0x00, 0x00});
        byte[] ace2 = buildBasicAce(AceRecord.ACCESS_ALLOWED, 0x00, 0x00080000,
                new byte[]{0x01, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x05,
                        0x12, 0x00, 0x00, 0x00});
        byte[] combined = new byte[ace1.length + ace2.length];
        System.arraycopy(ace1, 0, combined, 0, ace1.length);
        System.arraycopy(ace2, 0, combined, ace1.length, ace2.length);

        byte[] dacl = buildDaclRaw(2, combined);
        byte[] sd = buildSecurityDescriptorWithDacl(0, dacl);

        var result = SecurityDescriptorParser.parseBytes(sd);

        assertEquals(2, result.aces().size());
    }

    @Test
    void parse_base64Encoded_works() {
        byte[] ace = buildBasicAce(AceRecord.ACCESS_ALLOWED, 0x00, 0x000F01FF,
                new byte[]{0x01, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x05,
                        0x12, 0x00, 0x00, 0x00});
        byte[] dacl = buildDacl(ace);
        byte[] sd = buildSecurityDescriptorWithDacl(0, dacl);
        String b64 = Base64.getEncoder().encodeToString(sd);

        var result = SecurityDescriptorParser.parse(b64);

        assertEquals(1, result.aces().size());
    }

    @Test
    void parseBytes_ownerSid_extracted() {
        byte[] ownerSid = {0x01, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x05,
                0x12, 0x00, 0x00, 0x00};
        int ownerOffset = 20;
        byte[] sd = new byte[20 + ownerSid.length];
        ByteBuffer buf = ByteBuffer.wrap(sd).order(ByteOrder.LITTLE_ENDIAN);
        buf.put(0, (byte) 1);
        buf.putShort(2, (short) 0);
        buf.putInt(4, ownerOffset);
        buf.putInt(16, 0);
        System.arraycopy(ownerSid, 0, sd, ownerOffset, ownerSid.length);

        var result = SecurityDescriptorParser.parseBytes(sd);
        assertEquals("S-1-5-18", result.ownerSid());
    }

    @Test
    void parseBytes_objectAce_guidsParsed() {
        byte[] objGuid = guidToBytes("00299570-246d-11d0-a768-00aa006e0529");
        byte[] inhGuid = guidToBytes("bf967aba-0de6-11d0-a285-00aa003049e2");
        byte[] sid = {0x01, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x05,
                0x12, 0x00, 0x00, 0x00};
        byte[] aceData = buildObjectAce(AceRecord.ACCESS_ALLOWED_OBJECT, 0x00,
                AccessMask.ADS_RIGHT_DS_CONTROL_ACCESS, 0x03, objGuid, inhGuid, sid);
        byte[] dacl = buildDacl(aceData);
        byte[] sd = buildSecurityDescriptorWithDacl(0, dacl);

        var result = SecurityDescriptorParser.parseBytes(sd);

        assertEquals(1, result.aces().size());
        AceRecord ace = result.aces().get(0);
        assertTrue(ace.isObjectAce());
        assertEquals("00299570-246d-11d0-a768-00aa006e0529", ace.objectType());
        assertEquals("bf967aba-0de6-11d0-a285-00aa003049e2", ace.inheritedType());
    }

    @Test
    void parseBytes_objectAce_onlyObjectType() {
        byte[] objGuid = guidToBytes("1131f6aa-9c07-11d1-f79f-00c04fc2dcd2");
        byte[] sid = {0x01, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x05,
                0x12, 0x00, 0x00, 0x00};
        byte[] aceData = buildObjectAce(AceRecord.ACCESS_ALLOWED_OBJECT, 0x00,
                AccessMask.ADS_RIGHT_DS_CONTROL_ACCESS, 0x01, objGuid, null, sid);
        byte[] dacl = buildDacl(aceData);
        byte[] sd = buildSecurityDescriptorWithDacl(0, dacl);

        var result = SecurityDescriptorParser.parseBytes(sd);

        assertEquals(1, result.aces().size());
        AceRecord ace = result.aces().get(0);
        assertEquals("1131f6aa-9c07-11d1-f79f-00c04fc2dcd2", ace.objectType());
        assertEquals("", ace.inheritedType());
    }

    private static byte[] buildSecurityDescriptor(int control, int daclOffset) {
        ByteBuffer buf = ByteBuffer.allocate(20).order(ByteOrder.LITTLE_ENDIAN);
        buf.put((byte) 1);     // revision
        buf.put((byte) 0);     // sbz1
        buf.putShort((short) control);
        buf.putInt(0);          // owner offset
        buf.putInt(0);          // group offset
        buf.putInt(0);          // sacl offset
        buf.putInt(daclOffset); // dacl offset
        return buf.array();
    }

    private static byte[] buildSecurityDescriptorWithDacl(int control, byte[] dacl) {
        int daclOffset = 20;
        ByteBuffer buf = ByteBuffer.allocate(20 + dacl.length).order(ByteOrder.LITTLE_ENDIAN);
        buf.put((byte) 1);
        buf.put((byte) 0);
        buf.putShort((short) control);
        buf.putInt(0);
        buf.putInt(0);
        buf.putInt(0);
        buf.putInt(daclOffset);
        buf.put(dacl);
        return buf.array();
    }

    private static byte[] buildDacl(byte[] singleAce) {
        return buildDaclRaw(1, singleAce);
    }

    private static byte[] buildDaclRaw(int aceCount, byte[] aceData) {
        int size = 8 + aceData.length;
        ByteBuffer buf = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN);
        buf.put((byte) 2);       // revision
        buf.put((byte) 0);       // sbz1
        buf.putShort((short) size);
        buf.putShort((short) aceCount);
        buf.putShort((short) 0); // sbz2
        buf.put(aceData);
        return buf.array();
    }

    private static byte[] buildBasicAce(int type, int flags, int mask, byte[] sid) {
        int size = 4 + 4 + sid.length;
        ByteBuffer buf = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN);
        buf.put((byte) type);
        buf.put((byte) flags);
        buf.putShort((short) size);
        buf.putInt(mask);
        buf.put(sid);
        return buf.array();
    }

    private static byte[] buildObjectAce(int type, int flags, int mask,
            int objFlags, byte[] objGuid, byte[] inhGuid, byte[] sid) {
        int guidsLen = 0;
        if ((objFlags & 0x01) != 0 && objGuid != null) guidsLen += 16;
        if ((objFlags & 0x02) != 0 && inhGuid != null) guidsLen += 16;
        int size = 4 + 4 + 4 + guidsLen + sid.length;
        ByteBuffer buf = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN);
        buf.put((byte) type);
        buf.put((byte) flags);
        buf.putShort((short) size);
        buf.putInt(mask);
        buf.putInt(objFlags);
        if ((objFlags & 0x01) != 0 && objGuid != null) buf.put(objGuid);
        if ((objFlags & 0x02) != 0 && inhGuid != null) buf.put(inhGuid);
        buf.put(sid);
        return buf.array();
    }

    private static byte[] guidToBytes(String guid) {
        String[] parts = guid.split("-");
        ByteBuffer buf = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN);
        buf.putInt(Integer.parseUnsignedInt(parts[0], 16));
        buf.putShort((short) Integer.parseUnsignedInt(parts[1], 16));
        buf.putShort((short) Integer.parseUnsignedInt(parts[2], 16));
        byte[] d4 = new byte[8];
        String hex45 = parts[3] + parts[4];
        for (int i = 0; i < 8; i++) {
            d4[i] = (byte) Integer.parseUnsignedInt(hex45.substring(i * 2, i * 2 + 2), 16);
        }
        buf.put(d4);
        return buf.array();
    }
}
