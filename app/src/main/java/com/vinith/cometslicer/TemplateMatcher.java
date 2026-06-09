package com.vinith.cometslicer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TemplateMatcher {
    private final List<Template> comets = new ArrayList<>();
    private final List<Template> skulls = new ArrayList<>();
    private long lastNoMatchMs = 0L;

    public TemplateMatcher(Context context) {
        load(context, "templates/comet_white_template.png", true);
        load(context, "templates/comet_pink_template.png", true);
        load(context, "templates/skull_template.png", false);
        AppLog.add("Templates loaded c=" + comets.size() + " s=" + skulls.size());
    }

    private void load(Context context, String asset, boolean isComet) {
        try (InputStream is = context.getAssets().open(asset)) {
            Bitmap original = BitmapFactory.decodeStream(is).copy(Bitmap.Config.ARGB_8888, false);
            int w = Math.max(10, Math.round(original.getWidth() * Bot.scale));
            int h = Math.max(10, Math.round(original.getHeight() * Bot.scale));
            Bitmap scaled = Bitmap.createScaledBitmap(original, w, h, true);
            original.recycle();

            Template t = new Template(asset, scaled);
            scaled.recycle();

            if (isComet) comets.add(t);
            else skulls.add(t);
        } catch (Throwable t) {
            AppLog.add("ERROR: template load " + asset + " " + t.getClass().getSimpleName());
            throw new RuntimeException(t);
        }
    }

    public Detection find(Bitmap frame) {
        Detection comet = bestOf(frame, comets, 14, 0.62f);
        if (comet == null) {
            long now = System.currentTimeMillis();
            if (now - lastNoMatchMs > 1600) {
                lastNoMatchMs = now;
                AppLog.add("No comet match");
            }
            return null;
        }

        Detection skull = bestOf(frame, skulls, 14, 0.62f);
        if (skull != null && dist(comet.x, comet.y, skull.x, skull.y) < 170f * Bot.scale) {
            AppLog.add("Skipped: skull near comet score=" + fmt(skull.score));
            return null;
        }

        AppLog.add("Comet " + shortName(comet.label) + " score=" + fmt(comet.score));
        return comet;
    }

    private Detection bestOf(Bitmap frame, List<Template> templates, int step, float threshold) {
        Detection best = null;
        for (Template t : templates) {
            Detection d = best(frame, t, step, threshold);
            if (d != null && (best == null || d.score > best.score)) best = d;
        }
        return best;
    }

    private Detection best(Bitmap frame, Template t, int step, float threshold) {
        int fw = frame.getWidth();
        int fh = frame.getHeight();
        if (t.w >= fw || t.h >= fh) return null;

        int[] fp = new int[fw * fh];
        frame.getPixels(fp, 0, fw, 0, 0, fw, fh);

        float bestScore = -1f;
        int bestX = 0;
        int bestY = 0;

        for (int y = 0; y <= fh - t.h; y += step) {
            for (int x = 0; x <= fw - t.w; x += step) {
                float s = score(fp, fw, x, y, t);
                if (s > bestScore) {
                    bestScore = s;
                    bestX = x;
                    bestY = y;
                }
            }
        }

        if (bestScore < threshold) return null;
        return new Detection(bestX + t.w / 2f, bestY + t.h / 2f, bestScore, t.name);
    }

    private float score(int[] frame, int frameW, int ox, int oy, Template t) {
        long diff = 0;
        int count = 0;

        for (int y = 0; y < t.h; y += 4) {
            int fi = (oy + y) * frameW + ox;
            int ti = y * t.w;
            for (int x = 0; x < t.w; x += 4) {
                int a = frame[fi + x];
                int b = t.pixels[ti + x];

                int ar = (a >> 16) & 255;
                int ag = (a >> 8) & 255;
                int ab = a & 255;

                int br = (b >> 16) & 255;
                int bg = (b >> 8) & 255;
                int bb = b & 255;

                diff += Math.abs(ar - br) + Math.abs(ag - bg) + Math.abs(ab - bb);
                count++;
            }
        }

        return 1f - (diff / (count * 765f));
    }

    private float dist(float ax, float ay, float bx, float by) {
        float dx = ax - bx;
        float dy = ay - by;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private String fmt(float v) {
        return String.format(Locale.US, "%.2f", v);
    }

    private String shortName(String s) {
        int i = s.lastIndexOf('/');
        return i >= 0 ? s.substring(i + 1) : s;
    }

    private static final class Template {
        final String name;
        final int w;
        final int h;
        final int[] pixels;

        Template(String name, Bitmap bitmap) {
            this.name = name;
            this.w = bitmap.getWidth();
            this.h = bitmap.getHeight();
            this.pixels = new int[w * h];
            bitmap.getPixels(pixels, 0, w, 0, 0, w, h);
        }
    }
}
