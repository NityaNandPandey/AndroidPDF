//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.android.pdfnetsdksamples.samples;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
/*
import org.spongycastle.cert.jcajce.JcaCertStore;
import org.spongycastle.cms.CMSProcessableByteArray;
import org.spongycastle.cms.CMSSignedData;
import org.spongycastle.cms.CMSSignedDataGenerator;
import org.spongycastle.cms.CMSTypedData;
import org.spongycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.operator.ContentSigner;
import org.spongycastle.operator.jcajce.JcaContentSignerBuilder;
import org.spongycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.spongycastle.util.Store;
*/
import com.pdftron.sdf.SignatureHandler;

import com.pdftron.android.pdfnetsdksamples.OutputListener;

// This file shows a sample implementation of PDFNet's SignatureHandler class using
// the SpongyCastle library.

public class MySignatureHandler extends SignatureHandler {
    private ArrayList<Byte> m_data;
    private String m_pfx;
    private String m_password;
    private OutputListener m_outputListener = null;

    public MySignatureHandler(String pfx, String password) {
        this.m_pfx = pfx;
        this.m_password = password;
        m_data = new ArrayList<Byte>();
    }
    
    public void setOutputListener(OutputListener listener) {
        this.m_outputListener = listener;
    }

    @Override
    public String getName() {
        return ("Adobe.PPKLite");
    }

    @Override
    public void appendData(byte[] data) {
        for (int i = 0; i < data.length; i++)
            m_data.add(data[i]);
        return;
    }

    @Override
    public boolean reset() {
        m_data.clear();
        return (true);
    }

    @Override
    public byte[] createSignature() {
        /*
        try {
            java.security.Security.addProvider(new BouncyCastleProvider());
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            FileInputStream fis = new FileInputStream(m_pfx);
            keyStore.load(fis, m_password.toCharArray());
            String alias = keyStore.aliases().nextElement();
            PrivateKey privateKey = (PrivateKey) keyStore.getKey(alias, m_password.toCharArray());
            Certificate[] certChain = keyStore.getCertificateChain(alias);
            fis.close();

            Store certStore = new JcaCertStore(Arrays.asList(certChain));
            CMSSignedDataGenerator sigGen = new CMSSignedDataGenerator();
            ContentSigner signer = new JcaContentSignerBuilder("SHA1withRSA").setProvider("BC").build(privateKey);
            sigGen.addSignerInfoGenerator(new JcaSignerInfoGeneratorBuilder(
                    new JcaDigestCalculatorProviderBuilder().setProvider("BC").build()).build(signer, (X509Certificate) certChain[0]));
            sigGen.addCertificates(certStore);
            byte[] bdata = new byte[m_data.size()];
            for (int i = 0; i < m_data.size(); i++)
                bdata[i] = m_data.get(i).byteValue();
            CMSTypedData data = new CMSProcessableByteArray(bdata);
            CMSSignedData sigData = sigGen.generate(data, false);
            return (sigData.getEncoded());
        } catch (Exception ex) {
            if (this.m_outputListener != null) {
                this.m_outputListener.println(ex.getStackTrace());
            }
        }
        */
        return null;
    }
}
