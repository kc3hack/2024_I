package com.example.takoyaki;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class Circle extends View {
    private Paint paint;
    private float lineStrokeWidth = 20f;

    public Circle(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint();
    }

    public Circle(Context context) {
        super(context);
        paint = new Paint();
        paint.setStrokeWidth(lineStrokeWidth);
    }

    public void setColor(int color) {
        paint.setColor(getResources().getColor(color));
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        paint.setAntiAlias(true);
        canvas.drawColor(Color.argb(127, 0, 127, 63));
        canvas.drawCircle(canvas.getHeight() / 2, canvas.getHeight() / 2, (canvas.getWidth() / 2) - 2, paint);
    }
}