package com.faultcraft.weaver.ad.resolve;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.faultcraft.weaver.ad.model.BloodHoundComputer;
import com.faultcraft.weaver.ad.model.BloodHoundGroup;
import com.faultcraft.weaver.ad.model.BloodHoundObject;
import com.faultcraft.weaver.ad.model.BloodHoundUser;

/** Resolves group membership relationships. */
final class GroupMembershipResolver {

    private GroupMembershipResolver() {}

    /**
     * Resolves group membership from both directions:
     * <ul>
     *   <li>memberOf on users/computers/groups -> add to group Members</li>
     *   <li>member on groups -> resolve DN to objects, add to Members</li>
     *   <li>primaryGroupId -> add to primary group Members</li>
     * </ul>
     */
    static void resolve(List<BloodHoundObject> objects,
            Map<String, BloodHoundObject> dnIndex,
            Map<String, BloodHoundObject> sidIndex) {

        for (BloodHoundObject obj : objects) {
            if (obj instanceof BloodHoundUser user) {
                resolveMemberOf(user.getMemberOfDns(), user, dnIndex);
                resolvePrimaryGroup(user.getPrimaryGroupSid(), user, sidIndex);
            } else if (obj instanceof BloodHoundComputer comp) {
                resolveMemberOf(comp.getMemberOfDns(), comp, dnIndex);
                resolvePrimaryGroup(comp.getPrimaryGroupSid(), comp, sidIndex);
            } else if (obj instanceof BloodHoundGroup group) {
                resolveMemberOf(group.getMemberOfDns(), group, dnIndex);
                resolveDirectMembers(group, dnIndex);
            }
        }
    }

    private static void resolveMemberOf(List<String> memberOfDns,
            BloodHoundObject member, Map<String, BloodHoundObject> dnIndex) {
        for (String dn : memberOfDns) {
            BloodHoundObject group = dnIndex.get(dn.toUpperCase(Locale.ROOT));
            if (group instanceof BloodHoundGroup g) {
                addMember(g, member);
            }
        }
    }

    private static void resolveDirectMembers(BloodHoundGroup group,
            Map<String, BloodHoundObject> dnIndex) {
        for (String dn : group.getMemberDns()) {
            BloodHoundObject member = dnIndex.get(dn.toUpperCase(Locale.ROOT));
            if (member != null) {
                addMember(group, member);
            }
        }
    }

    private static void resolvePrimaryGroup(String primaryGroupSid,
            BloodHoundObject member, Map<String, BloodHoundObject> sidIndex) {
        if (primaryGroupSid == null || primaryGroupSid.isEmpty()) {
            return;
        }
        BloodHoundObject group = sidIndex.get(primaryGroupSid);
        if (group instanceof BloodHoundGroup g) {
            addMember(g, member);
        }
    }

    private static void addMember(BloodHoundGroup group, BloodHoundObject member) {
        String id = member.getObjectIdentifier();
        boolean alreadyAdded = group.getMembers().stream()
                .anyMatch(m -> id.equals(m.get("ObjectIdentifier")));
        if (!alreadyAdded) {
            group.getMembers().add(Map.of(
                    "ObjectIdentifier", id,
                    "ObjectType", capitalize(member.objectType())));
        }
    }

    private static String capitalize(String objectType) {
        if (objectType.isEmpty()) {
            return objectType;
        }
        return objectType.substring(0, 1).toUpperCase(Locale.ROOT)
                + objectType.substring(1);
    }
}
