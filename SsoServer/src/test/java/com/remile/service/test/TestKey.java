package com.remile.service.test;

import com.remile.util.KeyPairGenUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;


public class TestKey {
    public static void main(String[] args) throws Exception {
//        KeyPairGenUtil.generateKeyPair();

//        byte[] publicKey = KeyPairGenUtil.readKey("PublicKey.remile");
//        byte[] privateKey = KeyPairGenUtil.genPrivateKey();
//        byte[] publicKey = KeyPairGenUtil.genPublicKeyicKey(privateKey);
//        byte[] privateKey2 = readPrivateKeyFromPublicKey(publicKey);
//
//        String input = "this is a plain text.";
//        String output = encrypt(input, publicKey);
//        System.out.println("decrypted input=" + output);
//        String plain = decrypt(output, privateKey);
//        System.out.println("plain input=" + plain);

        byte[] privateKey = KeyPairGenUtil.readKey("PrivateKey.remile");
        byte[] publicKey = KeyPairGenUtil.readKey("PublicKey.remile");

//        System.out.print("publicKey:");
//        for(byte b : publicKey) {
//            System.out.print(b + " ");
//        }
//        System.out.println();
//        System.out.print("privateKey(parsed):");
//        for(byte b : readPrivateKeyFromPublicKey(publicKey)) {
//            System.out.print(b + " ");
//        }
//        System.out.println();

//        String test = "{\"userId\":0,\"userName\":\"remile\",\"passWord\":\"123456\",\"loginMac\":\"mac0-13131313\"}";
//        String encryptContent = encrypt(test, publicKey);
//        KeyPairGenUtil.writeKey("encrypt_content.test", encryptContent.getBytes());
        byte[] encryptContent = KeyPairGenUtil.readKey("encrypt_content.test");
        String strEncryptContent = new String(encryptContent);
        String content = decrypt(strEncryptContent, privateKey);
        System.out.println(content);

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


        byte[] source = text.getBytes();
        int lenRet = source.length / 2;
        int lenArg = source.length;
        byte[] target = new byte[lenRet];

        for(int i = 0; i < lenRet; ++ i) {
            target[i] = source[lenArg - 1 - 2 * i];
        }

        return new String(target);
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

}
