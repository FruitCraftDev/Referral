package de.silke.referralpaper.utils;

import java.util.List;

public class Levenshtein {
    public static int calculateDistance(String s1, String s2) {
        int len1 = s1.length();
        int len2 = s2.length();

        int[][] dp = new int[len1 + 1][len2 + 1];

        for (int i = 0; i <= len1; i++) {
            dp[i][0] = i;
        }

        for (int j = 0; j <= len2; j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= len1; i++) {
            for (int j = 1; j <= len2; j++) {
                int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;

                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
            }
        }

        return dp[len1][len2];
    }

    public static String findClosestMatch(String target, List<String> strings) {
        int minDistance = Integer.MAX_VALUE;
        String closestMatch = "";

        for (String str : strings) {
            int distance = calculateDistance(target, str);
            if (distance < minDistance) {
                minDistance = distance;
                closestMatch = str;
            }
        }

        return closestMatch;
    }
}
