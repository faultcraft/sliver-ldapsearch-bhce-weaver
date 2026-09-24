package com.faultcraft.weaver.ad.model;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.faultcraft.weaver.util.Log;

final class CertificateParser {

    private CertificateParser() {}

    static void parseCaCertificate(Map<String, List<String>> entry, TypedProperties p) {
        String certB64 = BloodHoundObject.firstValue(entry, "cacertificate");
        if (certB64.isEmpty()) {
            return;
        }

        byte[] certBytes;
        try {
            certBytes = Base64.getDecoder().decode(certB64.strip());
        } catch (IllegalArgumentException e) {
            Log.warn("Skipping malformed cACertificate (truncated base64)");
            p.put("certthumbprint", "");
            p.put("certname", "");
            p.put("certchain", List.of());
            p.put("hasbasicconstraints", false);
            p.put("basicconstraintpathlength", 0);
            return;
        }

        String thumbprint = sha1Hex(certBytes).toUpperCase(Locale.ROOT);
        p.put("certthumbprint", thumbprint);
        p.put("certname", thumbprint);
        p.put("certchain", List.of());
        p.put("hasbasicconstraints", false);
        p.put("basicconstraintpathlength", 0);

        parseCertFields(certBytes, p, thumbprint);
    }

    private static void parseCertFields(byte[] certBytes, TypedProperties p,
            String thumbprint) {
        try {
            org.bouncycastle.asn1.ASN1Sequence seq =
                    org.bouncycastle.asn1.ASN1Sequence.getInstance(certBytes);
            org.bouncycastle.asn1.x509.Certificate cert =
                    org.bouncycastle.asn1.x509.Certificate.getInstance(seq);
            org.bouncycastle.asn1.x509.TBSCertificate tbs = cert.getTBSCertificate();

            org.bouncycastle.asn1.x500.X500Name subject = tbs.getSubject();
            org.bouncycastle.asn1.x500.RDN[] rdns = subject.getRDNs(
                    org.bouncycastle.asn1.x500.style.BCStyle.CN);
            if (rdns.length > 0) {
                String cn = org.bouncycastle.asn1.x500.style.IETFUtils
                        .valueToString(rdns[0].getFirst().getValue());
                p.put("certname", cn);
            }

            parseBasicConstraints(tbs, p);
        } catch (Exception e) {
            Log.warn("Failed to parse cacertificate, using thumbprint as certname");
        }
    }

    private static void parseBasicConstraints(
            org.bouncycastle.asn1.x509.TBSCertificate tbs, TypedProperties p) {
        org.bouncycastle.asn1.x509.Extensions exts = tbs.getExtensions();
        if (exts == null) {
            return;
        }
        org.bouncycastle.asn1.x509.Extension bcExt =
                exts.getExtension(org.bouncycastle.asn1.x509.Extension.basicConstraints);
        if (bcExt == null) {
            return;
        }
        org.bouncycastle.asn1.x509.BasicConstraints bc =
                org.bouncycastle.asn1.x509.BasicConstraints.getInstance(
                        bcExt.getParsedValue());
        if (bc.getPathLenConstraint() != null) {
            p.put("hasbasicconstraints", true);
            p.put("basicconstraintpathlength", bc.getPathLenConstraint().intValue());
        }
    }

    static String sha1Hex(byte[] data) {
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            byte[] digest = sha1.digest(data);
            var sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
    }
}
