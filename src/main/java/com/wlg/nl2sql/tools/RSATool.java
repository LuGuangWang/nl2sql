package com.wlg.nl2sql.tools;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import javax.crypto.Cipher;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.HashMap;
import java.util.Map;

/**
 * RSA 公钥加密算法
 */
public class RSATool {
    private final String algorithm_name = "RSA";
    private final String privateKey_name = "privateKey";
    private final String publicKey_name = "publicKey";

    private volatile static RSATool ins = null;
    private volatile static BASE64Encoder encoder = null;
    private volatile static BASE64Decoder decoder = null;
    private volatile static Map<String, RSAKey> keyPairs = null;

    private RSATool() {
    }

    public static RSATool getIns() {
        if (ins == null) {
            synchronized (RSATool.class) {
                if (ins == null) {
                    ins = new RSATool();
                    encoder = new BASE64Encoder();
                    decoder = new BASE64Decoder();
                    keyPairs = ins.getKeyPair();
                }
            }
        }
        return ins;
    }

    /**
     * 加密
     */
    private byte[] encrypt(RSAPublicKey publicKey, byte[] srcBytes) throws Exception {
        if (publicKey != null) {
            //Cipher负责完成加密或解密工作，基于RSA
            Cipher cipher = Cipher.getInstance(algorithm_name);
            //根据公钥，对Cipher对象进行初始化
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] resultBytes = cipher.doFinal(srcBytes);
            return resultBytes;
        }
        return null;
    }

    public String encrypt(RSAPublicKey publicKey, String content) {
        String res = null;
        try {
            if (content != null) {
                byte[] resBytes = encrypt(publicKey, content.getBytes());
                res = encoder.encode(resBytes);
            }
        } catch (Exception e) {
            //
        }
        return res;
    }

    public String encrypt(String content) {
        String res = null;
        try {
            if (content != null) {
                byte[] resBytes = encrypt((RSAPublicKey) keyPairs.get(publicKey_name), content.getBytes());
                res = encoder.encode(resBytes);
            }
        } catch (Exception e) {
            //
        }
        return res;
    }

    /**
     * 解密
     */
    private byte[] decrypt(RSAPrivateKey privateKey, byte[] srcBytes) throws Exception {
        if (privateKey != null) {
            //Cipher负责完成加密或解密工作，基于RSA
            Cipher cipher = Cipher.getInstance(algorithm_name);
            //根据公钥，对Cipher对象进行初始化
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] resultBytes = cipher.doFinal(srcBytes);
            return resultBytes;

        }
        return null;
    }

    public String decrypt(RSAPrivateKey privateKey, String content) {
        String res = null;
        try {
            if (content != null) {
                byte[] bytes = decoder.decodeBuffer(content);
                byte[] resBytes = decrypt(privateKey, bytes);
                res = new String(resBytes);
            }
        } catch (Exception e) {
            //
        }
        return res;
    }

    public String decrypt(String content) {
        String res = null;
        try {
            if (content != null) {
                byte[] bytes = decoder.decodeBuffer(content);
                byte[] resBytes = decrypt((RSAPrivateKey) keyPairs.get(privateKey_name), bytes);
                res = new String(resBytes);
            }
        } catch (Exception e) {
            //
        }
        return res;
    }

    /**
     * 获取密钥对
     */
    public Map<String, RSAKey> getKeyPair() {
        Map<String, RSAKey> keyPairs = new HashMap<>();
        try {
            //KeyPairGenerator类用于生成公钥和私钥对，基于RSA算法生成对象
            KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance(algorithm_name);
            //初始化密钥对生成器，密钥大小为1024位
            keyPairGen.initialize(512);
            //生成一个密钥对，保存在keyPair中
            KeyPair keyPair = keyPairGen.generateKeyPair();
            //得到私钥
            RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
            //得到公钥
            RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();

            keyPairs.put(privateKey_name, privateKey);
            keyPairs.put(publicKey_name, publicKey);
        } catch (Exception e) {
            //
        }

        return keyPairs;
    }

    public static void main(String[] args) {
        String msg = " 1";

        String res = RSATool.getIns().encrypt(msg);
        System.out.println(res);

        String res1 = RSATool.getIns().decrypt(res);
        System.out.println("======pub:" + res1);

    }
}
