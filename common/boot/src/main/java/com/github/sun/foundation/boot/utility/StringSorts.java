package com.github.sun.foundation.boot.utility;

import lombok.experimental.UtilityClass;

import java.util.*;

@UtilityClass
public class StringSorts {
    // 对字符串进行聚类和排序
    public static List<String> sort(List<String> arr, double threshold) {
        return sort(arr.toArray(new String[]{}), threshold);
    }

    private static List<String> sort(String[] arr, double threshold) {
        // 使用相似度阈值进行聚类
        List<List<String>> clusters = clusterSort(arr, threshold);
        List<String> ret = new ArrayList<>();
        for (List<String> cluster : clusters) {
            cluster.sort(Comparator.comparingInt(String::length));
            ret.addAll(cluster);
        }
        return ret;
    }

    public static List<List<String>> clusterSort(List<String> arr, double threshold) {
        return clusterSort(arr.toArray(new String[]{}), threshold);
    }

    // 使用凝聚层次聚类来聚类字符串
    public static List<List<String>> clusterSort(String[] arr, double threshold) {
        double[][] matrix = buildMatrix(arr);
        int n = arr.length;
        // 初始化聚类
        List<Set<Integer>> clusters = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            Set<Integer> cluster = new HashSet<>();
            cluster.add(i);
            clusters.add(cluster);
        }

        // 凝聚层次聚类算法
        while (clusters.size() > 1) {
            double maxSimilarity = Double.NEGATIVE_INFINITY;
            int[] bestPair = {-1, -1};

            // 找到最相似的两个聚类
            for (int i = 0; i < clusters.size(); i++) {
                for (int j = i + 1; j < clusters.size(); j++) {
                    double similarity = similarity(clusters.get(i), clusters.get(j), matrix);
                    if (similarity > maxSimilarity) {
                        maxSimilarity = similarity;
                        bestPair[0] = i;
                        bestPair[1] = j;
                    }
                }
            }

            // 如果最大相似度小于阈值，停止合并
            if (maxSimilarity < threshold) {
                break;
            }

            // 合并最相似的两个聚类
            Set<Integer> cluster1 = clusters.get(bestPair[0]);
            Set<Integer> cluster2 = clusters.get(bestPair[1]);
            cluster1.addAll(cluster2);
            clusters.remove(bestPair[1]);
        }

        // 将结果转换为字符串列表
        List<List<String>> clusteredStrings = new ArrayList<>();
        for (Set<Integer> cluster : clusters) {
            List<String> clusterList = new ArrayList<>();
            for (int index : cluster) {
                clusterList.add(arr[index]);
            }
            clusteredStrings.add(clusterList);
        }

        return clusteredStrings;
    }

    // 计算两个聚类之间的相似度
    private static double similarity(Set<Integer> cluster1, Set<Integer> cluster2, double[][] matrix) {
        double total = 0;
        int count = 0;
        for (int i : cluster1) {
            for (int j : cluster2) {
                total += matrix[i][j];
                count++;
            }
        }
        return total / count;
    }

    // 构建相似度矩阵
    private static double[][] buildMatrix(String[] arr) {
        int size = arr.length;
        double[][] matrix = new double[size][size];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (i != j) {
                    matrix[i][j] = dice(arr[i], arr[j]);
                }
            }
        }
        return matrix;
    }

    // 计算Dice相似度
    private static double dice(String s1, String s2) {
        Set<String> bigrams1 = bigrams(s1);
        Set<String> bigrams2 = bigrams(s2);

        Set<String> intersection = new HashSet<>(bigrams1);
        intersection.retainAll(bigrams2);

        int intersectionSize = intersection.size();
        int totalBigrams = bigrams1.size() + bigrams2.size();

        return (2.0 * intersectionSize) / totalBigrams;
    }

    // 将字符串分成bigrams
    private static Set<String> bigrams(String s) {
        Set<String> bigrams = new HashSet<>();
        for (int i = 0; i < s.length() - 1; i++) {
            bigrams.add(s.substring(i, i + 2));
        }
        return bigrams;
    }
}
