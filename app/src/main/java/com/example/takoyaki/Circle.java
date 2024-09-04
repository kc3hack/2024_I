package com.example.takoyaki;

import android.view.View;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Canvas;
import android.content.Context;
import android.util.AttributeSet;

/**
 * 円を表示するViewクラス．
 */
public class Circle extends View {
    private final Paint paint;
    private int numOfRow;// 表示する円の行数．
    private int numOfColumn;// 表示する円の列数．
    private int marginX;// 画面左右のマージン．
    private int marginY;// 画面上下のマージン．
    private int cellSize;// 表示する円の直径（切り取る画像の横幅）．

    /**
     * XMLレイアウトファイルからこのカスタムビューをインスタンス化するときに使用されるコンストラクタ．
     *
     * @param context 現在の状態やリソースへのアクセスに必要な情報を保持するオブジェクト．
     * @param attrs   　XMLレイアウトファイルからビューに設定された属性（例えば、色やサイズなど）を含むオブジェクト．
     */
    public Circle(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint = createDefaultPaint();
    }

    /**
     * プログラム（コード）でこのカスタムビューを動的にインスタンス化する場合に使用されるコンストラクタ．
     *
     * @param context     現在の状態やリソースへのアクセスに必要な情報を保持するオブジェクト．
     * @param numOfRow    表示する円の行数．
     * @param numOfColumn 表示する円の列数．
     * @param marginX     画面左右のマージン．
     * @param marginY     画面上下のマージン．
     * @param cellSize    一つの円の横幅．
     */
    public Circle(Context context, int numOfRow, int numOfColumn, int marginX, int marginY, int cellSize) {
        super(context);
        this.numOfRow = numOfRow;
        this.numOfColumn = numOfColumn;
        this.marginX = marginX;
        this.marginY = marginY;
        this.cellSize = cellSize;
        paint = createDefaultPaint();
    }

    /**
     * Paintオブジェクトの設定を行うメソッド．
     *
     * @return デフォルトの値に設定したPaintオブジェクト．
     */
    private Paint createDefaultPaint() {
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5f);
        return paint;
    }

    /**
     * ユーザーが入力した数の円を描画するメソッド．
     *
     * @param canvas the canvas on which the background will be drawn
     */
    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(Color.TRANSPARENT);
        int r = cellSize / 2;// 円の半径
        for (int i = 0; i < numOfRow; i++) {
            for (int j = 0; j < numOfColumn; j++) {
                canvas.drawCircle(marginX + r + cellSize * j, marginY + r + cellSize * i, r, paint);
            }
        }
    }

}
