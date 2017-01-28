package com.bjrara.algorithms;

/**
 * Created by mengyizhou on 2017/1/28.
 */
public class ExtendedGcdSolution {
    public int extGcd(int a, int b) {
        Int x = new Int();
        Int y = new Int();
        int d = extGcd0(a, b, x, y);
        System.out.printf("Move %d by %d, Move %d by %d.\n", a, x.v, b, y.v);
        return d;
    }

    private int extGcd0(int a, int b, Int x, Int y) {
        int d = a;
        if (b != 0) {
            Int x0 = new Int();
            Int y0 = new Int();
            d = extGcd(b, a % b, x0, y0);
            x.v = y0.v;
            y.v = x0.v - (a / b) * y0.v;
        } else {
            x.v = 1;
            y.v = 1;
        }
        return d;
    }

    private int extGcd(int a, int b, Int x, Int y) {
        int d = a;
        if (b != 0) {
            d = extGcd(b, a % b, y, x);
            y.v -= (a / b) * x.v;
        } else {
            x.v = 1;
            y.v = 0;
        }
        return d;
    }

    class Int {
        private int v;
    }
}
