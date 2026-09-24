package com.faultcraft.weaver.ad.acl;

/**
 * Parsed ACE from an NT security descriptor DACL.
 *
 * @param aceType       ACE type byte (0=allow, 1=deny, 5=allow-object, 6=deny-object)
 * @param aceFlags      ACE flags byte (inheritance flags)
 * @param accessMask    access mask bits
 * @param objectType    object type GUID (empty if not present)
 * @param inheritedType inherited object type GUID (empty if not present)
 * @param principalSid  SID of the principal the ACE applies to
 */
public record AceRecord(
        int aceType,
        int aceFlags,
        int accessMask,
        String objectType,
        String inheritedType,
        String principalSid) {

    public static final int ACCESS_ALLOWED = 0x00;
    public static final int ACCESS_DENIED = 0x01;
    public static final int ACCESS_ALLOWED_OBJECT = 0x05;
    public static final int ACCESS_DENIED_OBJECT = 0x06;

    public static final int INHERITED_ACE = 0x10;

    /** Returns true if this is an allow ACE (type 0 or 5). */
    public boolean isAllow() {
        return aceType == ACCESS_ALLOWED || aceType == ACCESS_ALLOWED_OBJECT;
    }

    /** Returns true if this ACE is inherited from a parent container. */
    public boolean isInherited() {
        return (aceFlags & INHERITED_ACE) != 0;
    }

    /** Returns true if this is an object ACE with optional GUID fields. */
    public boolean isObjectAce() {
        return aceType == ACCESS_ALLOWED_OBJECT || aceType == ACCESS_DENIED_OBJECT;
    }
}
