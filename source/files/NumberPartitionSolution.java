package com.bjrara.algorithms;

/**
 * Created by mengyizhou on 2017/1/8.
 */
public class NumberPartitionSolution {

    public int modedPartition(int num, int size, int mod) {
        int[][] dc = new int[num + 1][size + 1];
        for (int i = 0; i <= num; i++) {
            for (int j = 0; j <= size; j++) {
                dc[i][j] = 0;
            }
        }

        dc[0][0] = 1;
        for (int j = 1; j <= size; j++) {
            for (int i = 0; i <= num; i++) {
                if (i - j >= 0) {
                    dc[i][j] = (dc[i][j - 1] + dc[i - j][j]) % mod;
                } else {
                    dc[i][j] = dc[i][j - 1];
                }
            }
        }

        return dc[num][size];
    }

    public int partition(int num, int size) {
        int[][] dc = new int[num + 1][size + 1];
        for (int i = 0; i <= num; i++) {
            for (int j = 0; j <= size; j++) {
                dc[i][j] = 0;
            }
        }

        dc[0][0] = 1;
        for (int j = 1; j <= size; j++) {
            for (int i = 0; i <= num; i++) {
                if (i - j >= 0) {
                    dc[i][j] = dc[i][j - 1] + dc[i - j][j];
                } else {
                    dc[i][j] = dc[i][j - 1];
                }
            }
        }

        return dc[num][size];
    }
}