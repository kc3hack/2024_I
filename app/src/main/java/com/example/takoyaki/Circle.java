package com.example.takoyaki;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

public class Circle extends View {
    private Paint paint;
    private float lineStrokeWidth = 5f;
    private int numOfRow, numOfClumn, marginX, marginY, cellSize;

    public Circle(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint();
    }

    public Circle(Context context, int numOfRow, int numOfColumn, int marginX, int marginY, int cellSize) {
        super(context);
        paint = new Paint();
        this.numOfRow = numOfRow;
        this.numOfClumn = numOfColumn;
        this.marginX = marginX;
        this.marginY = marginY;
        this.cellSize = cellSize;
    }

    public void setColor(int color) {
        paint.setColor(getResources().getColor(color));
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        paint.setAntiAlias(true);
        canvas.drawColor(Color.argb(0, 0, 0, 0));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(lineStrokeWidth);
        for (int i = 0; i < numOfClumn; i++) {
            for (int j = 0; j < numOfRow; j++) {
                canvas.drawCircle(marginX+cellSize/2+cellSize * j, marginY+cellSize/2 + cellSize * i, cellSize / 2, paint);
            }
        }

    }
}