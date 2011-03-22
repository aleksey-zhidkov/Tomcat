/*
 * Copyright (c) 2011 Alexey Zhidkov (Jdev). All Rights Reserved.
 */

package lxx.utils;

/**
 * User: jdev
 * Date: 07.03.2010
 */
public class Interval {

    public int a;
    public int b;

    public Interval(int a, int b) {
        this.a = a;
        this.b = b;
    }

    public Interval(Interval original) {
        this.a = original.a;
        this.b = original.b;
    }

    public int getLength() {
        return b - a + 1;
    }

    public boolean contains(int c) {
        return c >= a && c <= b;
    }

    public String toString() {
        return "[" + a + ", " + b + "]";
    }

    public void extend(int x) {
        if (a > x) {
            a = x;
        }
        if (b < x) {
            b = x;
        }
    }

    public boolean contains(Interval another) {
        return another.a >= a && another.b <= b;
    }

    public int center() {
        return (a + b) / 2;
    }
}