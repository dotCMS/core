/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.license;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dotcms.enterprise.license.bouncycastle.crypto.digests.SHA1Digest;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.RSAKeyParameters;
import com.dotcms.enterprise.license.bouncycastle.crypto.signers.RSADigestSigner;
import com.dotcms.enterprise.license.bouncycastle.util.encoders.Base64;
import org.apache.commons.io.IOUtils;
import com.dotmarketing.business.DotStateException;

public class LicenseTransformer{

    public final String license;
    public final DotLicense dotLicense;
    
    public LicenseTransformer(File f){
        try (InputStream is = Files.newInputStream(f.toPath())){
            this.license = IOUtils.toString(is);
            this.dotLicense=toLicense(this.license);
        } catch (Exception e) {
            throw new DotStateException(e);
        }
    }
    
    
    public LicenseTransformer(String license){
        this.license = license;
        try {
            this.dotLicense=toLicense(license);
        } catch (IOException e) {
            throw new DotStateException(e);
        }
        
    }
    public LicenseTransformer(byte[] licenseB){
        this(new String(licenseB));
    }
    
    /**
     * Initializes the License Manager information by reading the license data file.
     * 
     * @param raw - The license data file.
     * @throws IOException An error occurred when accessing or reading the file.
     */
    private DotLicense toLicense(final String raw) throws IOException  {
        final byte[] bytes = raw.getBytes();
        final Map<String, Object> data = getLicenseData(bytes);
        if (data != null) {
            
            return new DotLicense()
                   .level((Integer) data.get("level"))
                   .validUntil((Date) data.get("validUntil"))
                   .clientName((String) data.get("clientName"))
                   .licenseType((String) data.get("licenseType"))
                   .perpetual((Boolean) data.get("perpetual"))
                   .licenseVersion((Integer) data.get("version"))
                   .serial((String) data.get("serial"))
                   .raw(raw);

        } 
        return new DotLicense();
    }
    
    /**
     * Transforms the encrypted information contained in the license data file into human-readable
     * data and puts it inside a {@link Map}.
     * 
     * @param bytes - The byte array containing the license file's data.
     * @return A map with the license data.
     * @throws IOException An error occurred when verifying the license file.
     */
    protected Map<String, Object> getLicenseData(byte[] bytes) throws IOException {
        Map<String, Object> data = new HashMap<>();

        RSADigestSigner verifier = new RSADigestSigner(new SHA1Digest());
        verifier.init(false, loadPublicKey());

        byte[] unencodedData = Base64.decode(bytes);
        byte[] signature = split(unencodedData, 0, 128);
        byte[] body = split(unencodedData, 128, unencodedData.length - 128);
        verifier.update(body, 0, body.length);
        boolean ok = verifier.verifySignature(signature);
        if (ok) {
            List<byte[]> fields = splitBody(body);
            data.put("clientName", new String(fields.get(0)));
            data.put("issueDate", new Date(byteToLong(fields.get(1))));
            data.put("validUntil", new Date(byteToLong(fields.get(2))));
            data.put("level", byteToInt(fields.get(3)));

            data.put("licenseType", new String(fields.get(5)));
            data.put("perpetual", fields.get(6)[3] == 1);
            data.put("site", fields.get(7)[3] == 1);
            data.put("version", byteToInt(fields.get(8)));
            data.put("serial", new String(fields.get(9)));
            return data;
        }
        return null;
    }
    /**
     * Utility method that transforms a byte array into an integer.
     * 
     * @param b
     * @return
     */
    private int byteToInt(byte[] b) {
        return (b[0] << 24) + ((b[1] & 0xFF) << 16) + ((b[2] & 0xFF) << 8) + (b[3] & 0xFF);
    }

    /**
     * Utility method that transforms a byte array into a long.
     * 
     * @param b
     * @return
     */
    private long byteToLong(byte[] b) {

        return ((long) b[0] << 56) + ((long) (b[1] & 0xFF) << 48) + ((long) (b[2] & 0xFF) << 40)
                        + ((long) (b[3] & 0xFF) << 32) + ((long) (b[4] & 0xFF) << 24)
                        + ((long) (b[5] & 0xFF) << 16) + ((long) (b[6] & 0xFF) << 8)
                        + (long) (b[7] & 0xFF);
    }

    /**
     * 
     * @param array
     * @return
     */
    private List<byte[]> splitBody(byte[] array) {
        int pos = 0;
        List<byte[]> ret = new ArrayList<>();
        while (pos < array.length) {
            int size = byteToInt(split(array, pos, 4));
            pos += 4;
            byte[] data = split(array, pos, size);
            pos += size;
            ret.add(data);

        }
        return ret;
    }

    /**
     * Utility method that splits a byte array into a smaller section.
     * 
     * @param array - The array to split.
     * @param start - The start position that will be contained in the new array.
     * @param length - The offset of the new array.
     * @return The new sub-array.
     */
    private byte[] split(byte[] array, int start, int length) {
        byte[] ret = new byte[length];
        for (int i = 0; i < length; i++) {
            ret[i] = array[i + start];
        }
        return ret;
    }

    /**
     * 
     * @return
     * @throws IOException
     */
    private RSAKeyParameters loadPublicKey() throws IOException {
        String[] licData = publicDatFile;
        BigInteger mod = new BigInteger(licData[1]);
        BigInteger exponent = new BigInteger(licData[2]);
        RSAKeyParameters key = new RSAKeyParameters(false, mod, exponent);
        return key;
    }
    
    static final String[] publicDatFile = new String[]{"a9645f9fd30662311f40487dfa4806f7af367a8ad47618eb111031fa2bbfb06b",
                    "149401577611254638861748771664024149308035973319731601700679806189787935336357592020980482123451599299352822197941726907931740483371768833672606470799177999840802146912022025104358944304070698428736166001830605084809633902030517410807811572361092067370259932291339391222445907490650900460670674610409761321517",
                    "65537",
                    "0eab63d2fb7ae5572266879e9883437cc83a2f0a9723095306f9280d6c32710a"};
    
    


 
}
