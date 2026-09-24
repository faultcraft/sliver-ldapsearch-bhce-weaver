package com.faultcraft.weaver.ad.resolve;

import java.util.List;

import com.faultcraft.weaver.ad.model.BloodHoundObject;

/**
 * Stub resolver for local group edges (AdminTo, CanRDP, CanPSRemote, ExecuteDCOM).
 *
 * <p>sa-ldapsearch does not collect local group or session data (those require
 * SAM/registry/NetSession enumeration), so these edges remain empty. Computer
 * objects already set Sessions/PrivilegedSessions/RegistrySessions/LocalGroups
 * to NOT_COLLECTED status.
 */
final class LocalGroupResolver {

    private LocalGroupResolver() {}

    static void resolve(List<BloodHoundObject> objects) {
        // No local group data available from LDAP-only collection.
    }
}
