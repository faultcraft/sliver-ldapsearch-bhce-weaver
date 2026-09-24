package com.faultcraft.weaver.ad.acl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import com.faultcraft.weaver.util.Log;
import com.faultcraft.weaver.util.SidParser;

/**
 * Parses NT security descriptors per MS-DTYP 2.4.6.
 *
 * <p>Extracts the DACL and parses each ACE into an AceRecord. Also reports
 * whether the DACL is protected (SE_DACL_PROTECTED flag).
 */
public final class SecurityDescriptorParser {

    private static final int HEADER_SIZE = 20;
    private static final int ACL_HEADER_SIZE = 8;
    private static final int ACE_HEADER_SIZE = 4;
    private static final int CONTROL_OFFSET = 2;
    private static final int DACL_OFFSET_POS = 16;
    private static final int SE_DACL_PROTECTED = 0x1000;
    private static final int ACE_OBJECT_TYPE_PRESENT = 0x01;
    private static final int ACE_INHERITED_OBJECT_TYPE_PRESENT = 0x02;

    private static final int OWNER_OFFSET_POS = 4;
    private static final int INHERIT_ONLY_ACE = 0x08;

    /** Result of parsing a security descriptor. */
    public record ParseResult(List<AceRecord> aces, boolean isDaclProtected, String ownerSid) {}

    /** Parses a base64-encoded NT security descriptor. */
    public static ParseResult parse(String base64Value) {
        if (base64Value == null || base64Value.isBlank()) {
            return new ParseResult(List.of(), false, "");
        }

        byte[] data;
        try {
            data = Base64.getDecoder().decode(base64Value.strip());
        } catch (IllegalArgumentException e) {
            Log.warn("Failed to decode ntSecurityDescriptor base64: %s", e.getMessage());
            return new ParseResult(List.of(), false, "");
        }

        return parseBytes(data);
    }

    /** Parses raw NT security descriptor bytes. */
    public static ParseResult parseBytes(byte[] data) {
        if (data == null || data.length < HEADER_SIZE) {
            return new ParseResult(List.of(), false, "");
        }

        ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);

        int control = buf.getShort(CONTROL_OFFSET) & 0xFFFF;
        boolean daclProtected = (control & SE_DACL_PROTECTED) != 0;

        int ownerOffset = buf.getInt(OWNER_OFFSET_POS);
        String ownerSid = "";
        if (ownerOffset > 0 && ownerOffset < data.length) {
            ownerSid = parseSidAt(data, ownerOffset);
        }

        int daclOffset = buf.getInt(DACL_OFFSET_POS);
        if (daclOffset == 0 || daclOffset >= data.length) {
            return new ParseResult(List.of(), daclProtected, ownerSid);
        }

        List<AceRecord> aces = parseDacl(data, daclOffset);
        return new ParseResult(List.copyOf(aces), daclProtected, ownerSid);
    }

    private static List<AceRecord> parseDacl(byte[] data, int offset) {
        if (offset + ACL_HEADER_SIZE > data.length) {
            Log.warn("DACL truncated at offset %d", offset);
            return List.of();
        }

        ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        int aceCount = buf.getShort(offset + 4) & 0xFFFF;

        var aces = new ArrayList<AceRecord>(aceCount);
        int pos = offset + ACL_HEADER_SIZE;

        for (int i = 0; i < aceCount && pos < data.length; i++) {
            if (pos + ACE_HEADER_SIZE > data.length) {
                Log.warn("ACE %d truncated at offset %d", i, pos);
                break;
            }

            int aceType = data[pos] & 0xFF;
            int aceFlags = data[pos + 1] & 0xFF;
            int aceSize = buf.getShort(pos + 2) & 0xFFFF;

            if (aceSize < ACE_HEADER_SIZE || pos + aceSize > data.length) {
                Log.warn("ACE %d has invalid size %d at offset %d", i, aceSize, pos);
                break;
            }

            AceRecord ace = parseAce(data, pos, aceType, aceFlags);
            if (ace != null) {
                aces.add(ace);
            }

            pos += aceSize;
        }

        return aces;
    }

    private static AceRecord parseAce(byte[] data, int offset, int aceType, int aceFlags) {
        ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);

        return switch (aceType) {
            case AceRecord.ACCESS_ALLOWED, AceRecord.ACCESS_DENIED ->
                    parseBasicAce(buf, data, offset, aceType, aceFlags);
            case AceRecord.ACCESS_ALLOWED_OBJECT, AceRecord.ACCESS_DENIED_OBJECT ->
                    parseObjectAce(buf, data, offset, aceType, aceFlags);
            default -> null;
        };
    }

    private static AceRecord parseBasicAce(
            ByteBuffer buf, byte[] data, int offset, int aceType, int aceFlags) {
        if (offset + ACE_HEADER_SIZE + 4 > data.length) {
            return null;
        }

        int mask = buf.getInt(offset + ACE_HEADER_SIZE);
        int sidOffset = offset + ACE_HEADER_SIZE + 4;
        String sid = parseSidAt(data, sidOffset);

        return new AceRecord(aceType, aceFlags, mask, "", "", sid);
    }

    private static AceRecord parseObjectAce(
            ByteBuffer buf, byte[] data, int offset, int aceType, int aceFlags) {
        int pos = offset + ACE_HEADER_SIZE;
        if (pos + 8 > data.length) {
            return null;
        }

        int mask = buf.getInt(pos);
        pos += 4;

        int flags = buf.getInt(pos);
        pos += 4;

        String objectType = "";
        if ((flags & ACE_OBJECT_TYPE_PRESENT) != 0) {
            if (pos + 16 > data.length) {
                return null;
            }
            objectType = readGuid(data, pos);
            pos += 16;
        }

        String inheritedType = "";
        if ((flags & ACE_INHERITED_OBJECT_TYPE_PRESENT) != 0) {
            if (pos + 16 > data.length) {
                return null;
            }
            inheritedType = readGuid(data, pos);
            pos += 16;
        }

        String sid = parseSidAt(data, pos);

        return new AceRecord(aceType, aceFlags, mask, objectType, inheritedType, sid);
    }

    private static String parseSidAt(byte[] data, int offset) {
        if (offset >= data.length) {
            return "";
        }
        int remaining = data.length - offset;
        byte[] sidBytes = new byte[remaining];
        System.arraycopy(data, offset, sidBytes, 0, remaining);
        return SidParser.parse(sidBytes);
    }

    private static String readGuid(byte[] data, int offset) {
        ByteBuffer buf = ByteBuffer.wrap(data, offset, 16).order(ByteOrder.LITTLE_ENDIAN);
        int d1 = buf.getInt();
        short d2 = buf.getShort();
        short d3 = buf.getShort();
        byte[] d4 = new byte[8];
        buf.get(d4);
        return String.format("%08x-%04x-%04x-%02x%02x-%02x%02x%02x%02x%02x%02x",
                d1, d2 & 0xFFFF, d3 & 0xFFFF,
                d4[0], d4[1], d4[2], d4[3], d4[4], d4[5], d4[6], d4[7]);
    }
}
