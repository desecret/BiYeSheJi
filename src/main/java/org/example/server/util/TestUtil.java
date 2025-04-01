package org.example.server.util;

import java.io.IOException;

public class TestUtil {
    public static void main(String[] args) {
        try {
            util.writeToYaml();
            System.out.println("YAML文件生成成功！");
        } catch (IOException e) {
            System.err.println("生成YAML文件时出错：" + e.getMessage());
            e.printStackTrace();
        }
    }
}