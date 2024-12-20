package com.example.antispamsystem.utils;

import java.util.Arrays;

public class CubicSpline {
    private final double[] a, b, c, d, h;
    private final int n;

    public CubicSpline(double[] x, double[] y) {
        n = x.length - 1;
        a = Arrays.copyOf(y, y.length);
        b = new double[n];
        c = new double[n + 1];
        d = new double[n];
        h = new double[n];

        for (int i = 0; i < n; i++) {
            h[i] = x[i + 1] - x[i]; // Разница между соседними параметрами t
        }

        double[] alpha = new double[n];
        for (int i = 1; i < n; i++) {
            alpha[i] = (3 * (a[i + 1] - a[i]) / h[i]) - (3 * (a[i] - a[i - 1]) / h[i - 1]);
        }

        double[] l = new double[n + 1];
        double[] mu = new double[n];
        double[] z = new double[n + 1];

        l[0] = 1;
        z[0] = 0;
        c[0] = 0;

        for (int i = 1; i < n; i++) {
            l[i] = 2 * (x[i + 1] - x[i - 1]) - h[i - 1] * mu[i - 1];
            mu[i] = h[i] / l[i];
            z[i] = (alpha[i] - h[i - 1] * z[i - 1]) / l[i];
        }

        l[n] = 1;
        z[n] = 0;
        c[n] = 0;

        for (int j = n - 1; j >= 0; j--) {
            c[j] = z[j] - mu[j] * c[j + 1];
            b[j] = (a[j + 1] - a[j]) / h[j] - h[j] * (c[j + 1] + 2 * c[j]) / 3;
            d[j] = (c[j + 1] - c[j]) / (3 * h[j]);
        }
    }

    public double interpolate(double targetT) {
        double accumulatedLength = 0.0; // Накопленная длина пути
        int i = 0;

        // Найти подходящий интервал
        while (i < n && targetT > accumulatedLength + h[i]) {
            accumulatedLength += h[i]; // Увеличиваем накопленную длину
            i++;
        }
        if (i >= n) i = n - 1; // Если вышли за пределы, берём последний интервал

        double deltaX = targetT - accumulatedLength; // Остаток внутри интервала
        return a[i] + b[i] * deltaX + c[i] * deltaX * deltaX + d[i] * deltaX * deltaX * deltaX;
    }
}
