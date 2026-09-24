package com.faultcraft.weaver.ad.resolve;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.faultcraft.weaver.ad.model.BloodHoundComputer;
import com.faultcraft.weaver.ad.model.BloodHoundGroup;
import com.faultcraft.weaver.ad.model.BloodHoundObject;
import com.faultcraft.weaver.ad.model.BloodHoundUser;

class GroupMembershipResolverTest {

    @Test
    void resolve_userMemberOf_addedToGroupMembers() {
        var group = makeGroup(
                "CN=Domain Users,CN=Users,DC=test,DC=local", "S-1-5-21-1-2-3-513");
        var user = makeUser(
                "CN=jdoe,CN=Users,DC=test,DC=local", "S-1-5-21-1-2-3-1001",
                "CN=Domain Users,CN=Users,DC=test,DC=local");

        var objects = new ArrayList<BloodHoundObject>(List.of(group, user));
        var dnIndex = buildDnIndex(objects);
        var sidIndex = buildSidIndex(objects);

        GroupMembershipResolver.resolve(objects, dnIndex, sidIndex);

        assertEquals(1, group.getMembers().size());
        assertEquals("S-1-5-21-1-2-3-1001", group.getMembers().get(0).get("ObjectIdentifier"));
        assertEquals("Users", group.getMembers().get(0).get("ObjectType"));
    }

    @Test
    void resolve_computerMemberOf_addedToGroupMembers() {
        var group = makeGroup(
                "CN=Domain Computers,CN=Users,DC=test,DC=local", "S-1-5-21-1-2-3-515");
        var comp = makeComputer(
                "CN=WS01,CN=Computers,DC=test,DC=local", "S-1-5-21-1-2-3-1101",
                "CN=Domain Computers,CN=Users,DC=test,DC=local");

        var objects = new ArrayList<BloodHoundObject>(List.of(group, comp));
        var dnIndex = buildDnIndex(objects);
        var sidIndex = buildSidIndex(objects);

        GroupMembershipResolver.resolve(objects, dnIndex, sidIndex);

        assertEquals(1, group.getMembers().size());
        assertEquals("Computers", group.getMembers().get(0).get("ObjectType"));
    }

    @Test
    void resolve_primaryGroup_addedToGroupMembers() {
        var group = makeGroup(
                "CN=Domain Users,CN=Users,DC=test,DC=local", "S-1-5-21-1-2-3-513");
        var user = makeUserWithPrimaryGroup(
                "CN=jdoe,CN=Users,DC=test,DC=local", "S-1-5-21-1-2-3-1001", "513");

        var objects = new ArrayList<BloodHoundObject>(List.of(group, user));
        var dnIndex = buildDnIndex(objects);
        var sidIndex = buildSidIndex(objects);

        GroupMembershipResolver.resolve(objects, dnIndex, sidIndex);

        assertEquals(1, group.getMembers().size());
        assertEquals("S-1-5-21-1-2-3-1001", group.getMembers().get(0).get("ObjectIdentifier"));
    }

    @Test
    void resolve_groupMemberOf_addedToParentGroup() {
        var parent = makeGroup(
                "CN=Enterprise Admins,CN=Users,DC=test,DC=local", "S-1-5-21-1-2-3-519");
        var child = makeGroupWithMemberOf(
                "CN=Domain Admins,CN=Users,DC=test,DC=local", "S-1-5-21-1-2-3-512",
                "CN=Enterprise Admins,CN=Users,DC=test,DC=local");

        var objects = new ArrayList<BloodHoundObject>(List.of(parent, child));
        var dnIndex = buildDnIndex(objects);
        var sidIndex = buildSidIndex(objects);

        GroupMembershipResolver.resolve(objects, dnIndex, sidIndex);

        assertEquals(1, parent.getMembers().size());
        assertEquals("Groups", parent.getMembers().get(0).get("ObjectType"));
    }

    @Test
    void resolve_directMemberAttribute_addedToGroupMembers() {
        var group = makeGroupWithMembers(
                "CN=Admins,CN=Users,DC=test,DC=local", "S-1-5-21-1-2-3-512",
                "CN=jdoe,CN=Users,DC=test,DC=local");
        var user = makeUser(
                "CN=jdoe,CN=Users,DC=test,DC=local", "S-1-5-21-1-2-3-1001", "");

        var objects = new ArrayList<BloodHoundObject>(List.of(group, user));
        var dnIndex = buildDnIndex(objects);
        var sidIndex = buildSidIndex(objects);

        GroupMembershipResolver.resolve(objects, dnIndex, sidIndex);

        assertEquals(1, group.getMembers().size());
        assertEquals("S-1-5-21-1-2-3-1001", group.getMembers().get(0).get("ObjectIdentifier"));
    }

    @Test
    void resolve_duplicateMembership_deduplicated() {
        var group = makeGroupWithMembers(
                "CN=Admins,CN=Users,DC=test,DC=local", "S-1-5-21-1-2-3-512",
                "CN=jdoe,CN=Users,DC=test,DC=local");
        var user = makeUser(
                "CN=jdoe,CN=Users,DC=test,DC=local", "S-1-5-21-1-2-3-1001",
                "CN=Admins,CN=Users,DC=test,DC=local");

        var objects = new ArrayList<BloodHoundObject>(List.of(group, user));
        var dnIndex = buildDnIndex(objects);
        var sidIndex = buildSidIndex(objects);

        GroupMembershipResolver.resolve(objects, dnIndex, sidIndex);

        assertEquals(1, group.getMembers().size());
    }

    @Test
    void resolve_unknownGroupDn_noError() {
        var user = makeUser(
                "CN=jdoe,CN=Users,DC=test,DC=local", "S-1-5-21-1-2-3-1001",
                "CN=Missing,CN=Users,DC=test,DC=local");

        var objects = new ArrayList<BloodHoundObject>(List.of(user));
        var dnIndex = buildDnIndex(objects);
        var sidIndex = buildSidIndex(objects);

        assertDoesNotThrow(() ->
                GroupMembershipResolver.resolve(objects, dnIndex, sidIndex));
    }

    private static BloodHoundGroup makeGroup(String dn, String sid) {
        return BloodHoundGroup.fromEntry(Map.of(
                "distinguishedname", List.of(dn),
                "objectsid", List.of(sid),
                "samaccountname", List.of("Group1")));
    }

    private static BloodHoundGroup makeGroupWithMemberOf(String dn, String sid, String memberOf) {
        return BloodHoundGroup.fromEntry(Map.of(
                "distinguishedname", List.of(dn),
                "objectsid", List.of(sid),
                "samaccountname", List.of("Group2"),
                "memberof", List.of(memberOf)));
    }

    private static BloodHoundGroup makeGroupWithMembers(String dn, String sid, String memberDn) {
        return BloodHoundGroup.fromEntry(Map.of(
                "distinguishedname", List.of(dn),
                "objectsid", List.of(sid),
                "samaccountname", List.of("Admins"),
                "member", List.of(memberDn)));
    }

    private static BloodHoundUser makeUser(String dn, String sid, String memberOf) {
        var entry = new HashMap<String, List<String>>();
        entry.put("distinguishedname", List.of(dn));
        entry.put("objectsid", List.of(sid));
        entry.put("samaccountname", List.of("jdoe"));
        if (!memberOf.isEmpty()) {
            entry.put("memberof", List.of(memberOf));
        }
        return BloodHoundUser.fromEntry(entry);
    }

    private static BloodHoundUser makeUserWithPrimaryGroup(String dn, String sid, String pgid) {
        return BloodHoundUser.fromEntry(Map.of(
                "distinguishedname", List.of(dn),
                "objectsid", List.of(sid),
                "samaccountname", List.of("jdoe"),
                "primarygroupid", List.of(pgid)));
    }

    private static BloodHoundComputer makeComputer(String dn, String sid, String memberOf) {
        var entry = new HashMap<String, List<String>>();
        entry.put("distinguishedname", List.of(dn));
        entry.put("objectsid", List.of(sid));
        entry.put("samaccountname", List.of("WS01$"));
        if (!memberOf.isEmpty()) {
            entry.put("memberof", List.of(memberOf));
        }
        return BloodHoundComputer.fromEntry(entry);
    }

    private static Map<String, BloodHoundObject> buildDnIndex(List<BloodHoundObject> objects) {
        var index = new HashMap<String, BloodHoundObject>();
        for (BloodHoundObject obj : objects) {
            String dn = obj.getProperties().getString("distinguishedname");
            if (!dn.isEmpty()) {
                index.put(dn.toUpperCase(Locale.ROOT), obj);
            }
        }
        return index;
    }

    private static Map<String, BloodHoundObject> buildSidIndex(List<BloodHoundObject> objects) {
        var index = new HashMap<String, BloodHoundObject>();
        for (BloodHoundObject obj : objects) {
            if (!obj.getObjectIdentifier().isEmpty()) {
                index.put(obj.getObjectIdentifier(), obj);
            }
        }
        return index;
    }
}
