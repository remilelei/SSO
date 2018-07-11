package com.remile.util;

import sun.misc.BASE64Encoder;

import java.io.*;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.util.Date;
import java.util.Random;

public class KeyPairGenUtil {
    /** 指定加密算法为RSA */
    private static final String ALGORITHM = "RSA";
    /** 密钥长度，用来初始化 */
    private static final int KEYSIZE = 1024;
    /** 指定公钥存放文件 */
    private static String PUBLIC_KEY_FILE = "PublicKey.remile";
    /** 指定私钥存放文件 */
    private static String PRIVATE_KEY_FILE = "PrivateKey.remile";

    public static final String TAG = "KeyPairGenUtil";

    public static void main(String[] args) throws Exception {
        generateKeyPair();
//        genKeyPair();
        byte[] privateKey = readKey(PRIVATE_KEY_FILE);
        System.out.print("privateKey:");
        for(byte b : privateKey) {
            System.out.print(b + " ");
        }
        System.out.println();


        byte[] publicKey = readKey(PUBLIC_KEY_FILE);
        System.out.print("publicKey:");
        for(byte b : publicKey) {
            System.out.print(b + " ");
        }
        System.out.println();


        byte[] privateKeyParsed = readPrivateKeyFromPublicKey(publicKey);
        System.out.print("privateKeyParsed:");
        for(byte b : privateKeyParsed) {
            System.out.print(b + " ");
        }
        System.out.println();
    }

    /**
     * 生成密钥对
     * @throws Exception
     */
    public static void generateKeyPair() throws Exception {

        byte[] privateKeyBytes = genPrivateKey();
        System.out.print("privateKey(Gen):");
        for(byte b : privateKeyBytes) {
            System.out.print(b + " ");
        }
        System.out.println();

        byte[] publicKeyBytes = genPublicKey(privateKeyBytes);
        System.out.print("publicKey(Gen):");
        for(byte b : publicKeyBytes) {
            System.out.print(b + " ");
        }
        System.out.println();


        FileOutputStream oos1 = null;
        FileOutputStream oos2 = null;
        try {
            /** 用对象流将生成的密钥写入文件 */
            oos1 = new FileOutputStream(PUBLIC_KEY_FILE);
            oos2 = new FileOutputStream(PRIVATE_KEY_FILE);
            oos1.write(publicKeyBytes);
            oos2.write(privateKeyBytes);
        } catch (Exception e) {
            throw e;
        } finally {
            /** 清空缓存，关闭文件输出流 */
            oos1.close();
            oos2.close();
        }
    }

    // 时间做种生成私钥
    public static byte[] genPrivateKey() {
        Date date = new Date();
        String curDate = date.toString();
        byte[] privateKey = curDate.getBytes();
        return privateKey;
    }

    // 根据私钥生成公钥
    public static byte[] genPublicKey(byte[] privateKey) {
        int len = privateKey.length * 2;
        byte[] evilBytes = new byte[privateKey.length];
        byte[] publicKey = new byte[len];
        Random random = new Random();
        random.nextBytes(evilBytes);

        for(int i = 0; i < len; ++ i) {
            if(i % 2 == 0) {
                publicKey[i] = (byte)(privateKey[i / 2] ^ len);
            } else {
                publicKey[i] = evilBytes[i / 2];
            }
        }
        return publicKey;
    }

    public static byte[] readPrivateKeyFromPublicKey(byte[] publicKey) {
        byte[] bytes = new byte[publicKey.length / 2];
        for(int i = 0; i < publicKey.length; ++ i) {
            if(i % 2 == 0) {
                bytes[i / 2] = (byte)(publicKey[i] ^ publicKey.length);
            }
        }
        return bytes;
    }

    public static byte[] readKey(String pathStr) {
        Path path = Paths.get(pathStr);
        byte[] data = null;
        try {
            data = Files.readAllBytes(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return data;
    }

    public static void writeKey(String filePath, byte[] key) {
        Path path = Paths.get(filePath);
        try {
            Files.write(path, key);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String encrypt(String text, byte[] publicKey) {
        byte[] textBytes = text.getBytes();
        byte[] privateKey = readPrivateKeyFromPublicKey(publicKey);

        int lenContent = textBytes.length;
        int lenResult = lenContent  * 2;

        byte[] encryptedText = new byte[lenResult];
        System.out.println("content size=" + textBytes.length + "------");
//        for(int i = 0; i < textBytes.length; ++ i) {
//            encryptedText[textBytes.length - 1 - i] = (byte) (textBytes[i] ^ privateKey[i % privateKey.length]);
//            System.out.print(encryptedText[textBytes.length - 1 - i] + " ");
//        }
//        System.out.println("\n-----------");

        for(int i = 0; i < textBytes.length; ++ i) {
            encryptedText[lenResult - i * 2 - 1] = textBytes[i];
            if(lenResult - 2 - i * 2 >= 0) {
                encryptedText[lenResult - 2 - i * 2] = privateKey[i % privateKey.length];
            }
        }

        return new String(encryptedText);
    }

    public static String decrypt(String text, byte[] privateKey) {
        System.out.print("privateKey:");
        for(byte b : privateKey) {
            System.out.print(b + " ");
        }
        System.out.println();

        System.out.print("text:");
        for(byte b : text.getBytes()) {
            System.out.print(b + " ");
        }
        System.out.println();


        byte[] source = text.getBytes();
        int lenRet = source.length / 2;
        int lenArg = source.length;
        byte[] target = new byte[lenRet];

        for(int i = 0; i < lenRet; ++ i) {
            target[i] = source[lenArg - 1 - 2 * i];
        }

        return new String(target);
    }

//    private static void genKeyPair() throws NoSuchAlgorithmException {
//
//        /** RSA算法要求有一个可信任的随机数源 */
//        SecureRandom secureRandom = new SecureRandom();
//
//        /** 为RSA算法创建一个KeyPairGenerator对象 */
//        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(ALGORITHM);
//
//        /** 利用上面的随机数据源初始化这个KeyPairGenerator对象 */
//        keyPairGenerator.initialize(KEYSIZE, secureRandom);
//        //keyPairGenerator.initialize(KEYSIZE);
//
//        /** 生成密匙对 */
//        KeyPair keyPair = keyPairGenerator.generateKeyPair();
//
//        /** 得到公钥 */
//        Key publicKey = keyPair.getPublic();
//
//        /** 得到私钥 */
//        Key privateKey = keyPair.getPrivate();
//
//        byte[] publicKeyBytes = publicKey.getEncoded();
//        byte[] privateKeyBytes = privateKey.getEncoded();
//
//        String publicKeyBase64 = new BASE64Encoder().encode(publicKeyBytes);
//        String privateKeyBase64 = new BASE64Encoder().encode(privateKeyBytes);
//
//        System.out.println("publicKeyBase64.length():" + publicKeyBase64.length());
//        System.out.println("publicKeyBase64:" + publicKeyBase64);
//
//        System.out.println("privateKeyBase64.length():" + privateKeyBase64.length());
//        System.out.println("privateKeyBase64:" + privateKeyBase64);
//    }

}
