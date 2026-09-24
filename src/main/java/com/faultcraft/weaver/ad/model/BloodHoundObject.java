package com.faultcraft.weaver.ad.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.faultcraft.weaver.util.GuidParser;
import com.faultcraft.weaver.util.SidParser;
import com.faultcraft.weaver.util.TimestampParser;

/**
 * Base class for all BloodHound CE AD object types.
 *
 * <p>Subclasses must implement {@link #objectType()} and {@link #toJson()}.
 * Common fields (ObjectIdentifier, Properties, Aces, etc.) are managed here.
 */
public abstract class BloodHoundObject {

    private String objectIdentifier = "";
    private final TypedProperties properties = new TypedProperties();
    private final List<Map<String, Object>> aces = new ArrayList<>();
    private boolean isAclProtected = false;
    private boolean isDeleted = false;
    private String rawNtSecurityDescriptor = "";

    /** Returns the BH-CE object type name (e.g. "users", "computers"). */
    public abstract String objectType();

    /** Serializes this object to a BH-CE v6 JSON-compatible map. */
    public abstract Map<String, Object> toJson();

    /** Returns the object's unique identifier (SID or GUID). */
    public String getObjectIdentifier() {
        return objectIdentifier;
    }

    /** Returns the typed properties map. */
    public TypedProperties getProperties() {
        return properties;
    }

    /** Returns the ACE list (populated during ACL processing). */
    public List<Map<String, Object>> getAces() {
        return aces;
    }

    /** Returns whether the ACL is protected. */
    public boolean isAclProtected() {
        return isAclProtected;
    }

    /** Sets the ACL protected flag. */
    public void setAclProtected(boolean aclProtected) {
        isAclProtected = aclProtected;
    }

    /** Returns the raw ntSecurityDescriptor value for ACL processing. */
    public String getRawNtSecurityDescriptor() {
        return rawNtSecurityDescriptor;
    }

    /** Returns whether this object is deleted. */
    public boolean isDeleted() {
        return isDeleted;
    }

    /**
     * Populates common fields from a merged LDAP entry.
     *
     * <p>Resolves ObjectIdentifier from objectSid/objectGUID, parses timestamps,
     * extracts name from DN if missing.
     */
    protected void populateCommon(Map<String, List<String>> entry) {
        String sid = firstValue(entry, "objectsid");
        String guid = firstValue(entry, "objectguid");
        String dn = firstValue(entry, "distinguishedname");

        objectIdentifier = resolveIdentifier(sid, guid);

        String domain = dnToDomain(dn);
        properties.put("domain", domain);
        properties.put("domainsid", "");
        properties.put("distinguishedname", dn);

        String name = firstValue(entry, "name");
        if (name.isEmpty()) {
            name = extractCnFromDn(dn);
        }
        properties.put("name", name.isEmpty() ? name : name + "@" + domain);

        properties.put("description", firstValue(entry, "description"));

        String whenCreated = firstValue(entry, "whencreated");
        if (!whenCreated.isEmpty()) {
            properties.put("whencreated", TimestampParser.fromGeneralizedTime(whenCreated));
        }

        rawNtSecurityDescriptor = firstValue(entry, "ntsecuritydescriptor");

        String deleted = firstValue(entry, "isdeleted");
        isDeleted = "TRUE".equalsIgnoreCase(deleted);
        properties.put("isdeleted", isDeleted);
    }

    /** Builds the common JSON fields shared by all object types. */
    protected Map<String, Object> baseJson() {
        var json = new LinkedHashMap<String, Object>();
        json.put("ObjectIdentifier", objectIdentifier);
        json.put("Properties", properties.toOutputMap());
        json.put("Aces", List.copyOf(aces));
        json.put("IsACLProtected", isAclProtected);
        json.put("IsDeleted", isDeleted);
        return json;
    }

    private String resolveIdentifier(String sid, String guid) {
        String parsedSid = SidParser.parseAny(sid);
        if (!parsedSid.isEmpty()) {
            return parsedSid;
        }
        String parsedGuid = GuidParser.parseAny(guid);
        if (!parsedGuid.isEmpty()) {
            return parsedGuid;
        }
        return "";
    }

    /** Extracts a DNS domain name from a distinguished name. */
    protected static String dnToDomain(String dn) {
        if (dn == null || dn.isEmpty()) {
            return "";
        }
        var sb = new StringBuilder();
        String upper = dn.toUpperCase(Locale.ROOT);
        int idx = 0;
        while ((idx = upper.indexOf("DC=", idx)) >= 0) {
            int start = idx + 3;
            int end = upper.indexOf(',', start);
            if (end < 0) {
                end = upper.length();
            }
            if (sb.length() > 0) {
                sb.append('.');
            }
            sb.append(dn, start, end);
            idx = end;
        }
        return sb.toString().toUpperCase(Locale.ROOT);
    }

    /** Extracts the CN value from the first RDN of a distinguished name. */
    protected static String extractCnFromDn(String dn) {
        if (dn == null || dn.isEmpty()) {
            return "";
        }
        String upper = dn.toUpperCase(Locale.ROOT);
        if (upper.startsWith("CN=")) {
            int end = dn.indexOf(',');
            return end < 0 ? dn.substring(3) : dn.substring(3, end);
        }
        if (upper.startsWith("OU=")) {
            int end = dn.indexOf(',');
            return end < 0 ? dn.substring(3) : dn.substring(3, end);
        }
        return "";
    }

    /** Returns the first value for a key, or empty string. */
    protected static String firstValue(Map<String, List<String>> entry, String key) {
        List<String> vals = entry.get(key);
        if (vals == null || vals.isEmpty()) {
            return "";
        }
        return vals.get(0);
    }

    /** Returns all values for a key, or empty list. */
    protected static List<String> allValues(Map<String, List<String>> entry, String key) {
        List<String> vals = entry.get(key);
        return vals != null ? vals : List.of();
    }
}
