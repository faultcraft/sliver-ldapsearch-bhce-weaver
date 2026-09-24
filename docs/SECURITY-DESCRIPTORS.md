# NT Security Descriptor Parsing Reference

This document describes how BHCE Weaver parses NT security descriptors and resolves ACE entries into BloodHound CE relationship edges.

---

## 1. Security Descriptor Format

NT security descriptors follow the MS-DTYP 2.4.6 specification. The `nTSecurityDescriptor` attribute in Active Directory stores these as binary data (base64-encoded in LDAP results).

### Binary layout

```
Offset  Size  Field
0       1     Revision (must be 1)
1       1     Sbz1 (reserved)
2       2     Control flags (little-endian)
4       4     OffsetOwner (little-endian)
8       4     OffsetGroup (little-endian)
12      4     OffsetSacl (little-endian)
16      4     OffsetDacl (little-endian)
20+     var   Owner SID, Group SID, SACL, DACL
```

### Control flags

Weaver checks `SE_DACL_PROTECTED` (bit 12, 0x1000) to determine whether the DACL is protected from inheritance. This affects how BH-CE interprets ACL edges.

### Owner SID

The owner SID is extracted from the offset specified in OffsetOwner and converted to an "Owns" relationship in BH-CE.

---

## 2. DACL and ACE Entries

The Discretionary Access Control List (DACL) contains Access Control Entries (ACEs) that define who has what access to the object.

### ACE binary layout

```
Offset  Size  Field
0       1     AceType
1       1     AceFlags
2       2     AceSize (little-endian)
4       4     AccessMask (little-endian)
```

For object ACEs (types 0x05, 0x06), additional fields follow:

```
8       4     ObjectFlags (little-endian)
12      16    ObjectType GUID (if ObjectFlags bit 0 set)
12/28   16    InheritedObjectType GUID (if ObjectFlags bit 1 set)
var     var   SID
```

### ACE types

| Type | Value | Description |
|------|-------|-------------|
| ACCESS_ALLOWED | 0x00 | Grants access |
| ACCESS_DENIED | 0x01 | Denies access |
| ACCESS_ALLOWED_OBJECT | 0x05 | Grants access with object type constraint |
| ACCESS_DENIED_OBJECT | 0x06 | Denies access with object type constraint |

Weaver processes types 0x00 and 0x05 (allowed ACEs). Denied ACEs are not mapped to BH-CE edges.

---

## 3. Access Mask Constants

Active Directory uses specific access mask values that differ from standard Windows file access masks. Weaver uses the mapped values as they appear in stored security descriptors:

| Constant | Value | Description |
|----------|-------|-------------|
| GENERIC_ALL | 0x000F01FF | Full control |
| GENERIC_WRITE | 0x00020028 | Write properties and validated writes |
| WRITE_DACL | 0x00040000 | Modify the DACL |
| WRITE_OWNER | 0x00080000 | Take ownership |
| ADS_RIGHT_DS_CONTROL_ACCESS | 0x00000100 | Extended rights (with object type) |
| ADS_RIGHT_DS_WRITE_PROP | 0x00000020 | Write property (with object type) |
| ADS_RIGHT_DS_SELF | 0x00000008 | Validated writes |

---

## 4. ACE to BH-CE Edge Resolution

The ACE resolver maps each ACE to zero or more BH-CE relationship edges. Resolution is context-aware: the same access mask produces different edges depending on the target object type.

### Resolution logic

1. **GENERIC_ALL**: Maps to `GenericAll` regardless of target type.
2. **GENERIC_WRITE**: Maps to `GenericWrite` regardless of target type.
3. **WRITE_DACL**: Maps to `WriteDacl`.
4. **WRITE_OWNER**: Maps to `WriteOwner`.
5. **ADS_RIGHT_DS_CONTROL_ACCESS** (with object type GUID):
   - User-Force-Change-Password: `ForceChangePassword`
   - DS-Replication-Get-Changes + DS-Replication-Get-Changes-All: `DCSync` (both required)
   - Enroll: `Enroll`
   - AutoEnroll: `AutoEnroll`
6. **ADS_RIGHT_DS_WRITE_PROP** (with object type GUID):
   - Member: `AddMember` (on groups)
   - Service-Principal-Name: `WriteSPN` (on users)
   - ms-DS-Allowed-To-Delegate-To: `AddAllowedToAct`
   - msDS-KeyCredentialLink: `AddKeyCredentialLink`
   - GPC-File-Sys-Path: `WriteGPCFileSysPath` (on GPOs)
7. **ADS_RIGHT_DS_SELF** (with object type GUID):
   - Self-Membership: `AddSelf` (on groups)

### Well-known GUIDs

Extended rights, write properties, and self-writes each have a set of well-known GUIDs defined by Microsoft. Weaver maintains a registry of these GUIDs for resolution. Key GUIDs:

| GUID | Right |
|------|-------|
| `00299570-246d-11d0-a768-00aa006e0529` | User-Force-Change-Password |
| `1131f6aa-9c07-11d1-f79f-00c04fc2dcd2` | DS-Replication-Get-Changes |
| `1131f6ad-9c07-11d1-f79f-00c04fc2dcd2` | DS-Replication-Get-Changes-All |
| `bf9679c0-0de6-11d0-a285-00aa003049e2` | Member (write property) |
| `f3a64788-5306-11d1-a9c5-0000f80367c1` | Service-Principal-Name |
| `0e10c968-78fb-11d2-90d4-00c04f79dc55` | Enroll (certificate) |
| `a05b8cc2-17bc-4802-a710-e7c15ab866a2` | AutoEnroll (certificate) |
| `5b47d60f-6090-40b2-9f37-2a4de88f3063` | msDS-KeyCredentialLink |

---

## 5. Context-Aware Resolution

Some edges are only valid on specific target object types:

| Edge | Valid targets |
|------|-------------|
| `ForceChangePassword` | Users |
| `AddMember` | Groups |
| `AddSelf` | Groups |
| `WriteSPN` | Users, Computers |
| `Enroll` | Certificate Templates, Enterprise CAs |
| `AutoEnroll` | Certificate Templates |
| `WriteGPCFileSysPath` | GPOs |
| `DCSync` | Domains |

The resolver checks the target object type before emitting an edge. An ACE granting `User-Force-Change-Password` on a group object, for example, does not produce a `ForceChangePassword` edge.
