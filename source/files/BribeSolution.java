package com.ctrip.zeus;

import java.util.Arrays;

/**
 * Problem description:
 * https://code.google.com/codejam/contest/dashboard?c=189252#s=p2
 */

/**
 * Created by zhoumy on 2017/1/5.
 */
public class BribeSolution {
    public int bribe(int n, int c, int[] arr) {
        Arrays.sort(arr);

        int[] p = new int[c + 2];
        p[0] = 0;
        p[c + 1] = n;
        for (int i = 0; i < c; i++) {
            p[i + 1] = arr[i];
        }

        int[][] dp = new int[p.length][p.length];
        for (int i = 0; i < dp.length; i++) {
            for (int j = 0; j < dp.length; j++) {
                dp[i][j] = 0;
            }
        }

        for (int range_len = 1; range_len < p.length; range_len++) {
            for (int i = 0; i < p.length; i++) {
                int j = i + range_len;
                if (j >= p.length) continue;

                int tmp = Integer.MAX_VALUE;
                for (int k = i; k < j; k++) {
                    tmp = min(dp[i][k] + (p[j] - p[i]) + dp[k + 1][j], tmp);
                }
                dp[i][j] = tmp;
            }
        }
        return dp[0][c + 1];
    }

    private int min(int i, int i1) {
        return i < i1 ? i : i1;
    }
}
