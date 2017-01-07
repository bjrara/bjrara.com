package com.bjrara.algorithms;

import java.util.Arrays;

/**
 * Created by mengyizhou on 2017/1/1.
 */
public class LISSolution {

    // O(nlogn)
    public int maxLength(int[] nums) {
        int[] dp = new int[nums.length];
        for (int i = 0; i < nums.length; i++) {
            dp[i] = Integer.MAX_VALUE;
        }
        for (int j = 0; j < nums.length; j++) {
//            for (int i = j; i >= 0; i--) {
//                if (i == 0 || dp[i - 1] < nums[j]) {
//                    dp[i] = min(dp[i], nums[j]);
//                }
//            }
            // idx=(-(insertion point) - 1), the index of the first element in the range greater than the key
            int idx = Arrays.binarySearch(dp, 0, j, nums[j]);
            if (idx < 0) dp[-(idx + 1)] = nums[j];
        }

        for (int i = dp.length - 1; i >= 0; i--) {
            if (dp[i] != Integer.MAX_VALUE) {
                return i + 1;
            }
        }
        return 0;
    }

    // O(n^2)
    public int maxLength0(int[] nums) {
        int[] dp = new int[nums.length];
        for (int i = 0; i < nums.length; i++) {
            dp[i] = 1;
            for (int j = 0; j < i; j++) {
                if (nums[i] > nums[j]) {
                    dp[i] = max(dp[i], dp[j] + 1);
                } else {
                    dp[i] = max(dp[i], dp[j]);
                }
            }
        }
        return dp[nums.length - 1];
    }

    private int max(int i, int j) {
        return i < j ? j : i;
    }
}
