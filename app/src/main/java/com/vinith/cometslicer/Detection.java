package com.vinith.cometslicer;

public class Detection {
    public final float x;
    public final float y;
    public final float score;
    public final String label;

    public Detection(float x, float y, float score, String label) {
        this.x = x;
        this.y = y;
        this.score = score;
        this.label = label;
    }
}
