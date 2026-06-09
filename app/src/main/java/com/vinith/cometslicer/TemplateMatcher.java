package com.vinith.cometslicer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class TemplateMatcher {
    private final List<Template> cometTemplates = new ArrayList<>();
    private final List<Template> skullTemplates = new ArrayList<>();

    public TemplateMatcher(Context context) {
        load(context, "templates/comet_white_template.png", true);
        load(context, "templates/comet_pink_template.png", true);
        load(context, "templates/skull_template.png", false);
    }

    private void load(Context context, String asset, boolean comet) {
        try (InputStream is = context.getAssets().open(asset)) {
            Bitmap bitmap = BitmapFactory.decodeStream(is).copy(Bitmap.Config.ARGB_8888, false);
            Template t = new Template(asset, bitmap);
            if (comet) cometTemplates.add(t);
            else skullTemplates.add(t);
        } catch (Exception e) {
            throw new RuntimeException("Template load failed: " + asset, e);
        }
    }

    public Detection findComet(Bitmap screen) {
        Detection bestComet = null;
        for (Template t : cometTemplates) {
            Detection d = findBest(screen, t, 16, 0.74f);
            if (d != null && (bestComet == null || d.score > bestComet.score)) {
                bestComet = d;
            }
        }

        if (bestComet == null) return null;

        Detection skull = null;
        for (Template t : skullTemplates) {
            Detection d = findBest(screen, t, 14, 0.70f);
            if (d != null && (skull == null || d.score > skull.score)) {
                skull = d;
            }
        }

        if (skull != null && distance(bestComet.x, bestComet.y, skull.x, skull.y) < 160f * BotState.captureScale) {
            return null;
        }

        return bestComet;
    }

    private Detection findBest(Bitmap screen, Template template, int step, float minScore) {
        int sw = screen.getWidth();
        int sh = screen.getHeight();
        int tw = template.width;
        int th = template.height;
        if (tw >= sw || th >= sh) return null;

        int[] screenPixels = new int[sw * sh];
        screen.getPixels(screenPixels, 0, sw, 0, 0, sw, sh);

        float best = -1f;
        int bestX = 0;
        int bestY = 0;

        for (int y = 0; y <= sh - th; y += step) {
            for (int x = 0; x <= sw - tw; x += step) {
                float score = scoreAt(screenPixels, sw, x, y, template);
                if (score > best) {
                    best = score;
                    bestX = x;
                    bestY = y;
                }
            }
        }

        if (best < minScore) return null;
        return new Detection(bestX + tw / 2f, bestY + th / 2f, best, template.name);
    }

    private float scoreAt(int[] screen, int sw, int ox, int oy, Template t) {
        long totalDiff = 0;
        int count = 0;

        for (int y = 0; y < t.height; y += 4) {
            int si = (oy + y) * sw + ox;
            int ti = y * t.width;
            for (int x = 0; x < t.width; x += 4) {
                int sp = screen[si + x];
                int tp = t.pixels[ti + x];

                int sr = (sp >> 16) & 255;
                int sg = (sp >> 8) & 255;
                int sb = sp & 255;

                int tr = (tp >> 16) & 255;
                int tg = (tp >> 8) & 255;
                int tb = tp & 255;

                totalDiff += Math.abs(sr - tr) + Math.abs(sg - tg) + Math.abs(sb - tb);
                count++;
            }
        }

        float maxDiff = count * 765f;
        return 1f - (totalDiff / maxDiff);
    }

    private float distance(float ax, float ay, float bx, float by) {
        float dx = ax - bx;
        float dy = ay - by;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private static class Template {
        final String name;
        final int width;
        final int height;
        final int[] pixels;

        Template(String name, Bitmap bitmap) {
            this.name = name;
            this.width = bitmap.getWidth();
            this.height = bitmap.getHeight();
            this.pixels = new int[width * height];
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        }
    }
}
