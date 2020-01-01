package kr.ac.ajou.esd.ocr;

import android.content.Context;
import android.graphics.Bitmap;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
/**
 * Android Package 의 RenderScript를 이용하여 Gaussian Blur 구현
 */
public class GaussianBlur {

    private RenderScript renderScript;

    public GaussianBlur(Context context) {
        this.renderScript = RenderScript.create(context);
    }

    /**
     * 
     * @param radius Range(from = 1, to = 25) 
     * @param original GaussianBlur 처리할 이미지
     * @return
     */
    public Bitmap transform(int radius, Bitmap original) {
        Allocation input = Allocation.createFromBitmap(renderScript, original);
        Allocation output = Allocation.createTyped(renderScript, input.getType());
        ScriptIntrinsicBlur scriptIntrinsicBlur = ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript));
        scriptIntrinsicBlur.setRadius(radius);
        scriptIntrinsicBlur.setInput(input);
        scriptIntrinsicBlur.forEach(output);
        output.copyTo(original);
        return original;
    }

}