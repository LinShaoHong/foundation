package com.github.sun.foundation.algorithm.array;

import java.util.Scanner;

/**
 * 八大排序算法
 */
public class Sort {
    public static void main(String[] args) {
        Scanner s = new Scanner(System.in);
        String input1 = s.nextLine();
        String input2 = s.nextLine();
        if ((input1 == null || input1.isEmpty()) || (input2 == null || input2.isEmpty())) {
            System.out.println(0);
            return;
        }
        input1 = input1.toUpperCase();
        input2 = input2.toUpperCase();
        int c = 0;
        for (int i = 0; i < input1.length(); i++) {
            if (input1.charAt(0) == input2.charAt(0)) {
                c++;
            }
        }
        System.out.println(c);
    }
}
