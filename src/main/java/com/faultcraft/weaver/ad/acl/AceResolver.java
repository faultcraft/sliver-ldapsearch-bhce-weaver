package com.faultcraft.weaver.ad.acl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Resolves parsed ACEs into BH-CE edge rights.
 *
 * <p>Filters out inherited ACEs and well-known system principals,
 * then maps access masks + object GUIDs to named rights considering
 * the target object type for context-specific edges.
 */
public final class AceResolver {

    private static final Set<String> FILTERED_SIDS = Set.of(
            "S-1-3-0",   // CREATOR OWNER
            "S-1-5-18",  // LOCAL SYSTEM
            "S-1-5-10"   // PRINCIPAL SELF
    );

    private static final int INHERIT_ONLY_ACE = 0x08;

    /** Resolves ACEs for a given object type, returning BH-CE formatted ACE maps. */
    public List<Map<String, Object>> resolve(
            List<AceRecord> aces, String objectType, boolean hasLaps) {
        var resolved = new ArrayList<Map<String, Object>>();

        for (AceRecord ace : aces) {
            if (!ace.isAllow()) {
                continue;
            }
            if (FILTERED_SIDS.contains(ace.principalSid())) {
                continue;
            }

            boolean inherited = ace.isInherited();

            if (ace.isObjectAce()) {
                resolveObjectAce(ace, objectType, hasLaps, inherited, resolved);
            } else {
                resolveBasicAce(ace, objectType, hasLaps, inherited, resolved);
            }
        }

        return List.copyOf(resolved);
    }

    public List<Map<String, Object>> resolve(List<AceRecord> aces, String objectType) {
        return resolve(aces, objectType, false);
    }

    private void resolveBasicAce(
            AceRecord ace, String objectType, boolean hasLaps,
            boolean inherited, List<Map<String, Object>> out) {

        if (!inherited && (ace.aceFlags() & INHERIT_ONLY_ACE) != 0) {
            return;
        }

        int mask = ace.accessMask();

        if (AccessMask.hasGenericAll(mask)) {
            out.add(buildAce(ace, "GenericAll", inherited));
            return;
        }

        if (AccessMask.hasWriteProperty(mask)) {
            out.add(buildAce(ace, "GenericWrite", inherited));
        }

        if (AccessMask.hasWriteOwner(mask)) {
            out.add(buildAce(ace, "WriteOwner", inherited));
        }

        if (AccessMask.hasWriteDacl(mask)) {
            out.add(buildAce(ace, "WriteDacl", inherited));
        }

        if (AccessMask.hasExtendedRight(mask)) {
            resolveAllExtendedRights(objectType, hasLaps, ace, inherited, out);
        }
    }

    private void resolveObjectAce(
            AceRecord ace, String objectType, boolean hasLaps,
            boolean inherited, List<Map<String, Object>> out) {
        int mask = ace.accessMask();
        String objGuid = ace.objectType();
        boolean genericEdge = WellKnownGuids.isEmptyGuid(objGuid);

        if (AccessMask.hasGenericAll(mask) && genericEdge) {
            out.add(buildAce(ace, "GenericAll", inherited));
            return;
        }

        if (AccessMask.hasGenericWrite(mask) && genericEdge) {
            out.add(buildAce(ace, "GenericWrite", inherited));
        }

        if (AccessMask.hasWriteOwner(mask) && genericEdge) {
            out.add(buildAce(ace, "WriteOwner", inherited));
        }

        if (AccessMask.hasWriteDacl(mask) && genericEdge) {
            out.add(buildAce(ace, "WriteDacl", inherited));
        }

        if (AccessMask.hasWriteProperty(mask)) {
            resolveWriteProperty(ace, objectType, genericEdge, inherited, out);
        } else if (AccessMask.hasSelfRight(mask)) {
            resolveSelfWrite(ace, objectType, genericEdge, inherited, out);
        }

        if (AccessMask.hasExtendedRight(mask)) {
            resolveExtendedRight(ace, objectType, hasLaps, genericEdge, inherited, out);
        }
    }

    private void resolveWriteProperty(
            AceRecord ace, String objectType, boolean genericEdge,
            boolean inherited, List<Map<String, Object>> out) {

        if (genericEdge) {
            out.add(buildAce(ace, "GenericWrite", inherited));
            return;
        }

        String guid = WellKnownGuids.normalizeGuid(ace.objectType());
        String right = WellKnownGuids.getWriteProperty(guid);
        if (right == null) {
            return;
        }

        switch (right) {
            case "AddMember" -> {
                if ("groups".equals(objectType)) {
                    out.add(buildAce(ace, "AddMember", inherited));
                }
            }
            case "AddAllowedToAct" -> {
                if ("computers".equals(objectType)) {
                    out.add(buildAce(ace, "AddAllowedToAct", inherited));
                }
            }
            case "WriteAccountRestrictions" -> {
                if ("computers".equals(objectType) || "users".equals(objectType)) {
                    if (!ace.principalSid().endsWith("-512")) {
                        out.add(buildAce(ace, "WriteAccountRestrictions", inherited));
                    }
                }
            }
            case "WriteGPLink" -> {
                if ("ous".equals(objectType) || "domains".equals(objectType)) {
                    out.add(buildAce(ace, "WriteGPLink", inherited));
                }
            }
            case "AddKeyCredentialLink" -> {
                if ("users".equals(objectType) || "computers".equals(objectType)) {
                    out.add(buildAce(ace, "AddKeyCredentialLink", inherited));
                }
            }
            case "WriteSPN" -> {
                if ("users".equals(objectType) || "computers".equals(objectType)) {
                    out.add(buildAce(ace, "WriteSPN", inherited));
                }
            }
            case "WritePKINameFlag" -> {
                if ("certtemplates".equals(objectType)) {
                    out.add(buildAce(ace, "WritePKINameFlag", inherited));
                }
            }
            case "WritePKIEnrollmentFlag" -> {
                if ("certtemplates".equals(objectType)) {
                    out.add(buildAce(ace, "WritePKIEnrollmentFlag", inherited));
                }
            }
        }
    }

    private void resolveSelfWrite(
            AceRecord ace, String objectType, boolean genericEdge,
            boolean inherited, List<Map<String, Object>> out) {
        if (!"groups".equals(objectType)) {
            return;
        }
        String guid = WellKnownGuids.normalizeGuid(ace.objectType());
        if (genericEdge || WellKnownGuids.getSelfWrite(guid) != null) {
            out.add(buildAce(ace, "AddSelf", inherited));
        }
    }

    private void resolveExtendedRight(
            AceRecord ace, String objectType, boolean hasLaps,
            boolean genericEdge, boolean inherited, List<Map<String, Object>> out) {
        if (genericEdge) {
            resolveAllExtendedRights(objectType, hasLaps, ace, inherited, out);
            return;
        }

        String guid = WellKnownGuids.normalizeGuid(ace.objectType());

        if ("domains".equals(objectType)) {
            String right = WellKnownGuids.getExtendedRight(guid);
            if (right != null) {
                out.add(buildAce(ace, right, inherited));
            }
        }

        if ("users".equals(objectType) || "computers".equals(objectType)) {
            if (WellKnownGuids.FORCE_CHANGE_PASSWORD.equals(guid)) {
                out.add(buildAce(ace, "ForceChangePassword", inherited));
            }
        }

        if ("computers".equals(objectType) && hasLaps) {
            if (WellKnownGuids.isLapsGuid(guid)) {
                out.add(buildAce(ace, "ReadLAPSPassword", inherited));
            }
        }

        if ("certtemplates".equals(objectType) || "enterprisecas".equals(objectType)) {
            if (WellKnownGuids.ENROLL.equals(guid)) {
                out.add(buildAce(ace, "Enroll", inherited));
            }
        }
    }

    private void resolveAllExtendedRights(
            String objectType, boolean hasLaps, AceRecord ace,
            boolean inherited, List<Map<String, Object>> out) {
        if ("users".equals(objectType) || "domains".equals(objectType)) {
            out.add(buildAce(ace, "AllExtendedRights", inherited));
        }
        if ("computers".equals(objectType) && hasLaps) {
            out.add(buildAce(ace, "AllExtendedRights", inherited));
        }
    }

    private Map<String, Object> buildAce(AceRecord ace, String rightName, boolean inherited) {
        var map = new LinkedHashMap<String, Object>();
        map.put("PrincipalSID", ace.principalSid());
        map.put("PrincipalType", "");
        map.put("RightName", rightName);
        map.put("IsInherited", inherited);
        return map;
    }
}
